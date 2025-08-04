package com.fairing.fairplay.attendee.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 참석자 리스트
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendeeListInfoResponseDto {
  private Long reservationId;
  private List<AttendeeInfoResponseDto> attendees;
}
