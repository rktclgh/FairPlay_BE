package com.fairing.fairplay.review.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewReaction is a Querydsl query type for ReviewReaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewReaction extends EntityPathBase<ReviewReaction> {

    private static final long serialVersionUID = -1039448689L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewReaction reviewReaction = new QReviewReaction("reviewReaction");

    public final QReviewReactionId id;

    public final QReview review;

    public final com.fairing.fairplay.user.entity.QUsers user;

    public QReviewReaction(String variable) {
        this(ReviewReaction.class, forVariable(variable), INITS);
    }

    public QReviewReaction(Path<? extends ReviewReaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewReaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewReaction(PathMetadata metadata, PathInits inits) {
        this(ReviewReaction.class, metadata, inits);
    }

    public QReviewReaction(Class<? extends ReviewReaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QReviewReactionId(forProperty("id")) : null;
        this.review = inits.isInitialized("review") ? new QReview(forProperty("review"), inits.get("review")) : null;
        this.user = inits.isInitialized("user") ? new com.fairing.fairplay.user.entity.QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

