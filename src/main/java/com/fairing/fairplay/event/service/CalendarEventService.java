package com.fairing.fairplay.event.service;

import com.fairing.fairplay.event.dto.CalendarEventDto;
import com.fairing.fairplay.event.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.fairing.fairplay.event.dto.CalendarGroupedDto;


@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    public List<CalendarEventDto> getMonthlyEvents(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return calendarEventRepository.findEventsByMonth(start, end);
    }

    public List<CalendarGroupedDto> getGroupedEventsByDate(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<CalendarEventDto> events = calendarEventRepository.findEventsByMonth(start, end);

        // 날짜별 그룹핑
        return events.stream()
                .collect(Collectors.groupingBy(
                        CalendarEventDto::getStartDate,
                        Collectors.mapping(CalendarEventDto::getTitle, Collectors.toList())
                ))
                .entrySet().stream()
                .map(entry -> new CalendarGroupedDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CalendarGroupedDto::getDate))
                .collect(Collectors.toList());
    }


    public List<CalendarGroupedDto> getGroupedEventsByUser(Long userId) {
        // 현재는 userId는 사용하지 않음. 이후 필터링 로직 필요 시 적용
        LocalDate today = LocalDate.now();
        return getGroupedEventsByDate(today.getYear(), today.getMonthValue());
    }

    public List<CalendarEventDto> getMonthlyEventsForUser(Long userId, int year, int month) {
        // 현재는 userId로 필터링하지 않고 모든 유저에게 동일한 데이터를 반환
        // 추후 userId 기반 필터링이 필요하면 repository 쿼리 수정 필요

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return calendarEventRepository.findEventsByMonth(start, end);
    }

}

