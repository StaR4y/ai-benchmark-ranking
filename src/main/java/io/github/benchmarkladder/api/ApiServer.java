package io.github.benchmarkladder.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.benchmarkladder.BenchmarkLadder;
import io.github.benchmarkladder.chart.ChartOptions;
import io.github.benchmarkladder.chart.ChartTheme;
import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

public final class ApiServer {
  private final BenchmarkLadder ladder;
  private final ObjectMapper mapper;

  public ApiServer(BenchmarkLadder ladder) {
    this.ladder = ladder;
    this.mapper = objectMapper();
  }

  public Javalin start(int port) {
    Javalin app = Javalin.create(config -> {
      config.jsonMapper(new JavalinJackson(mapper, false));
      config.http.defaultContentType = "application/json";
      config.showJavalinBanner = false;
    });

    app.get("/health", context -> context.json(Map.of(
        "status", "ok",
        "time", Instant.now().toString())));

    app.get("/api/v1/sites", context -> context.json(Arrays.stream(BenchmarkSite.values())
        .map(site -> Map.of(
            "id", site.id(),
            "name", site.displayName(),
            "sourceUrl", site.sourceUrl()))
        .toList()));

    app.get("/api/v1/chart-themes", context -> context.json(Arrays.stream(ChartTheme.values())
        .map(theme -> theme.id())
        .toList()));

    app.get("/api/v1/leaderboards/{site}", context -> {
      BenchmarkSite site = BenchmarkSite.from(context.pathParam("site"));
      boolean refresh = Boolean.parseBoolean(context.queryParam("refresh"));
      context.header("Cache-Control", "public, max-age=300");
      context.json(ladder.fetch(site, refresh));
    });

    app.get("/api/v1/charts/{site}.png", context -> {
      BenchmarkSite site = BenchmarkSite.from(context.pathParam("site"));
      int top = integerQuery(context.queryParam("top"), 12, "top");
      int width = integerQuery(context.queryParam("width"), 1600, "width");
      ChartTheme theme = ChartTheme.from(context.queryParam("theme"));
      boolean refresh = Boolean.parseBoolean(context.queryParam("refresh"));
      byte[] png = ladder.renderPng(site, new ChartOptions(top, width, theme), refresh);
      context.contentType("image/png");
      context.header("Cache-Control", "public, max-age=300");
      context.result(png);
    });

    app.post("/api/v1/refresh/{site}", context -> {
      BenchmarkSite site = BenchmarkSite.from(context.pathParam("site"));
      LeaderboardSnapshot snapshot = ladder.fetch(site, true);
      context.json(snapshot);
    });

    app.exception(IllegalArgumentException.class, (exception, context) -> {
      context.status(HttpStatus.BAD_REQUEST);
      context.json(new ApiError("bad_request", exception.getMessage()));
    });
    app.exception(Exception.class, (exception, context) -> {
      context.status(HttpStatus.BAD_GATEWAY);
      context.json(new ApiError("upstream_error", exception.getMessage()));
    });
    return app.start(port);
  }

  private static int integerQuery(String value, int fallback, String name) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(name + " must be an integer", exception);
    }
  }

  public static ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  private record ApiError(String code, String message) {
  }
}
