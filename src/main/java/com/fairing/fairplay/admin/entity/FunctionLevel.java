package com.fairing.fairplay.admin.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "function_level")
public class FunctionLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long functionId;

    @Column(name = "function_name")
    private String functionName;

    @Column(name = "function_name_kr")
    private String functionNameKr;
    @Column(name = "level", precision = 65, scale = 0)
    private BigDecimal level;

}
