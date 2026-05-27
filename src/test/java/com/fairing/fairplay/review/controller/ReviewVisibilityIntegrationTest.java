package com.fairing.fairplay.review.controller;

import com.fairing.fairplay.core.service.SessionService;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventStatusCode;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.entity.TypesEnum;
import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.config.import=",
    "spring.datasource.url=jdbc:h2:mem:fairplay-review-visible-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=HOUR",
    "spring.jpa.open-in-view=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.main.lazy-initialization=true"
})
@AutoConfigureMockMvc
class ReviewVisibilityIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @MockBean
  private SessionService sessionService;

  @Test
  void publicEventReviewListExcludesInvisibleReviews() throws Exception {
    Long eventId = transactionTemplate.execute(status -> persistReviewFixture());

    mockMvc.perform(get("/api/reviews/{eventId}", eventId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(eventId))
        .andExpect(jsonPath("$.reviews.totalElements").value(1))
        .andExpect(jsonPath("$.reviews.content", hasSize(1)))
        .andExpect(jsonPath("$.reviews.content[0].review.comment").value("visible review"))
        .andExpect(jsonPath("$.reviews.content[0].review.visible").value(true));
  }

  private Long persistReviewFixture() {
    EventStatusCode eventStatusCode = new EventStatusCode();
    eventStatusCode.setCode("UPCOMING");
    eventStatusCode.setName("진행 예정");
    entityManager.persist(eventStatusCode);

    ReservationStatusCode reservationStatusCode = new ReservationStatusCode(1);
    reservationStatusCode.setCode("CONFIRMED");
    reservationStatusCode.setName("예약 완료");
    entityManager.persist(reservationStatusCode);

    Users user = Users.builder()
        .email("reviewer@example.com")
        .password("encoded-password")
        .nickname("reviewer")
        .name("Review Author")
        .phone("010-0000-0000")
        .build();
    entityManager.persist(user);

    Event event = new Event();
    event.setStatusCode(eventStatusCode);
    event.setEventCode("REVIEW-VISIBLE-REGRESSION");
    event.setTitleKr("리뷰 공개 회귀 행사");
    event.setTitleEng("Review Visibility Regression Event");
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

    Reservation visibleReservation = new Reservation(event, schedule, ticket, user, 1, 10000);
    visibleReservation.setReservationStatusCode(reservationStatusCode);
    visibleReservation.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
    entityManager.persist(visibleReservation);

    Reservation hiddenReservation = new Reservation(event, schedule, ticket, user, 1, 10000);
    hiddenReservation.setReservationStatusCode(reservationStatusCode);
    hiddenReservation.setCreatedAt(LocalDateTime.of(2026, 5, 1, 11, 0));
    entityManager.persist(hiddenReservation);

    Review visibleReview = Review.builder()
        .reservation(visibleReservation)
        .user(user)
        .comment("visible review")
        .star(5)
        .visible(true)
        .createdAt(LocalDateTime.of(2026, 5, 2, 10, 0))
        .build();
    entityManager.persist(visibleReview);

    Review hiddenReview = Review.builder()
        .reservation(hiddenReservation)
        .user(user)
        .comment("hidden review")
        .star(1)
        .visible(false)
        .createdAt(LocalDateTime.of(2026, 5, 3, 10, 0))
        .build();
    entityManager.persist(hiddenReview);

    entityManager.flush();
    entityManager.clear();
    return event.getEventId();
  }
}
