package com.fairing.fairplay.attendee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

/*참석자*/
/*추후 reservation 폴더로 이동될 가능성 있음*/
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Attendee {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name="attendee_id", nullable = false, updatable = false)
  private Long id;

  /*
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id")
  private Reservation reservation;
  */
  // 임시 설정
  @Column(name= "reservation_id", nullable = false)
  private Long reservationId;

  @Column(name = "name", nullable = true)
  private String name;
  @Column(name = "phone", nullable = true)
  private String phone;
  @Column(name = "email", nullable = true, unique = true)
  private String email;
  @Column(name="birth", nullable = true)
  private LocalDate birth;
  @Column(name="checked_in",nullable=false)
  @ColumnDefault("false")
  private Boolean checkedIn = false;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attendee_type_code_id")
  private AttendeeTypeCode attendeeTypeCode;
  @CreationTimestamp
  @Column(name="created_at", updatable = false)
  private LocalDateTime createdAt;
}
