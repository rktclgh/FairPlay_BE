package com.fairing.fairplay.ticket.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.ticket.dto.TicketRequestDto;
import com.fairing.fairplay.ticket.dto.TicketResponseDto;
import com.fairing.fairplay.ticket.service.TicketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events/{eventId}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // 티켓 정보 저장
    @PostMapping
    @FunctionAuth("createTicket")
    public ResponseEntity<TicketResponseDto> createTicket(
            @PathVariable Long eventId,
            @RequestBody TicketRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ticketService.createTicket(eventId, dto, userDetails.getUserId()));
    }

    // 티켓 목록 조회
    @GetMapping
    @FunctionAuth("getTicketList")
    public ResponseEntity<List<TicketResponseDto>> getTickets(
            @PathVariable Long eventId,
            @RequestParam(required = false) String audienceType,
            @RequestParam(required = false) String seatType,
            @RequestParam(required = false) String searchTicketName) {
        return ResponseEntity.ok(ticketService.getTickets(eventId, audienceType, seatType, searchTicketName));
    }

    // 티켓 정보 수정
    @PutMapping("/{ticketId}")
    @FunctionAuth("updateTicket")
    public ResponseEntity<TicketResponseDto> updateTicket(
            @PathVariable Long eventId,
            @PathVariable Long ticketId,
            @RequestBody TicketRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(ticketService.updateTicket(eventId, ticketId, dto, userDetails.getUserId()));
    }

    // 티켓 삭제
    @DeleteMapping("/{ticketId}")
    @FunctionAuth("deleteTicket")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Long eventId,
            @PathVariable Long ticketId) {
        ticketService.deleteTicket(eventId, ticketId);
        return ResponseEntity.noContent().build();
    }
}
