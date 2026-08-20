package io.github.benchmarkladder.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benchmarkladder.BenchmarkLadder;
import io.github.benchmarkladder.api.ApiServer;
import io.github.benchmarkladder.chart.ChartOptions;
import io.github.benchmarkladder.chart.ChartTheme;
import io.github.benchmarkladder.config.AppConfig;
import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardEntry;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "benchmark-ladder",
    description = "Crawl and visualize ProgramBench and FrontierBench leaderboards.",
    mixinStandardHelpOptions = true,
    version = "benchmark-ladder 1.0.0",
    subcommands = {
        BenchmarkLadderCommand.FetchCommand.class,
        BenchmarkLadderCommand.ChartCommand.class,
        BenchmarkLadderCommand.SnapshotCommand.class,
        BenchmarkLadderCommand.ServeCommand.class
    })
public final class BenchmarkLadderCommand implements Runnable {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new BenchmarkLadderCommand()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    new CommandLine(this).usage(System.out);
  }

  @Command(name = "fetch", description = "Print one leaderboard as JSON.")
  static final class FetchCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "programbench or frontierbench")
    private String site;

    @Option(names = "--refresh", description = "Bypass the in-memory cache.")
    private boolean refresh;

    @Override
    public Integer call() throws Exception {
      BenchmarkLadder ladder = new BenchmarkLadder();
      ObjectMapper mapper = ApiServer.objectMapper();
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(System.out, ladder.fetch(BenchmarkSite.from(site), refresh));
      System.out.println();
      return 0;
    }
  }

  @Command(name = "chart", description = "Write one leaderboard ladder chart as PNG.")
  static final class ChartCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "programbench or frontierbench")
    private String site;

    @Option(names = {"-o", "--output"}, required = true, description = "Output PNG path.")
    private Path output;

    @Option(names = "--top", defaultValue = "12", description = "Number of rows (1-50).")
    private int top;

    @Option(names = "--width", defaultValue = "1600", description = "Image width (900-3200).")
    private int width;

    @Option(names = "--theme", defaultValue = "dark",
        description = "Chart theme: dark, light, neon or mono (default: ${DEFAULT-VALUE}).")
    private String theme;

    @Option(names = "--refresh", description = "Bypass the in-memory cache.")
    private boolean refresh;

    @Override
    public Integer call() throws Exception {
      BenchmarkLadder ladder = new BenchmarkLadder();
      byte[] png = ladder.renderPng(
          BenchmarkSite.from(site),
          new ChartOptions(top, width, ChartTheme.from(theme)),
          refresh);
      writeAtomically(output, png);
      System.out.println(output.toAbsolutePath());
      return 0;
    }
  }

  @Command(name = "snapshot", description = "Write stable JSON and PNG artifacts for every site.")
  static final class SnapshotCommand implements Callable<Integer> {
    @Option(names = "--output-dir", defaultValue = ".", description = "Repository/output root.")
    private Path outputDirectory;

    @Option(names = "--top", defaultValue = "12", description = "Number of chart rows (1-50).")
    private int top;

    @Option(names = "--width", defaultValue = "1600", description = "Image width (900-3200).")
    private int width;

    @Option(names = "--theme", defaultValue = "dark",
        description = "Chart theme: dark, light, neon or mono (default: ${DEFAULT-VALUE}).")
    private String theme;

    @Option(names = "--refresh", defaultValue = "true", negatable = true,
        description = "Refresh upstream pages (default: ${DEFAULT-VALUE}).")
    private boolean refresh;

    @Override
    public Integer call() throws Exception {
      BenchmarkLadder ladder = new BenchmarkLadder();
      ObjectMapper mapper = ApiServer.objectMapper();
      Path dataDir = outputDirectory.resolve("data");
      Path chartDir = outputDirectory.resolve("charts");
      Files.createDirectories(dataDir);
      Files.createDirectories(chartDir);

      for (BenchmarkSite site : BenchmarkSite.values()) {
        LeaderboardSnapshot snapshot = ladder.fetch(site, refresh);
        StableSnapshot stable = new StableSnapshot(
            snapshot.site().id(), snapshot.title(), snapshot.sourceUrl(), snapshot.entries());
        byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(stable);
        byte[] png = ladder.renderPng(
            snapshot, new ChartOptions(top, width, ChartTheme.from(theme)));
        writeAtomically(dataDir.resolve(site.id() + ".json"), json);
        writeAtomically(chartDir.resolve(site.id() + ".png"), png);
        System.out.printf("updated %s (%d entries)%n", site.id(), snapshot.entries().size());
      }
      return 0;
    }
  }

  @Command(name = "serve", description = "Start the REST API.")
  static final class ServeCommand implements Callable<Integer> {
    @Option(names = {"-p", "--port"}, description = "HTTP port; defaults to PORT or 7070.")
    private Integer port;

    @Override
    public Integer call() throws InterruptedException {
      AppConfig config = AppConfig.fromEnvironment();
      int effectivePort = port == null ? config.port() : port;
      var app = new ApiServer(new BenchmarkLadder(config)).start(effectivePort);
      System.out.println("Benchmark Ladder API listening on http://localhost:" + effectivePort);
      Runtime.getRuntime().addShutdownHook(new Thread(app::stop, "benchmark-ladder-shutdown"));
      new CountDownLatch(1).await();
      return 0;
    }
  }

  private record StableSnapshot(
      String site,
      String title,
      String sourceUrl,
      java.util.List<LeaderboardEntry> entries) {
  }

  private static void writeAtomically(Path output, byte[] content) throws IOException {
    Path absolute = output.toAbsolutePath();
    Path parent = absolute.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Path temporary = Files.createTempFile(parent, output.getFileName().toString(), ".tmp");
    try {
      Files.write(temporary, content);
      try {
        Files.move(temporary, absolute,
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException atomicMoveUnsupported) {
        Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
