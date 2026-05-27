package com.fairing.fairplay.ticket.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventTicketRepository;
import com.fairing.fairplay.ticket.dto.ScheduleTicketRequestDto;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.EventTicketId;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleTicketServiceEventBoundaryTest {

    @Mock
    private ScheduleTicketRepository scheduleTicketRepository;

    @Mock
    private EventScheduleRepository eventScheduleRepository;

    @Mock
    private EventTicketRepository eventTicketRepository;

    private ScheduleTicketService scheduleTicketService;

    @BeforeEach
    void setUp() {
        scheduleTicketService = new ScheduleTicketService(
                scheduleTicketRepository,
                eventScheduleRepository,
                eventTicketRepository
        );
    }

    @Test
    void registerRejectsScheduleOutsidePathEventBeforeDeletingExistingTickets() {
        when(eventScheduleRepository.findByEvent_EventIdAndScheduleId(1L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleTicketService.registerScheduleTicket(
                1L, 20L, List.of(scheduleTicketRequest(10L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("회차");

        verify(scheduleTicketRepository, never()).deleteByEventSchedule_ScheduleId(any());
        verify(scheduleTicketRepository, never()).saveAll(any());
    }

    @Test
    void registerRejectsTicketOutsidePathEventBeforeDeletingExistingTickets() {
        when(eventScheduleRepository.findByEvent_EventIdAndScheduleId(1L, 20L))
                .thenReturn(Optional.of(schedule(1L, 20L)));
        when(eventTicketRepository.existsById(new EventTicketId(99L, 1L))).thenReturn(false);

        assertThatThrownBy(() -> scheduleTicketService.registerScheduleTicket(
                1L, 20L, List.of(scheduleTicketRequest(99L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("티켓");

        verify(scheduleTicketRepository, never()).deleteByEventSchedule_ScheduleId(any());
        verify(scheduleTicketRepository, never()).saveAll(any());
    }

    private ScheduleTicketRequestDto scheduleTicketRequest(Long ticketId) {
        ScheduleTicketRequestDto requestDto = new ScheduleTicketRequestDto();
        requestDto.setTicketId(ticketId);
        requestDto.setSaleQuantity(10);
        requestDto.setSalesStartAt(LocalDateTime.now().minusDays(1));
        requestDto.setSalesEndAt(LocalDateTime.now().plusDays(1));
        requestDto.setVisible(true);
        return requestDto;
    }

    private EventSchedule schedule(Long eventId, Long scheduleId) {
        Event event = new Event();
        event.setEventId(eventId);

        EventSchedule schedule = new EventSchedule();
        schedule.setScheduleId(scheduleId);
        schedule.setEvent(event);
        return schedule;
    }
}
