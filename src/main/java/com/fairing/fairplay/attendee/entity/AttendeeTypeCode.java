package com.fairing.fairplay.attendee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "attendee_type_code")
public class AttendeeTypeCode {

  public static final String PRIMARY = "PRIMARY";
  public static final String GUEST = "GUEST";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attendee_type_code_id")
  private Integer id;

  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "name", nullable = false, unique = true)
  private String name;
}
