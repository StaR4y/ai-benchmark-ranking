package io.github.benchmarkladder.crawler;

import io.github.benchmarkladder.config.AppConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public final class HttpPageClient {
  private static final int MAX_ATTEMPTS = 3;

  private final HttpClient client;
  private final Duration timeout;
  private final Duration requestDelay;
  private final String userAgent;
  private final AtomicLong nextRequestAt = new AtomicLong();

  public HttpPageClient(AppConfig config) {
    this.timeout = config.requestTimeout();
    this.requestDelay = config.requestDelay();
    this.userAgent = config.userAgent();
    this.client = HttpClient.newBuilder()
        .connectTimeout(config.requestTimeout())
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  public String get(String url) {
    return request(url, "GET", null, "text/html,application/xhtml+xml");
  }

  public String postJson(String url, String body) {
    return request(url, "POST", body, "application/json");
  }

  private String request(String url, String method, String body, String accept) {
    CrawlException lastFailure = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      awaitRequestSlot();
      try {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Accept", accept)
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("User-Agent", userAgent);
        if ("POST".equals(method)) {
          builder.header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
          builder.GET();
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(
            request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          return response.body();
        }
        if (response.statusCode() != 429 && response.statusCode() < 500) {
          throw new CrawlException(method + " " + url + " returned HTTP " + response.statusCode());
        }
        lastFailure = new CrawlException(
            method + " " + url + " returned HTTP " + response.statusCode());
      } catch (IOException exception) {
        lastFailure = new CrawlException("Unable to fetch " + url, exception);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new CrawlException("Interrupted while fetching " + url, exception);
      }
      if (attempt < MAX_ATTEMPTS) {
        sleep(Duration.ofMillis(300L * (1L << (attempt - 1))));
      }
    }
    throw lastFailure == null ? new CrawlException("Unable to fetch " + url) : lastFailure;
  }

  private void awaitRequestSlot() {
    while (true) {
      long now = System.currentTimeMillis();
      long current = nextRequestAt.get();
      long slot = Math.max(now, current);
      if (nextRequestAt.compareAndSet(current, slot + requestDelay.toMillis())) {
        sleep(Duration.ofMillis(Math.max(0, slot - now)));
        return;
      }
    }
  }

  private static void sleep(Duration duration) {
    if (duration.isZero() || duration.isNegative()) {
      return;
    }
    try {
      Thread.sleep(duration);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new CrawlException("Interrupted while rate limiting requests", exception);
    }
  }
}
