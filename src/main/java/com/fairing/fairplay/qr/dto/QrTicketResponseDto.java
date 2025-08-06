package com.fairing.fairplay.qr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrTicketResponseDto {

  private String title; // 행사명
  private String buildingName; //행사 장소
  private String qrCode; // qr이미지 코드
  private String manualCode; // 수동 코드
  private String ticketNo; // 티켓 번호
  private ViewingScheduleInfo viewingScheduleInfo; // 관람 일시 정보
  private String reservationDate; // 예매일

  /**
   * QueryDSL JPQL 프로젝션용 생성자
   *
   * @see com.fairing.fairplay.qr.repository.QrTicketRepository#findDtoById(Long)
   */
  public QrTicketResponseDto(String title, String buildingName,
      String ticketNo, String qrCode, String manualCode) {
    this.title = title;
    this.buildingName = buildingName;
    this.ticketNo = ticketNo;
    this.qrCode = qrCode;
    this.manualCode = manualCode;
  }

  // 관람 일시 정보
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ViewingScheduleInfo {

    private String date; // 날짜
    private String dayOfWeek; // 요일
    private String startTime; // 시작 시간
  }
}
