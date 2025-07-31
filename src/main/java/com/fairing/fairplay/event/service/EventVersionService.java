package com.fairing.fairplay.event.service;

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
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventVersionService {

    private final EventVersionRepository eventVersionRepository;

    @Transactional
    public EventVersion createEventVersion(Event event, Long updatedBy) {
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
                .collect(Collectors.toList());
        List<TicketSnapshotDto> ticketSnapshots = tickets.stream()
                .map(this::createTicketSnapshotDto)
                .collect(Collectors.toList());
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
                    .mainCategoryId(Optional.ofNullable(detail.getMainCategory()).map(mc -> mc.getGroupId()).orElse(null))
                    .subCategoryId(Optional.ofNullable(detail.getSubCategory()).map(sc -> sc.getCategoryId()).orElse(null))
                    .regionCodeId(Optional.ofNullable(detail.getRegionCode()).map(rc -> rc.getRegionCodeId()).orElse(null));

            // external links from EventDetail
            Hibernate.initialize(event.getExternalLinks());
            List<ExternalLink> externalLinks = Optional.ofNullable(event.getExternalLinks()).orElse(Collections.emptySet()).stream().collect(Collectors.toList());
            builder.externalLinks(externalLinks.stream()
                    .map(link -> new EventSnapshotDto.ExternalLinkSnapshot(link.getUrl(), link.getDisplayText()))
                    .collect(Collectors.toList()));
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