package com.fairing.fairplay.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserRoleCode is a Querydsl query type for UserRoleCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserRoleCode extends EntityPathBase<UserRoleCode> {

    private static final long serialVersionUID = -1302040817L;

    public static final QUserRoleCode userRoleCode = new QUserRoleCode("userRoleCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QUserRoleCode(String variable) {
        super(UserRoleCode.class, forVariable(variable));
    }

    public QUserRoleCode(Path<? extends UserRoleCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserRoleCode(PathMetadata metadata) {
        super(UserRoleCode.class, metadata);
    }

}

