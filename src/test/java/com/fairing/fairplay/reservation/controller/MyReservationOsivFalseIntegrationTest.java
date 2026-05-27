package com.fairing.fairplay.reservation.controller;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventStatusCode;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.entity.TypesEnum;
import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:fairplay-osiv-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=HOUR",
        "spring.jpa.open-in-view=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.main.lazy-initialization=true"
})
@AutoConfigureMockMvc
class MyReservationOsivFalseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private SessionService sessionService;

    @Test
    void returnsMyReservationsWithoutLazyInitializationExceptionWhenOpenInViewIsFalse() throws Exception {
        Long userId = transactionTemplate.execute(status -> persistReservationFixture());

        when(sessionService.getSessionData("session-user")).thenReturn(Map.of(
                "userId", userId,
                "email", "user@example.com",
                "role", "COMMON",
                "roleId", 4
        ));

        mockMvc.perform(get("/api/me/reservations")
                        .cookie(new Cookie("FAIRPLAY_SESSION", "session-user"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventName").value("OSIV 회귀 행사"))
                .andExpect(jsonPath("$.content[0].ticketName").value("일반권"))
                .andExpect(jsonPath("$.content[0].userEmail").value("user@example.com"))
                .andExpect(jsonPath("$.content[0].reservationStatus").value("예약 완료"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/me/reservations")
                        .param("activeOnly", "true")
                        .cookie(new Cookie("FAIRPLAY_SESSION", "session-user"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reservationStatus").value("예약 완료"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private Long persistReservationFixture() {
        EventStatusCode eventStatusCode = new EventStatusCode();
        eventStatusCode.setCode("UPCOMING");
        eventStatusCode.setName("진행 예정");
        entityManager.persist(eventStatusCode);

        ReservationStatusCode reservationStatusCode = new ReservationStatusCode(1);
        reservationStatusCode.setCode("CONFIRMED");
        reservationStatusCode.setName("예약 완료");
        entityManager.persist(reservationStatusCode);

        ReservationStatusCode cancelledStatusCode = new ReservationStatusCode(2);
        cancelledStatusCode.setCode("CANCELLED");
        cancelledStatusCode.setName("예약 취소");
        entityManager.persist(cancelledStatusCode);

        PaymentStatusCode refundedPaymentStatusCode = PaymentStatusCode.builder()
                .code("REFUNDED")
                .name("환불 완료")
                .build();
        entityManager.persist(refundedPaymentStatusCode);

        PaymentTargetType reservationPaymentTargetType = PaymentTargetType.builder()
                .paymentTargetCode("RESERVATION")
                .paymentTargetName("예약")
                .build();
        entityManager.persist(reservationPaymentTargetType);

        PaymentTypeCode cardPaymentTypeCode = PaymentTypeCode.builder()
                .code("CARD")
                .name("카드")
                .build();
        entityManager.persist(cardPaymentTypeCode);

        Users user = Users.builder()
                .email("user@example.com")
                .password("encoded-password")
                .nickname("user")
                .name("테스트 사용자")
                .phone("010-0000-0000")
                .build();
        entityManager.persist(user);

        Event event = new Event();
        event.setStatusCode(eventStatusCode);
        event.setEventCode("OSIV-REGRESSION");
        event.setTitleKr("OSIV 회귀 행사");
        event.setTitleEng("OSIV Regression Event");
        event.setHidden(false);
        entityManager.persist(event);

        EventSchedule schedule = EventSchedule.builder()
                .event(event)
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .weekday(1)
                .build();
        entityManager.persist(schedule);

        Ticket ticket = Ticket.builder()
                .name("일반권")
                .description("일반 입장권")
                .price(10000)
                .stock(100)
                .visible(true)
                .deleted(false)
                .types(TypesEnum.EVENT)
                .build();
        entityManager.persist(ticket);

        Reservation reservation = new Reservation(event, schedule, ticket, user, 1, 10000);
        reservation.setReservationStatusCode(reservationStatusCode);
        reservation.setCreatedAt(LocalDateTime.of(2026, 5, 2, 12, 0));
        entityManager.persist(reservation);

        Reservation refundedReservation = new Reservation(event, schedule, ticket, user, 1, 10000);
        refundedReservation.setReservationStatusCode(reservationStatusCode);
        refundedReservation.setCreatedAt(LocalDateTime.of(2026, 5, 1, 13, 0));
        entityManager.persist(refundedReservation);
        entityManager.flush();

        Payment refundedPayment = Payment.builder()
                .event(event)
                .user(user)
                .paymentTargetType(reservationPaymentTargetType)
                .targetId(refundedReservation.getReservationId())
                .merchantUid("merchant-refunded")
                .impUid("imp-refunded")
                .quantity(1)
                .price(BigDecimal.valueOf(10000))
                .refundedAmount(BigDecimal.valueOf(10000))
                .amount(BigDecimal.valueOf(10000))
                .pgProvider("test")
                .paymentTypeCode(cardPaymentTypeCode)
                .paymentStatusCode(refundedPaymentStatusCode)
                .requestedAt(LocalDateTime.of(2026, 5, 1, 13, 0))
                .paidAt(LocalDateTime.of(2026, 5, 1, 13, 1))
                .refundedAt(LocalDateTime.of(2026, 5, 1, 13, 2))
                .build();
        entityManager.persist(refundedPayment);

        Reservation cancelledReservation = new Reservation(event, schedule, ticket, user, 1, 10000);
        cancelledReservation.setReservationStatusCode(cancelledStatusCode);
        cancelledReservation.setCanceled(true);
        cancelledReservation.setCreatedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        entityManager.persist(cancelledReservation);
        entityManager.flush();
        entityManager.clear();

        return user.getUserId();
    }
}
