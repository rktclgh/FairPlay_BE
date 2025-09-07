package com.fairing.fairplay.attendeeform.entity;

import com.fairing.fairplay.reservation.entity.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id", nullable = false)
  private Reservation reservation;

  @Column(name = "link_token", nullable = false, unique = true)
  private String linkToken;

  @Column(name = "submitted_count", nullable = false)
  @ColumnDefault("0")
  @Builder.Default
  private Integer submittedCount = 0;

  @Column(name = "total_allowed", nullable = false)
  private Integer totalAllowed;

  @Column(name = "expired", nullable = false)
  @ColumnDefault("false")
  @Builder.Default
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
