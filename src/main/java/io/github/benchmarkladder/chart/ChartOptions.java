package io.github.benchmarkladder.chart;

public record ChartOptions(int top, int width, ChartTheme theme) {
  public ChartOptions {
    if (top < 1 || top > 50) {
      throw new IllegalArgumentException("top must be between 1 and 50");
    }
    if (width < 900 || width > 3200) {
      throw new IllegalArgumentException("width must be between 900 and 3200");
    }
    if (theme == null) {
      throw new IllegalArgumentException("theme is required");
    }
  }

  public ChartOptions(int top, int width) {
    this(top, width, ChartTheme.DARK);
  }

  public static ChartOptions defaults() {
    return new ChartOptions(12, 1600, ChartTheme.DARK);
  }
}
