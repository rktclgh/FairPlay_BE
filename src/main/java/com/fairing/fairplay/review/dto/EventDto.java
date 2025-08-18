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
import lombok.extern.slf4j.Slf4j;

// 행사 정보
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class EventDto {

  private String title;
  private String buildingName;
  private String address;
  private String thumbnail;
  private ViewingScheduleInfo viewingScheduleInfo;
  private EventScheduleInfo eventScheduleInfo;

  public EventDto(String title, String buildingName, String address, String thumbnail,
      LocalDate scheduleDate, Integer weekday, LocalTime startTime,
      LocalDate startDate, LocalDate endDate) {
    log.info("EventDto 생성자 title:{}",title);
    log.info("EventDto 생성자 buildingName:{}",buildingName);
    log.info("EventDto 생성자 address:{}",address);
    log.info("EventDto 생성자 thumbnail:{}",thumbnail);
    log.info("EventDto 생성자 scheduleDate:{}",scheduleDate);
    log.info("EventDto 생성자 weekday:{}",weekday);
    log.info("EventDto 생성자 startTime:{}",startTime);
    log.info("EventDto 생성자 startDate:{}",startDate);
    log.info("EventDto 생성자 startTime:{}",endDate);





    this.title = title;
    this.buildingName = buildingName;
    this.address = address;
    this.thumbnail = thumbnail;
    this.viewingScheduleInfo = new ViewingScheduleInfo(
        DateTimeUtils.formatDate(scheduleDate),
        DateTimeUtils.formatDayOfWeek(weekday != null ? weekday : 0),
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