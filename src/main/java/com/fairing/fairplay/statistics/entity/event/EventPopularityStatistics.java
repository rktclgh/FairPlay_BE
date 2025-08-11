package com.fairing.fairplay.statistics.entity.event;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "event_popularity_statistics", indexes = @Index(name = "idx_event_pop_stats_event_id", columnList = "event_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventPopularityStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long popularityId;

    @Column(name = "event_id", nullable = false)
    private Long eventId; // 단순 FK ID만 저장
    private String eventTitle;

    @Column(nullable = false)
    private Long viewCount;

    @Column(nullable = false)
    private Long reservationCount;

    @Column(nullable = false)
    private Long wishlistCount;

    private LocalDateTime calculatedAt;

    @PrePersist
    private void initCounts() {
        if (viewCount == null)       viewCount = 0L;
        if (reservationCount == null) reservationCount = 0L;
        if (wishlistCount == null)    wishlistCount = 0L;
    }
}
