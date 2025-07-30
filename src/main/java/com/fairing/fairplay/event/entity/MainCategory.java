package com.fairing.fairplay.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_group")
public class MainCategory {

    @Id
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "group_name", length = 50, nullable = false)
    private String groupName;
}