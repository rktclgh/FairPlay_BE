package com.fairing.fairplay.ticket.controller;

import com.fairing.fairplay.ticket.dto.ScheduleTicketRequestDto;
import com.fairing.fairplay.ticket.dto.ScheduleTicketResponseDto;
import com.fairing.fairplay.ticket.service.ScheduleTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/schedule/{scheduleId}/tickets")
@RequiredArgsConstructor
public class ScheduleTicketController {

    private final ScheduleTicketService scheduleTicketService;

    // 회차별 티켓 설정 등록
    @PostMapping
    public ResponseEntity<Void> registerScheduleTicket(
            @PathVariable Long eventId,
            @PathVariable Long scheduleId,
            @RequestBody List<ScheduleTicketRequestDto> ticketList) {

        scheduleTicketService.registerScheduleTicket(eventId, scheduleId, ticketList);

        return ResponseEntity.ok().build();
    }

    // 회차별 티켓 설정 조회
    @GetMapping
    public ResponseEntity<List<ScheduleTicketResponseDto>> getScheduleTickets(
            @PathVariable Long eventId,
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleTicketService.getScheduleTickets(eventId, scheduleId));
    }
}
