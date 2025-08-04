package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.dto.EventSnapshotDto;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.EventVersion;
import com.fairing.fairplay.event.entity.ExternalLink;
import com.fairing.fairplay.event.repository.EventVersionRepository;
import com.fairing.fairplay.ticket.dto.TicketSnapshotDto;
import com.fairing.fairplay.ticket.entity.EventTicket;
import com.fairing.fairplay.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventVersionService {

    private final EventVersionRepository eventVersionRepository;

    @Transactional
    public EventVersion createEventVersion(Event event, Long updatedBy) {

        try { // 행사 버전 생성
            int nextVersionNumber = eventVersionRepository.findTopByEventOrderByVersionNumberDesc(event)
                    .map(latestVersion -> latestVersion.getVersionNumber() + 1)
                    .orElse(1);

            EventSnapshotDto snapshotDto = createSnapshotDto(event);

            EventVersion eventVersion = new EventVersion();
            eventVersion.setEvent(event);
            eventVersion.setVersionNumber(nextVersionNumber);
            eventVersion.setSnapshotFromDto(snapshotDto);
            eventVersion.setUpdatedBy(updatedBy);
            eventVersion.setUpdatedAt(LocalDateTime.now());

            return eventVersionRepository.save(eventVersion);
        } catch (DataIntegrityViolationException e) {
            log.error("중복 버전 번호로 인한 제약 조건 위반: " + e.getMessage());
            throw new CustomException(HttpStatus.CONFLICT, "동시 요청으로 인해 버전 생성에 실패했습니다. 다시 시도해주세요.", e);
        }

    }

    private EventSnapshotDto createSnapshotDto(Event event) {
        if (event == null) {
            return null;
        }

        EventDetail detail = event.getEventDetail();
        EventSnapshotDto.EventSnapshotDtoBuilder builder = EventSnapshotDto.builder();

        // from Event
        builder.eventCode(event.getEventCode())
                .titleKr(event.getTitleKr())
                .titleEng(event.getTitleEng())
                .hidden(event.getHidden())
                .managerId(Optional.ofNullable(event.getManager()).map(m -> m.getUser().getUserId()).orElse(null))
                .eventStatusCodeId(Optional.ofNullable(event.getStatusCode()).map(s -> s.getEventStatusCodeId()).orElse(null));

        // tickets from Event
        Hibernate.initialize(event.getEventTickets());
        List<Ticket> tickets = Optional.ofNullable(event.getEventTickets()).orElse(Collections.emptySet()).stream()
                .map(EventTicket::getTicket)
                .toList();
        List<TicketSnapshotDto> ticketSnapshots = tickets.stream()
                .map(this::createTicketSnapshotDto)
                .toList();
        builder.tickets(ticketSnapshots);

        // from EventDetail (존재하는 경우)
        if (detail != null) {
            builder.locationId(Optional.ofNullable(detail.getLocation()).map(l -> l.getLocationId()).orElse(null))
                    .locationDetail(detail.getLocationDetail())
                    .hostName(detail.getHostName())
                    .contactInfo(detail.getContactInfo())
                    .bio(detail.getBio())
                    .content(detail.getContent())
                    .policy(detail.getPolicy())
                    .officialUrl(detail.getOfficialUrl())
                    .eventTime(detail.getEventTime())
                    .thumbnailUrl(detail.getThumbnailUrl())
                    .startDate(detail.getStartDate())
                    .endDate(detail.getEndDate())
                    .reentryAllowed(detail.getReentryAllowed())
                    .checkOutAllowed(detail.getCheckOutAllowed())
                    .mainCategoryId(Optional.ofNullable(detail.getMainCategory()).map(mc -> mc.getGroupId()).orElse(null))
                    .subCategoryId(Optional.ofNullable(detail.getSubCategory()).map(sc -> sc.getCategoryId()).orElse(null))
                    .regionCodeId(Optional.ofNullable(detail.getRegionCode()).map(rc -> rc.getRegionCodeId()).orElse(null));

            // external links from EventDetail
            Hibernate.initialize(event.getExternalLinks());
            List<ExternalLink> externalLinks = Optional.ofNullable(event.getExternalLinks()).orElse(Collections.emptySet()).stream().toList();
            builder.externalLinks(externalLinks.stream()
                    .map(link -> new EventSnapshotDto.ExternalLinkSnapshot(link.getUrl(), link.getDisplayText()))
                    .toList());
        } else {
            builder.externalLinks(Collections.emptyList());
        }

        return builder.build();
    }

    private TicketSnapshotDto createTicketSnapshotDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketSnapshotDto.builder()
                .name(ticket.getName())
                .price(ticket.getPrice())
                .stock(ticket.getStock())
                .build();
    }
}