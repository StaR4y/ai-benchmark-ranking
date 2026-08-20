package io.github.benchmarkladder.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardEntry;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FrontierBenchCrawler implements LeaderboardCrawler {
  static final String DATA_URL =
      "https://ofhuhcpkvzjlejydnvyd.supabase.co/functions/v1/leaderboard-read";
  static final String REQUEST_BODY =
      "{\"package\":\"terminal-bench/terminal-bench\",\"name\":\"3-0-0\"}";

  private static final BenchmarkSite SITE = BenchmarkSite.FRONTIERBENCH;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpPageClient pageClient;

  public FrontierBenchCrawler(HttpPageClient pageClient) {
    this.pageClient = pageClient;
  }

  @Override
  public BenchmarkSite site() {
    return SITE;
  }

  @Override
  public LeaderboardSnapshot crawl() {
    return parse(pageClient.postJson(DATA_URL, REQUEST_BODY), Instant.now());
  }

  LeaderboardSnapshot parse(String json, Instant fetchedAt) {
    try {
      JsonNode root = MAPPER.readTree(json);
      JsonNode rows = root.path("rows");
      List<LeaderboardEntry> entries = new ArrayList<>();
      for (JsonNode row : rows) {
        if (!"display".equals(row.path("status").asText("display"))) {
          continue;
        }
        JsonNode metadata = row.path("metadata");
        JsonNode metrics = row.path("metrics");
        String model = metadata.path("model_display").path("label").asText();
        String effort = metadata.path("reasoning_effort").asText();
        if (!effort.isBlank()) {
          model += " (" + effort + ")";
        }
        Map<String, String> extras = new LinkedHashMap<>();
        extras.put("primaryMetric", "resolutionRate");
        putIfPresent(extras, "agentOrganization", metadata.path("agent_org").path("label"));
        if (row.hasNonNull("n_trials")) {
          extras.put("trials", row.path("n_trials").asText());
        }
        entries.add(new LeaderboardEntry(
            row.path("rank").asInt(),
            model,
            textOrNull(metadata.path("model_org").path("label")),
            metadata.path("agent_display").path("label").asText(),
            metrics.path("accuracy").asDouble(),
            null,
            metrics.path("accuracy_stderr").isNumber()
                ? metrics.path("accuracy_stderr").asDouble()
                : null,
            localDate(metadata.path("release_date").asText()),
            textOrNull(metrics.path("display_total_tokens")),
            textOrNull(metrics.path("display_cost")),
            textOrNull(metadata.path("model_display").path("url")),
            extras));
      }
      if (entries.isEmpty()) {
        throw new CrawlException(
            "FrontierBench API returned no display rows; its response schema may have changed");
      }
      String title = root.path("leaderboard").path("title").asText("Terminal-Bench 3.0");
      return new LeaderboardSnapshot(
          SITE, title + " Leaderboard", SITE.sourceUrl(), fetchedAt, entries);
    } catch (IOException exception) {
      throw new CrawlException("Unable to parse FrontierBench leaderboard JSON", exception);
    }
  }

  private static LocalDate localDate(String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }

  private static String textOrNull(JsonNode node) {
    return node.isMissingNode() || node.isNull() || node.asText().isBlank()
        ? null
        : node.asText();
  }

  private static void putIfPresent(Map<String, String> target, String key, JsonNode value) {
    String text = textOrNull(value);
    if (text != null) {
      target.put(key, text);
    }
  }
}
