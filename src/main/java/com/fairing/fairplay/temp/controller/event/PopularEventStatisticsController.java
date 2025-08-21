package com.fairing.fairplay.temp.controller.event;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.temp.dto.event.EventCategoryStatisticsDto;
import com.fairing.fairplay.temp.dto.event.PopularEventStatisticsDto;
import com.fairing.fairplay.temp.dto.event.Top5EventStatisticsDto;
import com.fairing.fairplay.temp.repository.event.PopularEventStatisticsRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/popular-events")
@RequiredArgsConstructor
public class PopularEventStatisticsController {

    private final PopularEventStatisticsRepository eventStatisticsRepository;

    @GetMapping("/get-popular-statistics")
    public ResponseEntity<PopularEventStatisticsDto> getPopularStatistics() {
        PopularEventStatisticsDto statistics = eventStatisticsRepository.getPopularEvents();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-category-statistics")
    public ResponseEntity<List<EventCategoryStatisticsDto>> getCategoryStatistics() {
        List<EventCategoryStatisticsDto> statistics = eventStatisticsRepository.getCategoryEventStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/get-top5/{code}")
    public ResponseEntity<List<Top5EventStatisticsDto>> getTop5Events(@PathVariable int code) {
        List<Top5EventStatisticsDto> statistics = eventStatisticsRepository.getTop5Events(code);
        return ResponseEntity.ok(statistics);
    }
}
