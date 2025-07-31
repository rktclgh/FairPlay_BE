package com.fairing.fairplay.qr.entity;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.ticket.entity.EventTicket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/*QR티켓*/
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "qr_ticket")
public class QrTicket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "qr_ticket_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attendee_id", nullable = false)
  private Attendee attendee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "ticket_id", referencedColumnName = "ticket_id"),
      @JoinColumn(name = "event_id", referencedColumnName = "event_id")
  })
  private EventTicket eventTicket;

  @Column(name = "expired_at", nullable = false)
  private LocalDateTime expiredAt;

  @Column(name = "issued_at", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
  @CreationTimestamp
  private LocalDateTime issuedAt;

  @Column(nullable = false)
  private Boolean active = true;

  @Column(name = "reentry_allowed", nullable = false)
  private Boolean reentryAllowed = false;

  @Column(name = "qr_code", nullable = false, unique = true, length = 255)
  private String qrCode;

  @Column(name = "manual_code", nullable = false, unique = true, length = 15)
  private String manualCode;
}
