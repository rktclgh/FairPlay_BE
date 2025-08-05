package com.fairing.fairplay.event.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.fairing.fairplay.event.dto.QEventSummaryDto is a Querydsl Projection type for EventSummaryDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QEventSummaryDto extends ConstructorExpression<EventSummaryDto> {

    private static final long serialVersionUID = -555723465L;

    public QEventSummaryDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> eventCode, com.querydsl.core.types.Expression<Boolean> hidden, com.querydsl.core.types.Expression<String> title, com.querydsl.core.types.Expression<Integer> minPrice, com.querydsl.core.types.Expression<String> mainCategory, com.querydsl.core.types.Expression<String> location, com.querydsl.core.types.Expression<java.time.LocalDate> startDate, com.querydsl.core.types.Expression<java.time.LocalDate> endDate, com.querydsl.core.types.Expression<String> thumbnailUrl, com.querydsl.core.types.Expression<String> region) {
        super(EventSummaryDto.class, new Class<?>[]{long.class, String.class, boolean.class, String.class, int.class, String.class, String.class, java.time.LocalDate.class, java.time.LocalDate.class, String.class, String.class}, id, eventCode, hidden, title, minPrice, mainCategory, location, startDate, endDate, thumbnailUrl, region);
    }

}

