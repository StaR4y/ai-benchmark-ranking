package io.github.benchmarkladder.crawler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ParseSupport {
  private static final Pattern NUMBER = Pattern.compile("[-+]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)");
  private static final DateTimeFormatter ENGLISH_DATE =
      DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);

  private ParseSupport() {
  }

  static double firstNumber(String value, String field) {
    Matcher matcher = NUMBER.matcher(value == null ? "" : value.replace(",", ""));
    if (!matcher.find()) {
      throw new CrawlException("Could not parse " + field + " from '" + value + "'");
    }
    return Double.parseDouble(matcher.group());
  }

  static int integer(String value, String field) {
    return (int) firstNumber(value, field);
  }

  static LocalDate englishDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim(), ENGLISH_DATE);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }
}
