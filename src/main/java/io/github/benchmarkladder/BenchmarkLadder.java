package io.github.benchmarkladder;

import io.github.benchmarkladder.chart.ChartOptions;
import io.github.benchmarkladder.chart.LadderChartRenderer;
import io.github.benchmarkladder.config.AppConfig;
import io.github.benchmarkladder.crawler.FrontierBenchCrawler;
import io.github.benchmarkladder.crawler.HttpPageClient;
import io.github.benchmarkladder.crawler.ProgramBenchCrawler;
import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import io.github.benchmarkladder.service.LeaderboardService;
import java.util.List;

/** Public Java API for embedding the crawler and renderer. */
public final class BenchmarkLadder {
  private final LeaderboardService service;
  private final LadderChartRenderer renderer;

  public BenchmarkLadder() {
    this(AppConfig.fromEnvironment());
  }

  public BenchmarkLadder(AppConfig config) {
    HttpPageClient pageClient = new HttpPageClient(config);
    this.service = new LeaderboardService(
        List.of(new ProgramBenchCrawler(pageClient), new FrontierBenchCrawler(pageClient)),
        config.cacheTtl());
    this.renderer = new LadderChartRenderer();
  }

  public LeaderboardSnapshot fetch(BenchmarkSite site) {
    return service.get(site);
  }

  public LeaderboardSnapshot fetch(BenchmarkSite site, boolean refresh) {
    return service.get(site, refresh);
  }

  public byte[] renderPng(BenchmarkSite site, ChartOptions options) {
    return renderer.render(fetch(site), options);
  }

  public byte[] renderPng(BenchmarkSite site, ChartOptions options, boolean refresh) {
    return renderer.render(fetch(site, refresh), options);
  }

  public byte[] renderPng(LeaderboardSnapshot snapshot, ChartOptions options) {
    return renderer.render(snapshot, options);
  }

  public LeaderboardService service() {
    return service;
  }
}
