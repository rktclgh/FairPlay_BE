package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.banner.dto.HotPickDto;
import com.fairing.fairplay.event.service.EventHotPickService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventHotPickController {
    
    private final EventHotPickService eventHotPickService;

    @GetMapping("/hot-picks")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<HotPickDto>> getHotPicks(
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int limit = Math.max(1, Math.min(size, 20));
        return ResponseEntity.ok(eventHotPickService.getHotPicksByReservation(limit));
    }
}