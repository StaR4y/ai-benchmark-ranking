package io.github.benchmarkladder.chart;

import java.util.Arrays;

public enum ChartTheme {
  DARK("dark"),
  LIGHT("light"),
  NEON("neon"),
  MONO("mono");

  private final String id;

  ChartTheme(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static ChartTheme from(String value) {
    if (value == null || value.isBlank()) {
      return DARK;
    }
    return Arrays.stream(values())
        .filter(theme -> theme.id.equalsIgnoreCase(value) || theme.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown theme '" + value + "'. Expected dark, light, neon or mono."));
  }
}
