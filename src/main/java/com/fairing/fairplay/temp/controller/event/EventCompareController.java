package com.fairing.fairplay.temp.controller.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.temp.dto.event.EventCompareDto;
import com.fairing.fairplay.temp.dto.event.Top3EventCompareDto;
import com.fairing.fairplay.temp.repository.event.EventCompareRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/event-compare/")
@RequiredArgsConstructor
public class EventCompareController {

    private final EventCompareRepository eventCompareRepository;

    @GetMapping("/list")
    public Page<EventCompareDto> getEventComparisonList(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventCompareRepository.getEventComparisonDataWithPaging(status, pageable);
    }

    @GetMapping("/top3")
    public Top3EventCompareDto getTop3EventComparisonList() {
        return eventCompareRepository.getTop3EventComparisonList();
    }
}
