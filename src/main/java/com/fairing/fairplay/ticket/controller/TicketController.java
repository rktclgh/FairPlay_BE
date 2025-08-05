package com.fairing.fairplay.ticket.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.ticket.dto.TicketRequestDto;
import com.fairing.fairplay.ticket.dto.TicketResponseDto;
import com.fairing.fairplay.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // 티켓 정보 저장
    @PostMapping
    public ResponseEntity<TicketResponseDto> createTicket(
            @PathVariable Long eventId,
            @RequestBody TicketRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ticketService.createTicket(eventId, dto, userDetails.getUserId()));
    }
    
    // 티켓 목록 조회
    @GetMapping
    public ResponseEntity<List<TicketResponseDto>> getTickets(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketService.getTickets(eventId));
    }
    
    // 티켓 정보 수정
    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDto> updateTicket(
            @PathVariable Long eventId,
            @PathVariable Long ticketId,
            @RequestBody TicketRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ticketService.updateTicket(eventId, ticketId, dto, userDetails.getUserId()));
    }

    // 티켓 삭제
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Long eventId,
            @PathVariable Long ticketId) {
        ticketService.deleteTicket(eventId, ticketId);
        return ResponseEntity.noContent().build();
    }
}
