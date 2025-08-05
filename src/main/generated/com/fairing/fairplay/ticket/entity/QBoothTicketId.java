package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoothTicketId is a Querydsl query type for BoothTicketId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QBoothTicketId extends BeanPath<BoothTicketId> {

    private static final long serialVersionUID = -2143724997L;

    public static final QBoothTicketId boothTicketId = new QBoothTicketId("boothTicketId");

    public final NumberPath<Long> boothId = createNumber("boothId", Long.class);

    public final NumberPath<Long> ticketId = createNumber("ticketId", Long.class);

    public QBoothTicketId(String variable) {
        super(BoothTicketId.class, forVariable(variable));
    }

    public QBoothTicketId(Path<? extends BoothTicketId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoothTicketId(PathMetadata metadata) {
        super(BoothTicketId.class, metadata);
    }

}

