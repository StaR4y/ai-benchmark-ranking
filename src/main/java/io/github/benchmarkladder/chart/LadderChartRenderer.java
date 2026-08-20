package io.github.benchmarkladder.chart;

import io.github.benchmarkladder.model.BenchmarkSite;
import io.github.benchmarkladder.model.LeaderboardEntry;
import io.github.benchmarkladder.model.LeaderboardSnapshot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;

public final class LadderChartRenderer {
  public byte[] render(LeaderboardSnapshot snapshot, ChartOptions options) {
    List<LeaderboardEntry> selected = selectEntries(snapshot, options.top());
    Palette palette = Palette.forTheme(options.theme());

    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (LeaderboardEntry entry : selected) {
      String label = label(entry);
      dataset.addValue(entry.score(), primarySeriesName(snapshot.site()), label);
      if (snapshot.site() == BenchmarkSite.PROGRAMBENCH) {
        dataset.addValue(
            entry.secondaryScore() == null ? 0.0 : entry.secondaryScore(),
            "Almost resolved",
            label);
      }
    }

    JFreeChart chart = ChartFactory.createBarChart(
        snapshot.title().toUpperCase(),
        "",
        metricTitle(snapshot.site()),
        dataset,
        PlotOrientation.HORIZONTAL,
        true,
        false,
        false);
    configure(chart, selected, snapshot.site(), palette);
    int height = Math.max(760, 270 + selected.size() * 64);
    try {
      return ChartUtils.encodeAsPNG(chart.createBufferedImage(options.width(), height));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to encode chart as PNG", exception);
    }
  }

  private static void configure(
      JFreeChart chart,
      List<LeaderboardEntry> entries,
      BenchmarkSite site,
      Palette palette) {
    chart.setBackgroundPaint(palette.background());
    chart.setPadding(new RectangleInsets(28, 30, 30, 30));
    chart.getTitle().setPaint(palette.text());
    chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
    chart.getLegend().setBackgroundPaint(palette.background());
    chart.getLegend().setItemPaint(palette.muted());
    chart.getLegend().setItemFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
    chart.setAntiAlias(true);
    chart.setTextAntiAlias(true);

    CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(palette.plot());
    plot.setOutlineVisible(false);
    plot.setRangeGridlinePaint(palette.grid());
    plot.setRangeGridlineStroke(new BasicStroke(1f));
    plot.setDomainGridlinesVisible(false);
    plot.setInsets(new RectangleInsets(20, 18, 12, 22));

    CategoryAxis categoryAxis = plot.getDomainAxis();
    categoryAxis.setTickLabelPaint(palette.text());
    categoryAxis.setTickLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    categoryAxis.setAxisLineVisible(false);
    categoryAxis.setTickMarksVisible(false);
    categoryAxis.setCategoryMargin(0.24);

    NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
    numberAxis.setLabelPaint(palette.muted());
    numberAxis.setTickLabelPaint(palette.muted());
    numberAxis.setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
    numberAxis.setTickLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    numberAxis.setNumberFormatOverride(new DecimalFormat("0.0'%'"));
    numberAxis.setAxisLineVisible(false);
    numberAxis.setTickMarksVisible(false);
    numberAxis.setRange(0.0, upperBound(entries, site));

    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setShadowVisible(false);
    renderer.setDrawBarOutline(false);
    renderer.setSeriesPaint(0, palette.primary());
    renderer.setSeriesPaint(1, palette.secondary());
    renderer.setItemMargin(0.08);
    renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator(
        "{2}%", new DecimalFormat("0.0")));
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setDefaultItemLabelPaint(palette.text());
    renderer.setDefaultItemLabelFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
  }

  static List<LeaderboardEntry> selectEntries(LeaderboardSnapshot snapshot, int top) {
    Comparator<LeaderboardEntry> ordering = Comparator
        .comparingDouble(LeaderboardEntry::score)
        .reversed()
        .thenComparing(
            entry -> entry.secondaryScore() == null ? Double.NEGATIVE_INFINITY : entry.secondaryScore(),
            Comparator.reverseOrder())
        .thenComparingInt(LeaderboardEntry::rank);
    return snapshot.entries().stream()
        .sorted(ordering)
        .limit(top)
        .toList();
  }

  private static double upperBound(List<LeaderboardEntry> entries, BenchmarkSite site) {
    double maximum = entries.stream()
        .mapToDouble(entry -> site == BenchmarkSite.PROGRAMBENCH && entry.secondaryScore() != null
            ? Math.max(entry.score(), entry.secondaryScore())
            : entry.score())
        .max()
        .orElse(100.0);
    return Math.max(10.0, Math.ceil(maximum * 1.18 / 5.0) * 5.0);
  }

  private static String label(LeaderboardEntry entry) {
    String suffix = entry.agent() == null || entry.agent().isBlank()
        ? ""
        : "  ·  " + entry.agent();
    return "%02d  %s%s".formatted(entry.rank(), shorten(entry.model(), 38), suffix);
  }

  private static String shorten(String value, int maxLength) {
    return value.length() <= maxLength
        ? value
        : value.substring(0, maxLength - 3) + "...";
  }

  private static String metricTitle(BenchmarkSite site) {
    return site == BenchmarkSite.PROGRAMBENCH
        ? "TASK RATE (%)"
        : "RESOLUTION RATE (%)";
  }

  private static String primarySeriesName(BenchmarkSite site) {
    return site == BenchmarkSite.PROGRAMBENCH ? "Resolved" : "Resolution rate";
  }

  private record Palette(
      Color background,
      Color plot,
      Color grid,
      Color text,
      Color muted,
      Color primary,
      Color secondary) {

    private static Palette forTheme(ChartTheme theme) {
      return switch (theme) {
        case DARK -> colors(
            "#0A0F14", "#0E151D", "#263341", "#D9E7F2", "#91A4B5", "#2DE2E6", "#62F5A8");
        case LIGHT -> colors(
            "#F5F8FA", "#FFFFFF", "#DCE5EC", "#15202B", "#5B6B7B", "#0067C5", "#18A57B");
        case NEON -> colors(
            "#05040A", "#0D0B16", "#2A2140", "#F4F5FF", "#B4ADD0", "#00F0FF", "#FF4FD8");
        case MONO -> colors(
            "#F4F4F4", "#FFFFFF", "#D1D1D1", "#111111", "#555555", "#242424", "#929292");
      };
    }

    private static Palette colors(
        String background,
        String plot,
        String grid,
        String text,
        String muted,
        String primary,
        String secondary) {
      return new Palette(
          Color.decode(background),
          Color.decode(plot),
          Color.decode(grid),
          Color.decode(text),
          Color.decode(muted),
          Color.decode(primary),
          Color.decode(secondary));
    }
  }
}
