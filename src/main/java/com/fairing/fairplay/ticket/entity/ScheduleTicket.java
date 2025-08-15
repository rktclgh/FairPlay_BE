package com.fairing.fairplay.ticket.entity;

import com.fairing.fairplay.ticket.dto.ScheduleTicketRequestDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "schedule_ticket")
public class ScheduleTicket {
    
    @EmbeddedId
    private ScheduleTicketId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("scheduleId")
    @JoinColumn(name = "schedule_id")
    private EventSchedule eventSchedule;

    @Column(name = "remaining_stock")
    private Integer remainingStock;  // 실제 재고 (시스템 관리용)

    @Column(name = "sale_quantity")
    private Integer saleQuantity;    // 판매할 수량 (관리자 설정)

    @Column(name = "sales_start_at")
    private LocalDateTime salesStartAt;

    @Column(name = "sales_end_at")
    private LocalDateTime salesEndAt;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean visible = false;

    public ScheduleTicket(Ticket ticket, EventSchedule eventSchedule) {
        this.ticket = ticket;
        this.eventSchedule = eventSchedule;
        this.id = new ScheduleTicketId(ticket.getTicketId(), eventSchedule.getScheduleId());
    }

    public static List<ScheduleTicket> fromList(List<ScheduleTicketRequestDto> ticketList, Long eventId, Long scheduleId) {
        List<ScheduleTicket> tickets = new ArrayList<>();

        ticketList.forEach(dto -> {
            Ticket ticket = Ticket.builder()
                    .ticketId(dto.getTicketId())
                    .build();

            EventSchedule schedule = EventSchedule.builder()
                    .scheduleId(scheduleId)
                    .build();

            ScheduleTicket scheduleTicket = ScheduleTicket.builder()
                    .id(new ScheduleTicketId(dto.getTicketId(), scheduleId))
                    .ticket(ticket)
                    .eventSchedule(schedule)
                    .remainingStock(dto.getSaleQuantity()) // 초기 재고는 판매 수량으로 설정
                    .saleQuantity(dto.getSaleQuantity())
                    .salesStartAt(dto.getSalesStartAt())
                    .salesEndAt(dto.getSalesEndAt())
                    .visible(dto.getVisible())
                    .build();

            tickets.add(scheduleTicket);
        });

        return tickets;
    }
}