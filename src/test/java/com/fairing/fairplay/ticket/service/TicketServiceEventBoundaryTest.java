package com.fairing.fairplay.ticket.service;

import com.fairing.fairplay.event.repository.EventTicketRepository;
import com.fairing.fairplay.ticket.dto.TicketRequestDto;
import com.fairing.fairplay.ticket.entity.EventTicketId;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.entity.TicketAudienceType;
import com.fairing.fairplay.ticket.entity.TicketSeatType;
import com.fairing.fairplay.ticket.entity.TicketStatusCode;
import com.fairing.fairplay.ticket.repository.TicketAudienceTypeRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.ticket.repository.TicketSeatTypeRepository;
import com.fairing.fairplay.ticket.repository.TicketStatusCodeRepository;
import com.fairing.fairplay.ticket.repository.TicketVersionRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceEventBoundaryTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventTicketRepository eventTicketRepository;

    @Mock
    private TicketVersionRepository ticketVersionRepository;

    @Mock
    private TicketStatusCodeRepository ticketStatusCodeRepository;

    @Mock
    private TicketAudienceTypeRepository ticketAudienceTypeRepository;

    @Mock
    private TicketSeatTypeRepository ticketSeatTypeRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                eventRepository,
                eventTicketRepository,
                ticketVersionRepository,
                ticketStatusCodeRepository,
                ticketAudienceTypeRepository,
                ticketSeatTypeRepository
        );
    }

    @Test
    void updateTicketRejectsTicketOutsidePathEventBeforeVersionWrite() {
        when(eventTicketRepository.existsById(new EventTicketId(99L, 1L))).thenReturn(false);

        assertThatThrownBy(() -> ticketService.updateTicket(1L, 99L, ticketRequest(), 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("티켓");

        verify(ticketRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(ticketVersionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteTicketRejectsTicketOutsidePathEventWithoutSoftDeleting() {
        Ticket ticket = ticket(99L);
        when(eventTicketRepository.existsById(new EventTicketId(99L, 1L))).thenReturn(false);

        assertThatThrownBy(() -> ticketService.deleteTicket(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("티켓");

        assertThat(ticket.getDeleted()).isFalse();
        verify(ticketRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateTicketAllowsTicketInsidePathEvent() {
        Ticket ticket = ticket(99L);
        when(eventTicketRepository.existsById(new EventTicketId(99L, 1L))).thenReturn(true);
        when(ticketRepository.findById(99L)).thenReturn(Optional.of(ticket));
        when(ticketAudienceTypeRepository.findByCode("ADULT"))
                .thenReturn(Optional.of(new TicketAudienceType(1, "ADULT", "성인")));
        when(ticketSeatTypeRepository.findByCode("GENERAL"))
                .thenReturn(Optional.of(new TicketSeatType(1, "GENERAL", "일반")));
        when(ticketStatusCodeRepository.findByCode("AVAILABLE"))
                .thenReturn(Optional.of(new TicketStatusCode(1, "AVAILABLE", "판매중")));

        assertThat(ticketService.updateTicket(1L, 99L, ticketRequest(), 300L).getTicketId())
                .isEqualTo(99L);
    }

    private TicketRequestDto ticketRequest() {
        return TicketRequestDto.builder()
                .name("수정 티켓")
                .price(1000)
                .maxPurchase(2)
                .audienceType("ADULT")
                .seatType("GENERAL")
                .ticketStatusCode("AVAILABLE")
                .build();
    }

    private Ticket ticket(Long ticketId) {
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setName("티켓");
        ticket.setPrice(1000);
        ticket.setStock(10);
        ticket.setMaxPurchase(2);
        ticket.setDeleted(false);
        return ticket;
    }
}
