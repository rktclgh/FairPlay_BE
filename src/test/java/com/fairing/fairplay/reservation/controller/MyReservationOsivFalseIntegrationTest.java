package com.fairing.fairplay.reservation.controller;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventStatusCode;
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

import java.time.LocalDate;
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
                .andExpect(jsonPath("$[0].eventName").value("OSIV 회귀 행사"))
                .andExpect(jsonPath("$[0].ticketName").value("일반권"))
                .andExpect(jsonPath("$[0].userEmail").value("user@example.com"))
                .andExpect(jsonPath("$[0].reservationStatus").value("예약 완료"));
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
        entityManager.persist(reservation);
        entityManager.flush();
        entityManager.clear();

        return user.getUserId();
    }
}
