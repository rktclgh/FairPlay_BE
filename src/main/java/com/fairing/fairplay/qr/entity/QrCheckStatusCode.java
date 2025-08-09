package com.fairing.fairplay.qr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "qr_check_status_code")
public class QrCheckStatusCode {
  public static final String ENTRY = "ENTRY";
  public static final String EXIT = "EXIT";
  public static final String REENTRY = "REENTRY";
  public static final String DUPLICATE = "DUPLICATE";
  public static final String INVALID = "INVALID";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "qr_check_status_code_id")
  private Integer id;

  @Column(name = "code", nullable = false, unique = true, length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;
}
