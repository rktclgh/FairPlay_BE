package com.fairing.fairplay.attendee.entity;

import com.fairing.fairplay.reservation.entity.Reservation;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.ColumnDefault;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Attendee {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attendee_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id")
  private Reservation reservation;

  @Column(name = "name", nullable = true)
  private String name;

  @Column(name = "phone", nullable = true)
  private String phone;

  @Column(name = "email", nullable = true, unique = true)
  private String email;

  @Column(name = "birth", nullable = true)
  private LocalDate birth;

  @Column(name = "checked_in", nullable = false)
  @ColumnDefault("false")
  @Builder.Default
  private Boolean checkedIn = false;

  @Column(name = "agree_to_terms", nullable = false)
  @ColumnDefault("false")
  @Builder.Default
  private Boolean agreeToTerms = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attendee_type_code_id")
  private AttendeeTypeCode attendeeTypeCode;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
