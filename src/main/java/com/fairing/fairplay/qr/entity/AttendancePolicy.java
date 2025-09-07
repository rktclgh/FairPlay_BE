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
@Table(name = "attendance_policy")
public class AttendancePolicy {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attendance_policy_id")
  private Integer id;

  @Column(name = "reentry_allowed", nullable = false)
  @Builder.Default
  private Boolean reentryAllowed = true;

  @Column(name = "check_in_allowed", nullable = false)
  @Builder.Default
  private Boolean checkInAllowed = false;

  @Column(name = "check_out_allowed", nullable = false)
  @Builder.Default
  private Boolean checkOutAllowed = false;
}
