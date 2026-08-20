package io.github.benchmarkladder.chart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChartThemeTest {
  @Test
  void parsesThemeCaseInsensitivelyAndDefaultsToDark() {
    assertThat(ChartTheme.from(null)).isEqualTo(ChartTheme.DARK);
    assertThat(ChartTheme.from("NEON")).isEqualTo(ChartTheme.NEON);
  }

  @Test
  void rejectsUnknownTheme() {
    assertThatThrownBy(() -> ChartTheme.from("purple"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dark, light, neon or mono");
  }
}
