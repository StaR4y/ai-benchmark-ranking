package io.github.benchmarkladder.crawler;

import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardSnapshot;

public interface LeaderboardCrawler {
  BenchmarkSite site();

  LeaderboardSnapshot crawl();
}
