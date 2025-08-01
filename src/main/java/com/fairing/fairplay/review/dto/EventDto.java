package com.fairing.fairplay.review.dto;

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
  private ViewingScheduleInfo viewingScheduleInfo;
  private EventScheduleInfo eventScheduleInfo;

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
  public static class EventScheduleInfo{
    private String startDate;
    private String endDate;
  }
}
