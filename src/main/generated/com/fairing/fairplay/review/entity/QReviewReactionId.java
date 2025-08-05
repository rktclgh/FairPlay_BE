package com.fairing.fairplay.review.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReviewReactionId is a Querydsl query type for ReviewReactionId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QReviewReactionId extends BeanPath<ReviewReactionId> {

    private static final long serialVersionUID = 1817192202L;

    public static final QReviewReactionId reviewReactionId = new QReviewReactionId("reviewReactionId");

    public final NumberPath<Long> reviewId = createNumber("reviewId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QReviewReactionId(String variable) {
        super(ReviewReactionId.class, forVariable(variable));
    }

    public QReviewReactionId(Path<? extends ReviewReactionId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewReactionId(PathMetadata metadata) {
        super(ReviewReactionId.class, metadata);
    }

}

