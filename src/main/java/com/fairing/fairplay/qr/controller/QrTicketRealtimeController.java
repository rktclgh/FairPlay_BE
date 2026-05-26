package com.fairing.fairplay.qr.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.realtime.service.RealtimeSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qr-tickets")
public class QrTicketRealtimeController {

    private final RealtimeSseService realtimeSseService;

    @GetMapping(value = "/{qrTicketId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamQrTicketStatus(
            @PathVariable Long qrTicketId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(realtimeSseService.openQrTicketStream(qrTicketId, userDetails.getUserId()));
    }
}
