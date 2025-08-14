package com.fairing.fairplay.event.service;

import com.fairing.fairplay.event.dto.CalendarEventDto;
import com.fairing.fairplay.event.dto.CalendarEventSummaryDto;
import com.fairing.fairplay.event.dto.CalendarGroupedDto;
import com.fairing.fairplay.event.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    /**
     * 리스트형(카드 뷰 등)에서 사용: 해당 월 이벤트 목록
     */
    public List<CalendarEventDto> getMonthlyEvents(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        return calendarEventRepository.findEventsByMonth(start, end);
    }

    /**
     * 캘린더형에서 사용: 이벤트 기간을 날짜별로 펼쳐서(clip 포함) 그룹핑하여 반환
     * (각 날짜에 {eventId, title} 목록 포함 → 셀 클릭 시 상세 페이지 이동 가능)
     */
    public List<CalendarGroupedDto> getGroupedEventsByDate(int year, int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth   = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        List<CalendarEventDto> events =
                calendarEventRepository.findEventsByMonth(startOfMonth, endOfMonth);

        // 날짜 → (해당 날짜의 이벤트 요약 리스트)
        Map<LocalDate, List<CalendarEventSummaryDto>> byDate = new HashMap<>();

        for (CalendarEventDto ev : events) {
            // 월 범위로 클리핑
            LocalDate s = ev.getStartDate().isBefore(startOfMonth) ? startOfMonth : ev.getStartDate();
            LocalDate e = ev.getEndDate().isAfter(endOfMonth) ? endOfMonth : ev.getEndDate();

            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                byDate.computeIfAbsent(d, k -> new ArrayList<>())
                        .add(new CalendarEventSummaryDto(ev.getEventId(), ev.getTitle()));
            }
        }

        // 각 날짜별 중복 제거 + 정렬(원하는 기준으로 조절 가능)
        byDate.replaceAll((date, list) -> {
            // 중복 제거(동일 eventId/title 조합 제거)
            List<CalendarEventSummaryDto> dedup = new ArrayList<>(new LinkedHashSet<>(list));
            // 정렬: eventId → title
            dedup.sort(Comparator
                    .comparing(CalendarEventSummaryDto::getEventId)
                    .thenComparing(CalendarEventSummaryDto::getTitle)
            );
            return dedup;
        });

        // 날짜 오름차순으로 반환
        return byDate.entrySet().stream()
                .map(e -> new CalendarGroupedDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(CalendarGroupedDto::getDate))
                .collect(Collectors.toList());
    }

    /**
     * 오늘 기준 월을 반환(필요 시 유지)
     */
    public List<CalendarGroupedDto> getGroupedEventsByUser(Long userId) {
        LocalDate today = LocalDate.now();
        return getGroupedEventsByDate(today.getYear(), today.getMonthValue());
    }

    /**
     * 리스트형(유저 기준) – 현재는 필터 미적용
     */
    public List<CalendarEventDto> getMonthlyEventsForUser(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        return calendarEventRepository.findEventsByMonth(start, end);
    }
}
