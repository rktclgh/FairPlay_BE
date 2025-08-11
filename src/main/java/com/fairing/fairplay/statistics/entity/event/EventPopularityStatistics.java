package com.fairing.fairplay.statistics.entity.event;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "event_popularity_statistics")
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
    private Long viewCount;
    private Long reservationCount;
    private Long wishlistCount;

    private LocalDateTime calculatedAt;
}
