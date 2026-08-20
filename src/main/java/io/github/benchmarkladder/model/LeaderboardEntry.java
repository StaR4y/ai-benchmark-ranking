package io.github.benchmarkladder.model;

import java.time.LocalDate;
import java.util.Map;

public record LeaderboardEntry(
    int rank,
    String model,
    String organization,
    String agent,
    double score,
    Double secondaryScore,
    Double uncertainty,
    LocalDate releaseDate,
    String tokens,
    String cost,
    String detailsUrl,
    Map<String, String> extras) {

  public LeaderboardEntry {
    extras = extras == null ? Map.of() : Map.copyOf(extras);
  }
}
