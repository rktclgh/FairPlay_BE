package com.fairing.fairplay.wishlist.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WishlistResponseDto {

    private Long eventId;
    private String eventTitle;
    private String categoryName;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer price;
    private String thumbnailUrl;
}
