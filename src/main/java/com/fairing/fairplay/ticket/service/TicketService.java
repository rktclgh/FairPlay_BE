package com.fairing.fairplay.ticket.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.ticket.dto.TicketRequestDto;
import com.fairing.fairplay.ticket.dto.TicketResponseDto;
import com.fairing.fairplay.ticket.dto.TicketSnapshotDto;
import com.fairing.fairplay.ticket.entity.*;
import com.fairing.fairplay.event.repository.EventTicketRepository;
import com.fairing.fairplay.ticket.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final EventTicketRepository eventTicketRepository;
    private final TicketVersionRepository ticketVersionRepository;
    private final TicketStatusCodeRepository ticketStatusCodeRepository;
    private final TicketAudienceTypeRepository ticketAudienceTypeRepository;
    private final TicketSeatTypeRepository ticketSeatTypeRepository;

    /**
     * 특정 행사의 티켓 정보 저장
     *
     * @param eventId 이벤트 ID
     * @param dto     티켓 생성 요청 DTO
     * @return 생성된 티켓 정보
     */
    @Transactional
    public TicketResponseDto createTicket(Long eventId, @Valid TicketRequestDto dto, Long userId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행사 아이디: " + eventId));


        TicketAudienceType ticketAudienceType = ticketAudienceTypeRepository.findByCode(dto.getAudienceType())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 유형 : " + dto.getAudienceType()));

        TicketSeatType ticketSeatType = ticketSeatTypeRepository.findByCode(dto.getSeatType())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석 유형 : " + dto.getSeatType()));

        TicketStatusCode ticketStatusCode = ticketStatusCodeRepository.findByCode(dto.getTicketStatusCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태 유형 : " + dto.getTicketStatusCode()));

        // Ticket 저장
        Ticket ticket = new Ticket();
        ticket.setName(dto.getName());
        ticket.setDescription(dto.getDescription());
        ticket.setStock(dto.getStock());
        ticket.setPrice(dto.getPrice());
        ticket.setMaxPurchase(dto.getMaxPurchase());
        ticket.setTypes(TypesEnum.EVENT);
        ticket.setTicketAudienceType(ticketAudienceType);
        ticket.setTicketSeatType(ticketSeatType);
        ticket.setTicketStatusCode(ticketStatusCode);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setDeleted(false);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Ticket version 저장
        TicketVersion ticketVersion = new TicketVersion();
        ticketVersion.setTicket(savedTicket);
        ticketVersion.setVersionNumber(1);
        ticketVersion.setUpdatedBy(userId);

        TicketSnapshotDto snapshotDto = new TicketSnapshotDto(savedTicket);
        ticketVersion.setSnapshotFromDto(snapshotDto);

        ticketVersionRepository.save(ticketVersion);

        // Event_Ticket 저장
        EventTicket eventTicket = new EventTicket(savedTicket, event);
        eventTicketRepository.save(eventTicket);

        return new TicketResponseDto(savedTicket);
    }

    /**
     * 특정 행사의 티켓 목록 조회
     *
     * @param eventId 조회할 이벤트의 ID
     * @param audienceType 티켓 유형 (성인, 청소년, 어린이 등)
     * @param seatType 좌석 유형 (VIP석, R석 등)
     * @param searchTicketName 티켓명 검색어
     * @return 티켓 정보 목록 (삭제되지 않은 티켓만 반환)
     */
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTickets(Long eventId, String audienceType, String seatType, String searchTicketName) {
        return ticketRepository.findTicketsByEventIdWithFilters(eventId, audienceType, seatType, searchTicketName)
                .stream()
                .filter(ticket -> !ticket.getDeleted())
                .map(ticket -> new TicketResponseDto(ticket))
                .toList();
    }

    /**
     * 특정 행사의 티켓 정보 수정
     *
     * @param eventId  이벤트 ID
     * @param ticketId 티켓 ID
     * @param dto      티켓 수정 요청 DTO
     * @param userId
     * @return 수정된 티켓 정보
     */
    @Transactional
    public TicketResponseDto updateTicket(Long eventId, Long ticketId, TicketRequestDto dto, Long userId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 아이디: " + ticketId));

        TicketAudienceType ticketAudienceType = ticketAudienceTypeRepository.findByCode(dto.getAudienceType())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 유형 : " + dto.getAudienceType()));

        TicketSeatType ticketSeatType = ticketSeatTypeRepository.findByCode(dto.getSeatType())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석 유형 : " + dto.getSeatType()));

        TicketStatusCode ticketStatusCode = ticketStatusCodeRepository.findByCode(dto.getTicketStatusCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태 유형 : " + dto.getTicketStatusCode()));


        // 1. 티켓 엔티티 수정
        ticket.setName(dto.getName());
        ticket.setPrice(dto.getPrice());
        ticket.setMaxPurchase(dto.getMaxPurchase());
        ticket.setTicketAudienceType(ticketAudienceType);
        ticket.setTicketSeatType(ticketSeatType);
        ticket.setTicketStatusCode(ticketStatusCode);

        // 2. 버전 번호 계산 (MAX + 1)
        Integer latestVersion = ticketVersionRepository.findMaxVersionByTicket(ticket.getTicketId());
        int newVersion = (latestVersion == null ? 1 : latestVersion + 1);

        // 3. 수정된 엔티티 상태를 스냅샷으로 변환
        TicketSnapshotDto snapshotDto = new TicketSnapshotDto(ticket);

        // 4. TicketVersion 저장
        TicketVersion ticketVersion = new TicketVersion();
        ticketVersion.setTicket(ticket);
        ticketVersion.setVersionNumber(newVersion);
        ticketVersion.setSnapshotFromDto(snapshotDto);
        ticketVersion.setUpdatedBy(userId);
        ticketVersion.setUpdatedAt(LocalDateTime.now());
        ticketVersionRepository.save(ticketVersion);

        return TicketResponseDto.builder()
                .ticketId(ticket.getTicketId())
                .name(ticket.getName())
                .description(ticket.getDescription())
                .price(ticket.getPrice())
                .stock(ticket.getStock())
                .maxPurchase(ticket.getMaxPurchase())
                .audienceTypeCode(ticketAudienceType.getCode())
                .audienceTypeName(ticketAudienceType.getName())
                .seatTypeCode(ticketSeatType.getCode())
                .seatTypeName(ticketSeatType.getName())
                .ticketStatusCode(ticketStatusCode.getCode())
                .ticketStatusName(ticketStatusCode.getName())
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    /**
     * 특정 행사의 티켓 삭제 (소프트 삭제)
     *
     * @param eventId  이벤트 ID
     * @param ticketId 티켓 ID
     */
    @Transactional
    public void deleteTicket(Long eventId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 아이디: " + ticketId));
        ticket.setDeleted(true);
    }
}
