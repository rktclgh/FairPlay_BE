package com.fairing.fairplay.shareticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "share_ticket")
public class ShareTicket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "share_ticket_id")
  private Long id;

  // Reservation 엔티티 생성 시 수정
  @Column(name = "reservation_id")
  private Long reservationId;

  @Column(name = "link_token", nullable = false, unique = true)
  private String linkToken;

  @Column(name = "submitted_count", nullable = false)
  @ColumnDefault("0")
  private Integer submittedCount = 0;

  @Column(name = "total_allowed", nullable = false)
  private Integer totalAllowed;

  @Column(name = "expired", nullable = false)
  @ColumnDefault("false")
  private Boolean expired = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "expired_at")
  private LocalDateTime expiredAt;

  public boolean isFull() {
    return submittedCount >= totalAllowed;
  }

  public void increaseSubmittedCount() {
    this.submittedCount++;
  }
}
