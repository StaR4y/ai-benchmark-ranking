package io.github.benchmarkladder.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class BenchmarkLadderCommandTest {
  @Test
  void snapshotHelpDocumentsAllThemesOption() {
    String usage = new CommandLine(new BenchmarkLadderCommand.SnapshotCommand())
        .getUsageMessage();

    assertThat(usage).contains("--all-themes");
  }
}
