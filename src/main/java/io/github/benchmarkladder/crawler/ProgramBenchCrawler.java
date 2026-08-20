package io.github.benchmarkladder.crawler;

import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardEntry;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class ProgramBenchCrawler implements LeaderboardCrawler {
  private static final BenchmarkSite SITE = BenchmarkSite.PROGRAMBENCH;

  private final HttpPageClient pageClient;

  public ProgramBenchCrawler(HttpPageClient pageClient) {
    this.pageClient = pageClient;
  }

  @Override
  public BenchmarkSite site() {
    return SITE;
  }

  @Override
  public LeaderboardSnapshot crawl() {
    return parse(pageClient.get(SITE.sourceUrl()), Instant.now());
  }

  LeaderboardSnapshot parse(String html, Instant fetchedAt) {
    Document document = Jsoup.parse(html, SITE.sourceUrl());
    Elements rows = document.select("table.lb-table tbody tr.clickable-row");
    if (rows.isEmpty()) {
      rows = document.select("table tbody tr.clickable-row");
    }
    List<LeaderboardEntry> entries = new ArrayList<>();
    for (Element row : rows) {
      Elements cells = row.select("td");
      if (cells.size() < 6) {
        continue;
      }
      String organization = row.attr("data-provider");
      if (organization.isBlank()) {
        organization = row.select(".col-logo img").attr("alt");
      }
      entries.add(new LeaderboardEntry(
          ParseSupport.integer(cells.get(0).text(), "rank"),
          requiredText(row, ".model-name", "model"),
          organization,
          cells.get(3).text(),
          ParseSupport.firstNumber(cells.get(4).text(), "resolved score"),
          ParseSupport.firstNumber(cells.get(5).text(), "almost-resolved score"),
          null,
          null,
          null,
          null,
          row.absUrl("data-href"),
          Map.of("primaryMetric", "resolved", "secondaryMetric", "almostResolved")));
    }
    if (entries.isEmpty()) {
      throw new CrawlException("ProgramBench leaderboard rows were not found; the page layout may have changed");
    }
    return new LeaderboardSnapshot(
        SITE,
        "ProgramBench Leaderboard",
        SITE.sourceUrl(),
        fetchedAt,
        entries);
  }

  private static String requiredText(Element row, String selector, String field) {
    Element element = row.selectFirst(selector);
    if (element == null || element.text().isBlank()) {
      throw new CrawlException("ProgramBench row is missing " + field);
    }
    return element.text();
  }
}
