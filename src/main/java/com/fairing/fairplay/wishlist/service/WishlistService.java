package com.fairing.fairplay.wishlist.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.ticket.entity.EventTicket;

import com.fairing.fairplay.wishlist.dto.WishlistResponseDto;
import com.fairing.fairplay.wishlist.entity.Wishlist;
import com.fairing.fairplay.wishlist.repository.WishlistRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    // 찜 등록
    @Transactional
    public void addWishlist(Long userId, Long eventId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다."));

        wishlistRepository.findByUser_UserIdAndEvent_EventId(userId, eventId).ifPresentOrElse(
                wishlist -> {
                    if (wishlist.getDeleted()) {
                        wishlist.setDeleted(false);
                    } else {
                        throw new IllegalStateException("이미 찜한 이벤트입니다.");
                    }
                },
                () -> {
                    Wishlist wishlist = Wishlist.builder()
                            .user(user)
                            .event(event)
                            .deleted(false)
                            .build();
                    wishlistRepository.save(wishlist);
                }
        );
    }

    // 찜 취소
    @Transactional
    public void cancelWishlist(Long userId, Long eventId) {
        Wishlist wishlist = wishlistRepository.findByUser_UserIdAndEvent_EventIdAndDeletedFalse(userId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("찜한 이벤트가 없습니다."));
        wishlist.setDeleted(true);
    }

    // 찜 목록 조회
    @Transactional(readOnly = true)
    public List<WishlistResponseDto> getMyWishlist(Long userId) {
        List<Wishlist> wishlistList = wishlistRepository.findWithAllDetailsByUserId(userId);

        return wishlistList.stream()
                .map(wishlist -> {
                    Event event = wishlist.getEvent();

                    // 필수 연관 엔티티 null 체크
                    if (event == null ||
                            event.getEventDetail() == null ||
                            event.getEventDetail().getMainCategory() == null ||
                            event.getEventDetail().getLocation() == null) {

                        System.out.println("불완전한 이벤트: eventId = " + (event != null ? event.getEventId() : "null"));
                        return null;
                    }

                    return WishlistResponseDto.builder()
                            .eventId(event.getEventId())
                            .eventTitle(event.getTitleKr())
                            .categoryName(event.getEventDetail().getMainCategory().getGroupName())
                            .location(event.getEventDetail().getLocation().getAddress())
                            .startDate(event.getEventDetail().getStartDate())
                            .endDate(event.getEventDetail().getEndDate())
                            .price(
                                    event.getEventTickets().stream()
                                            .map(EventTicket::getTicket)
                                            .filter(t -> t != null && !t.getDeleted())
                                            .mapToInt(t -> t.getPrice())
                                            .min()
                                            .orElse(0)
                            )
                            .thumbnailUrl(event.getEventDetail().getThumbnailUrl())
                            .build();
                })
                .filter(dto -> dto != null)  // null 걸러주기
                .collect(Collectors.toList());
    }


}
