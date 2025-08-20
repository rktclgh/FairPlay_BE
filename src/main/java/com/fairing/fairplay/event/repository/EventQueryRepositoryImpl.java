package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.dto.EventSummaryDto;
import com.fairing.fairplay.event.dto.QEventSummaryDto;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.repository.FileRepository;
import com.fairing.fairplay.ticket.entity.QEventTicket;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EventQueryRepositoryImpl implements EventQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final FileRepository fileRepository;
    private final AwsS3Service awsS3Service;

    @Override
    public Page<EventSummaryDto> findEventSummaries(Pageable pageable) {
        QEvent event = QEvent.event;
        QEventDetail detail = QEventDetail.eventDetail;
        QEventTicket eventTicket = QEventTicket.eventTicket;
        QTicket ticket = QTicket.ticket;

        List<EventSummaryDto> content = queryFactory
                .select(new QEventSummaryDto(
                        event.eventId,
                        event.eventCode,
                        event.hidden,
                        event.titleKr,
                        ticket.price.min(),  // null 허용 (티켓 없을 경우)
                        detail.mainCategory.groupName,
                        detail.location.placeName,
                        detail.location.latitude,
                        detail.location.longitude,
                        detail.startDate,
                        detail.endDate,
                        detail.thumbnailUrl,
                        detail.regionCode.name,
                        event.statusCode.code
                ))
                .from(event)
                .join(event.eventDetail, detail)
                .leftJoin(event.eventTickets, eventTicket)
                .leftJoin(eventTicket.ticket, ticket)
                .where(
                        event.hidden.eq(false),
                        detail.eventDetailId.isNotNull()
                )
                .groupBy(event.eventId)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(event.countDistinct())
                .from(event)
                .leftJoin(event.eventDetail, detail)
                .fetchOne();

        if (!content.isEmpty()) {
            enrichEventSummariesWithFiles(content);
        }

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<EventSummaryDto> findEventSummariesWithFilters(
            String keyword,
            Integer mainCategoryId,
            Integer subCategoryId,
            String regionName,
            LocalDate fromDate,
            LocalDate toDate,
            Boolean includeHidden,
            Pageable pageable
    ) {
        QEvent event = QEvent.event;
        QEventDetail detail = QEventDetail.eventDetail;
        QEventTicket eventTicket = QEventTicket.eventTicket;
        QTicket ticket = QTicket.ticket;

        BooleanBuilder builder = new BooleanBuilder()
                .and(detail.eventDetailId.isNotNull());

        if (includeHidden == null || !includeHidden) {
            builder.and(event.hidden.eq(false));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.and(
                    event.titleKr.containsIgnoreCase(keyword)
                            .or(event.titleEng.containsIgnoreCase(keyword))
            );
        }

        if (mainCategoryId != null) {
            builder.and(detail.mainCategory.groupId.eq(mainCategoryId));
        }

        if (subCategoryId != null) {
            builder.and(detail.subCategory.categoryId.eq(subCategoryId));
        }

        if (regionName != null && !regionName.equals("모든지역")) {
            builder.and(
                    detail.regionCode.name.eq(regionName)
            );
        }

        if (fromDate != null) {
            builder.and(detail.endDate.goe(fromDate));
        }
        if (toDate != null) {
            builder.and(detail.startDate.loe(toDate));
        }

        List<EventSummaryDto> content = queryFactory
                .select(new QEventSummaryDto(
                        event.eventId,
                        event.eventCode,
                        event.hidden,
                        event.titleKr,
                        ticket.price.min(),
                        detail.mainCategory.groupName,
                        detail.location.placeName,
                        detail.location.latitude,
                        detail.location.longitude,
                        detail.startDate,
                        detail.endDate,
                        detail.thumbnailUrl,
                        detail.regionCode.name,
                        event.statusCode.code
                ))
                .from(event)
                .join(event.eventDetail, detail)
                .leftJoin(event.eventTickets, eventTicket)
                .leftJoin(eventTicket.ticket, ticket)
                .where(builder)
                .groupBy(event.eventId)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(event.eventId.desc())
                .fetch();

        Long total = queryFactory
                .select(event.countDistinct())
                .from(event)
                .join(event.eventDetail, detail)
                .where(builder)
                .fetchOne();

        if (!content.isEmpty()) {
            enrichEventSummariesWithFiles(content);
        }

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private void enrichEventSummariesWithFiles(List<EventSummaryDto> summaries) {
        List<Long> eventIds = summaries.stream()
                .map(EventSummaryDto::getId)
                .collect(Collectors.toList());

        List<Object[]> fileResults = fileRepository.findByTargetTypeAndTargetIdIn("EVENT", eventIds);

        Map<Long, List<File>> filesByEventId = fileResults.stream()
                .collect(Collectors.groupingBy(
                        obj -> (Long) obj[1],
                        Collectors.mapping(obj -> (File) obj[0], Collectors.toList())
                ));

        summaries.forEach(dto -> {
            List<File> files = filesByEventId.getOrDefault(dto.getId(), new ArrayList<>());
            dto.setFiles(files.stream()
                    .map(file -> EventSummaryDto.FileDto.builder()
                            .id(file.getId())
                            .fileUrl(awsS3Service.getCdnUrl(file.getFileUrl()))
                            .originalFileName(file.getOriginalFileName())
                            .build())
                    .collect(Collectors.toList()));

            // File 테이블에서 썸네일을 가져오는 대신 EventDetail의 thumbnailUrl 사용 (이미 최신 버전임)
            // 기존 QueryDSL에서 이미 detail.thumbnailUrl을 가져오므로 추가 처리 불필요
        });
    }
}