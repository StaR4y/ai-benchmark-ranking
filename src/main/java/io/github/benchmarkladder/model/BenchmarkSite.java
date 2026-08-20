package io.github.benchmarkladder.model;

import java.util.Arrays;

public enum BenchmarkSite {
  PROGRAMBENCH("programbench", "ProgramBench", "https://programbench.com/"),
  FRONTIERBENCH("frontierbench", "Terminal-Bench 3.0", "https://www.frontierbench.ai/");

  private final String id;
  private final String displayName;
  private final String sourceUrl;

  BenchmarkSite(String id, String displayName, String sourceUrl) {
    this.id = id;
    this.displayName = displayName;
    this.sourceUrl = sourceUrl;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  public String sourceUrl() {
    return sourceUrl;
  }

  public static BenchmarkSite from(String value) {
    return Arrays.stream(values())
        .filter(site -> site.id.equalsIgnoreCase(value) || site.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Unknown site '" + value + "'. Expected programbench or frontierbench."));
  }
}
