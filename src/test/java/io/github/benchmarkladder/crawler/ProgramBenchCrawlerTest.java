package io.github.benchmarkladder.crawler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProgramBenchCrawlerTest {
  @Test
  void parsesLeaderboardRows() {
    String html = """
        <html><body><table class="lb-table"><tbody>
          <tr class="clickable-row" data-href="/run/model-a/" data-provider="Acme AI">
            <td class="col-rank">1</td>
            <td class="col-logo"><img alt="Acme AI"></td>
            <td class="col-model"><span class="model-name">Model A (high)</span></td>
            <td class="col-agent">Agent CLI</td>
            <td class="col-num resolved-highlight">12.5<span>%</span></td>
            <td class="col-num col-almost">44.0<span>%</span></td>
          </tr>
        </tbody></table></body></html>
        """;

    var snapshot = new ProgramBenchCrawler(null).parse(html, Instant.EPOCH);

    assertThat(snapshot.entries()).hasSize(1);
    var entry = snapshot.entries().getFirst();
    assertThat(entry.rank()).isEqualTo(1);
    assertThat(entry.model()).isEqualTo("Model A (high)");
    assertThat(entry.organization()).isEqualTo("Acme AI");
    assertThat(entry.agent()).isEqualTo("Agent CLI");
    assertThat(entry.score()).isEqualTo(12.5);
    assertThat(entry.secondaryScore()).isEqualTo(44.0);
    assertThat(entry.detailsUrl()).isEqualTo("https://programbench.com/run/model-a/");
  }
}
