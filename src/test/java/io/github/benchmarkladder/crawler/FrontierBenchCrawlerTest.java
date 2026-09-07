package io.github.benchmarkladder.crawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FrontierBenchCrawlerTest {
  @Test
  void parsesPublicLeaderboardApi() {
    String json = """
        {
          "leaderboard": {"title": "Terminal-Bench 4.0"},
          "rows": [{
            "rank": 2,
            "status": "display",
            "n_trials": 330,
            "metadata": {
              "model_display": {"label": "Model B", "url": "https://example.com/model-b"},
              "agent_display": {"label": "Code Agent", "url": "https://example.com/agent"},
              "model_org": {"label": "Model Org", "url": "https://example.com"},
              "agent_org": {"label": "Agent Org", "url": "https://example.com"},
              "reasoning_effort": "max",
              "date": "2026-09-01"
            },
            "metrics": {
              "accuracy": 34.59,
              "accuracy_ci95_half_width": 2.72,
              "display_total_tokens": "5.8B",
              "display_cost": "$$4.0k"
            }
          }]
        }
        """;

    var snapshot = new FrontierBenchCrawler(null).parse(json, Instant.EPOCH);

    assertThat(snapshot.entries()).hasSize(1);
    var entry = snapshot.entries().getFirst();
    assertThat(entry.rank()).isEqualTo(2);
    assertThat(entry.model()).isEqualTo("Model B (max)");
    assertThat(entry.organization()).isEqualTo("Model Org");
    assertThat(entry.agent()).isEqualTo("Code Agent");
    assertThat(entry.score()).isEqualTo(34.59);
    assertThat(entry.uncertainty()).isEqualTo(2.72);
    assertThat(entry.releaseDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(entry.tokens()).isEqualTo("5.8B");
    assertThat(entry.cost()).isEqualTo("$4.0k");
    assertThat(entry.extras()).containsEntry("trials", "330");
  }

  @Test
  void discoversCurrentLeaderboardVersionFromTbenchPage() {
    String html = "prefix \\\"queryKey\\\":[\\\"leaderboard\\\","
        + "\\\"terminal-bench/terminal-bench\\\",\\\"4-0-0\\\"] suffix";

    assertThat(FrontierBenchCrawler.discoverLeaderboardName(html)).isEqualTo("4-0-0");
    assertThat(FrontierBenchCrawler.requestBody("4-0-0"))
        .isEqualTo("{\"package\":\"terminal-bench/terminal-bench\",\"name\":\"4-0-0\"}");
  }
}
