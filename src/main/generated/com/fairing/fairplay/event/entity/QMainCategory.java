package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMainCategory is a Querydsl query type for MainCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMainCategory extends EntityPathBase<MainCategory> {

    private static final long serialVersionUID = 122463989L;

    public static final QMainCategory mainCategory = new QMainCategory("mainCategory");

    public final NumberPath<Integer> groupId = createNumber("groupId", Integer.class);

    public final StringPath groupName = createString("groupName");

    public QMainCategory(String variable) {
        super(MainCategory.class, forVariable(variable));
    }

    public QMainCategory(Path<? extends MainCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMainCategory(PathMetadata metadata) {
        super(MainCategory.class, metadata);
    }

}

