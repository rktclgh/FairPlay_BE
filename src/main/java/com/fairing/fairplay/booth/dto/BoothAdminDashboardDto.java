package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.Booth;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class BoothAdminDashboardDto {
    private Long boothId;
    private String boothTitle;
    private String boothDescription;
    private String boothBannerUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 부스 타입 정보
    private String boothTypeName;
    private String boothTypeSize;
    private Integer price;

    // 이벤트 정보
    private String eventTitle;
    private Long eventId;

    // 관리자 정보
    private String managerName;
    private String contactEmail;
    private String contactNumber;

    // 상태 정보
    private String statusCode;
    private String statusName;
    private String paymentStatusCode;
    private String paymentStatus;

    // 상세 정보
    private String location;
    private List<BoothExternalLinkDto> boothExternalLinks;


    public static BoothAdminDashboardDto from(Booth booth) {
        return BoothAdminDashboardDto.builder()
                .boothId(booth.getId())
                .boothTitle(booth.getBoothTitle())
                .boothDescription(booth.getBoothDescription())
                .boothBannerUrl(booth.getBoothBannerUrl())
                .startDate(booth.getStartDate())
                .endDate(booth.getEndDate())
                .createdAt(booth.getCreatedAt())
                .boothTypeName(booth.getBoothType() != null ? booth.getBoothType().getName() : null)
                .boothTypeSize(booth.getBoothType() != null ? booth.getBoothType().getSize() : null)
                .price(booth.getBoothType() != null ? booth.getBoothType().getPrice() : null)
                .eventTitle(booth.getEvent() != null ? booth.getEvent().getTitleKr() : null)
                .eventId(booth.getEvent() != null ? booth.getEvent().getEventId() : null)
                .managerName(booth.getBoothAdmin() != null ? booth.getBoothAdmin().getManagerName() : null)
                .contactEmail(booth.getBoothAdmin() != null ? booth.getBoothAdmin().getEmail() : null)
                .contactNumber(booth.getBoothAdmin() != null ? booth.getBoothAdmin().getContactNumber() : null)
                .statusCode("APPROVED")
                .statusName("승인")
                .paymentStatusCode("PAID")
                .paymentStatus("결제완료")
                .location(booth.getLocation())
                .boothExternalLinks(booth.getBoothExternalLinks() != null ? booth.getBoothExternalLinks().stream()
                        .map(BoothExternalLinkDto::from)
                        .collect(Collectors.toList()) : null)
                .build();
    }
}