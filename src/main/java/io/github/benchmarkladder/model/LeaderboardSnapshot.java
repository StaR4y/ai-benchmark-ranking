package io.github.benchmarkladder.model;

import java.time.Instant;
import java.util.List;

public record LeaderboardSnapshot(
    BenchmarkSite site,
    String title,
    String sourceUrl,
    Instant fetchedAt,
    List<LeaderboardEntry> entries) {

  public LeaderboardSnapshot {
    entries = List.copyOf(entries);
  }
}
