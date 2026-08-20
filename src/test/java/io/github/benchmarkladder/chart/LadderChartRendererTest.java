package io.github.benchmarkladder.chart;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardEntry;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class LadderChartRendererTest {
  @Test
  void rendersPngAtRequestedWidth() throws Exception {
    var snapshot = new LeaderboardSnapshot(
        BenchmarkSite.FRONTIERBENCH,
        "Test Leaderboard",
        "https://example.com",
        Instant.EPOCH,
        List.of(new LeaderboardEntry(
            1, "Model A", "Acme", "Agent", 42.7, null, 1.5,
            null, "1B", "$10", "https://example.com/model", Map.of())));

    byte[] png = new LadderChartRenderer().render(snapshot, new ChartOptions(5, 1200));
    var image = ImageIO.read(new ByteArrayInputStream(png));

    assertThat(png).startsWith(0x89, 0x50, 0x4E, 0x47);
    assertThat(image.getWidth()).isEqualTo(1200);
    assertThat(image.getHeight()).isGreaterThanOrEqualTo(720);
  }

  @Test
  void ordersHighestScoreAtTheTopOfTheDataset() {
    var snapshot = new LeaderboardSnapshot(
        BenchmarkSite.PROGRAMBENCH,
        "Test Leaderboard",
        "https://example.com",
        Instant.EPOCH,
        List.of(
            entry(3, "Low", 10.0, 20.0),
            entry(1, "High", 90.0, 95.0),
            entry(2, "Middle", 50.0, 70.0)));

    assertThat(LadderChartRenderer.selectEntries(snapshot, 3))
        .extracting(LeaderboardEntry::model)
        .containsExactly("High", "Middle", "Low");
  }

  @Test
  void rendersAllThemesWithDistinctBackgrounds() throws Exception {
    var snapshot = new LeaderboardSnapshot(
        BenchmarkSite.PROGRAMBENCH,
        "Theme Test",
        "https://example.com",
        Instant.EPOCH,
        List.of(entry(1, "Model A", 42.0, 70.0)));

    var renderer = new LadderChartRenderer();
    List<Integer> backgrounds = new java.util.ArrayList<>();
    for (ChartTheme theme : ChartTheme.values()) {
      byte[] png = renderer.render(snapshot, new ChartOptions(1, 900, theme));
      var image = ImageIO.read(new ByteArrayInputStream(png));
      backgrounds.add(image.getRGB(0, 0));
    }

    assertThat(backgrounds).doesNotHaveDuplicates();
  }

  private static LeaderboardEntry entry(
      int rank, String model, double score, Double secondaryScore) {
    return new LeaderboardEntry(
        rank, model, "Acme", "Agent", score, secondaryScore, null,
        null, null, null, null, Map.of());
  }
}
