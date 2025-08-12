package com.fairing.fairplay.admin.entity;

import java.math.BigDecimal;
import java.math.BigInteger;

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

    private String functionName;
    private BigDecimal level;

}
