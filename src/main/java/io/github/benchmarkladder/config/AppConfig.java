package io.github.benchmarkladder.config;

import java.time.Duration;

public record AppConfig(
    int port,
    Duration cacheTtl,
    Duration requestTimeout,
    Duration requestDelay,
    String userAgent) {

  public static AppConfig fromEnvironment() {
    return new AppConfig(
        envInt("PORT", 7070),
        Duration.ofMinutes(envInt("CACHE_TTL_MINUTES", 15)),
        Duration.ofSeconds(envInt("REQUEST_TIMEOUT_SECONDS", 20)),
        Duration.ofMillis(envInt("REQUEST_DELAY_MS", 1_000)),
        System.getenv().getOrDefault(
            "CRAWLER_USER_AGENT",
            "BenchmarkLadder/1.0 (+https://github.com/benchmark-ladder/benchmark-ladder)"));
  }

  private static int envInt(String name, int fallback) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(name + " must be an integer", exception);
    }
  }
}
