package com.fairing.fairplay.review.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateTimeUtils {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy. MM. dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private DateTimeUtils() {
  }

  public static String formatDate(LocalDate date) {
    return date.format(DATE_FORMATTER);
  }

  public static String formatDayOfWeek(int weekday) {
    int day = (weekday == 0) ? 7 : weekday;
    return DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.KOREAN);
  }

  public static String formatTime(LocalTime time) {
    return time.format(TIME_FORMATTER);
  }
}
