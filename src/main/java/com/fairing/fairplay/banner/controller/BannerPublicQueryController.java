// com.fairing.fairplay.banner.controller.BannerPublicQueryController.java
package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.FixedTopDto;
import com.fairing.fairplay.banner.service.SearchTopQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerPublicQueryController {

    private final SearchTopQueryService searchTopQueryService;

    @GetMapping("/search-top")
    public List<FixedTopDto> getSearchTop(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate d = (date != null) ? date : LocalDate.now();
        return searchTopQueryService.getFixedTwo(d);
    }
}
