package com.fairing.fairplay.wishlist.repository;


import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 유저가 특정 이벤트를 찜했는지 조회 (삭제되지 않은 것만)
    Optional<Wishlist> findByUser_UserIdAndEvent_EventIdAndDeletedFalse(Long userId, Long eventId);

    // 유저의 찜 목록 전체 조회 (삭제되지 않은 것만)
    List<Wishlist> findAllByUser_UserIdAndDeletedFalse(Long userId);

    // 유저 + 이벤트로 전체 엔티티 조회 (삭제 여부 상관없이)
    Optional<Wishlist> findByUser_UserIdAndEvent_EventId(Long userId, Long eventId);

    @Query("SELECT w FROM Wishlist w " +
            "JOIN FETCH w.event e " +
            "LEFT JOIN FETCH e.eventDetail d " +
            "LEFT JOIN FETCH d.mainCategory " +
            "LEFT JOIN FETCH d.location " +
            "LEFT JOIN FETCH e.eventTickets et " +
            "LEFT JOIN FETCH et.ticket t " +
            "WHERE w.user.userId = :userId AND w.deleted = false")
    List<Wishlist> findWithAllDetailsByUserId(@Param("userId") Long userId);

    void deleteAllByEvent(Event event);
}