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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FrontierBenchCrawler implements LeaderboardCrawler {
  static final String DATA_URL =
      "https://ofhuhcpkvzjlejydnvyd.supabase.co/functions/v1/leaderboard-read";
  static final String PACKAGE_NAME = "terminal-bench/terminal-bench";
  private static final Pattern CURRENT_LEADERBOARD_KEY = Pattern.compile(
      "\\\\\"queryKey\\\\\":\\[\\\\\"leaderboard\\\\\","
          + "\\\\\"terminal-bench/terminal-bench\\\\\","
          + "\\\\\"([0-9]+-[0-9]+-[0-9]+)\\\\\"\\]");

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
    String leaderboardName = discoverLeaderboardName(pageClient.get(SITE.sourceUrl()));
    return parse(pageClient.postJson(DATA_URL, requestBody(leaderboardName)), Instant.now());
  }

  static String discoverLeaderboardName(String html) {
    Matcher matcher = CURRENT_LEADERBOARD_KEY.matcher(html);
    if (!matcher.find()) {
      throw new CrawlException(
          "Terminal-Bench current leaderboard key was not found; the page layout may have changed");
    }
    return matcher.group(1);
  }

  static String requestBody(String leaderboardName) {
    return "{\"package\":\"" + PACKAGE_NAME + "\",\"name\":\""
        + leaderboardName + "\"}";
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
            uncertainty(metrics),
            localDate(firstText(metadata, "release_date", "date")),
            textOrNull(metrics.path("display_total_tokens")),
            normalizeCost(textOrNull(metrics.path("display_cost"))),
            textOrNull(metadata.path("model_display").path("url")),
            extras));
      }
      if (entries.isEmpty()) {
        throw new CrawlException(
            "Terminal-Bench API returned no display rows; its response schema may have changed");
      }
      String title = root.path("leaderboard").path("title").asText("Terminal-Bench");
      return new LeaderboardSnapshot(
          SITE, title + " Leaderboard", SITE.sourceUrl(), fetchedAt, entries);
    } catch (IOException exception) {
      throw new CrawlException("Unable to parse Terminal-Bench leaderboard JSON", exception);
    }
  }

  private static Double uncertainty(JsonNode metrics) {
    JsonNode confidenceInterval = metrics.path("accuracy_ci95_half_width");
    if (confidenceInterval.isNumber()) {
      return confidenceInterval.asDouble();
    }
    JsonNode standardError = metrics.path("accuracy_stderr");
    return standardError.isNumber() ? standardError.asDouble() : null;
  }

  private static String firstText(JsonNode parent, String... fields) {
    for (String field : fields) {
      String value = parent.path(field).asText();
      if (!value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private static String normalizeCost(String value) {
    return value != null && value.startsWith("$$") ? value.substring(1) : value;
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
