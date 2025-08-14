package com.fairing.fairplay.review.dto;

import com.fairing.fairplay.review.util.DateTimeUtils;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 행사 정보
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {

  private String title;
  private String buildingName;
  private String address;
  private String thumbnail;
  private ViewingScheduleInfo viewingScheduleInfo;
  private EventScheduleInfo eventScheduleInfo;

  public EventDto(String title, String buildingName, String address, String thumbnail,
      LocalDate scheduleDate, int weekday, LocalTime startTime,
      LocalDate startDate, LocalDate endDate) {
    this.title = title;
    this.buildingName = buildingName;
    this.address = address;
    this.thumbnail = thumbnail;
    this.viewingScheduleInfo = new ViewingScheduleInfo(
        DateTimeUtils.formatDate(scheduleDate),
        DateTimeUtils.formatDayOfWeek(weekday),
        DateTimeUtils.formatTime(startTime)
    );
    this.eventScheduleInfo = new EventScheduleInfo(
        DateTimeUtils.formatDate(startDate),
        DateTimeUtils.formatDate(endDate)
    );
  }

  // 관람 일시 정보
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ViewingScheduleInfo {

    private String date;
    private String dayOfWeek;
    private String startTime;
  }

  // 행사 시작-끝 정보
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class EventScheduleInfo {

    private String startDate;
    private String endDate;
  }
}