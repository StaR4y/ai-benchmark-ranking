package io.github.benchmarkladder.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.benchmarkladder.crawler.LeaderboardCrawler;
import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LeaderboardService {
  private final Map<BenchmarkSite, LeaderboardCrawler> crawlers;
  private final Cache<BenchmarkSite, LeaderboardSnapshot> cache;

  public LeaderboardService(List<LeaderboardCrawler> crawlers, Duration cacheTtl) {
    EnumMap<BenchmarkSite, LeaderboardCrawler> bySite = new EnumMap<>(BenchmarkSite.class);
    for (LeaderboardCrawler crawler : crawlers) {
      bySite.put(crawler.site(), crawler);
    }
    this.crawlers = Map.copyOf(bySite);
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(cacheTtl)
        .maximumSize(BenchmarkSite.values().length)
        .build();
  }

  public LeaderboardSnapshot get(BenchmarkSite site) {
    return get(site, false);
  }

  public LeaderboardSnapshot get(BenchmarkSite site, boolean refresh) {
    LeaderboardCrawler crawler = crawlers.get(site);
    if (crawler == null) {
      throw new IllegalArgumentException("No crawler configured for " + site.id());
    }
    if (refresh) {
      cache.invalidate(site);
    }
    return cache.get(site, ignored -> crawler.crawl());
  }

  public void invalidate(BenchmarkSite site) {
    cache.invalidate(site);
  }
}
