package com.fairing.fairplay.scheduler;

import com.fairing.fairplay.attendee.service.AttendeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendeeScheduler {
  private final AttendeeService attendeeService;

  @Scheduled(cron = "0 0 4 * * *")
  public void deleteOldAttendees() {
    attendeeService.deleteOldAttendees();
  }
}
