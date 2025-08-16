package com.fairing.fairplay.banner.controller;


import com.fairing.fairplay.banner.batch.HotPickScheduler;
import com.fairing.fairplay.banner.batch.NewPickScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class AdminBannerBatchController {

    private final NewPickScheduler newPickScheduler;

    @PostMapping("/new-picks/run")
    public ResponseEntity<Void> runNewPicks() {
        newPickScheduler.updateNewPicks();
        return ResponseEntity.ok().build();
    }

    private final HotPickScheduler hotPickScheduler;

    @PostMapping("/hot-picks/run")
    public ResponseEntity<Void> runHotPicks() {
        hotPickScheduler.updateHotPicks();
        return ResponseEntity.ok().build();
    }
}