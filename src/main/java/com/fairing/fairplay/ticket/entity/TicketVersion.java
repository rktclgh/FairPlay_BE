package com.fairing.fairplay.ticket.entity;

import com.fairing.fairplay.core.util.JsonUtil;
import com.fairing.fairplay.ticket.dto.TicketSnapshotDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_version", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ticket_id", "version_number"})
})
public class TicketVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_version_id")
    private Long ticketVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "JSON")
    private String snapshot;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TicketSnapshotDto getSnapshotAsDto() {
        return JsonUtil.fromJson(this.snapshot, TicketSnapshotDto.class);
    }

    public void setSnapshotFromDto(TicketSnapshotDto dto) {
        this.snapshot = JsonUtil.toJson(dto);
    }
}
