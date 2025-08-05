package com.fairing.fairplay.admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAdminRoleCode is a Querydsl query type for AdminRoleCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminRoleCode extends EntityPathBase<AdminRoleCode> {

    private static final long serialVersionUID = 554917119L;

    public static final QAdminRoleCode adminRoleCode = new QAdminRoleCode("adminRoleCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QAdminRoleCode(String variable) {
        super(AdminRoleCode.class, forVariable(variable));
    }

    public QAdminRoleCode(Path<? extends AdminRoleCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAdminRoleCode(PathMetadata metadata) {
        super(AdminRoleCode.class, metadata);
    }

}

