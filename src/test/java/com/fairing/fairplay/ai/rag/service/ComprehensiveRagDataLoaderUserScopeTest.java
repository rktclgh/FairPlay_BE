package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.booth.repository.BoothExperienceRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.event.repository.EventDetailRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.MainCategoryRepository;
import com.fairing.fairplay.event.repository.SubCategoryRepository;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.review.repository.ReviewRepository;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprehensiveRagDataLoaderUserScopeTest {

    @Mock
    EventRepository eventRepository;
    @Mock
    EventDetailRepository eventDetailRepository;
    @Mock
    MainCategoryRepository mainCategoryRepository;
    @Mock
    SubCategoryRepository subCategoryRepository;
    @Mock
    BoothRepository boothRepository;
    @Mock
    BoothExperienceRepository boothExperienceRepository;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    TicketRepository ticketRepository;
    @Mock
    EventScheduleRepository eventScheduleRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ReservationRepository reservationRepository;
    @Mock
    AttendeeRepository attendeeRepository;
    @Mock
    DocumentIngestService documentIngestService;

    ComprehensiveRagDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ComprehensiveRagDataLoader(
            eventRepository,
            eventDetailRepository,
            mainCategoryRepository,
            subCategoryRepository,
            boothRepository,
            boothExperienceRepository,
            reviewRepository,
            ticketRepository,
            eventScheduleRepository,
            userRepository,
            reservationRepository,
            attendeeRepository,
            documentIngestService,
            new NoOpTransactionManager()
        );
    }

    @Test
    void singleUserDataLoadDeletesSoftDeletedUserDocumentInsteadOfReindexingIt() {
        Users deletedUser = new Users(10L);
        deletedUser.setDeletedAt(LocalDateTime.now());
        when(userRepository.findById(10L)).thenReturn(Optional.of(deletedUser));

        loader.loadSingleUserData(10L);

        verify(documentIngestService).deleteDocument("user_10");
        verify(documentIngestService, never()).ingestDocument(any());
    }

    @Test
    void singleReservationLoadIndexesPrivateReservationDocumentWithScopeMetadata() {
        Reservation reservation = reservation(141L, 1081L, 52L);
        when(reservationRepository.findByIdForResponse(141L)).thenReturn(Optional.of(reservation));

        loader.loadSingleReservation(141L);

        ArgumentCaptor<com.fairing.fairplay.ai.rag.domain.Document> documentCaptor =
            forClass(com.fairing.fairplay.ai.rag.domain.Document.class);
        verify(documentIngestService).ingestDocument(documentCaptor.capture());

        com.fairing.fairplay.ai.rag.domain.Document document = documentCaptor.getValue();
        assertThat(document.getDocId()).isEqualTo("reservation_141");
        assertThat(document.getDocType()).isEqualTo("USER_RESERVATION");
        assertThat(document.getVisibility()).isEqualTo("USER_PRIVATE");
        assertThat(document.getOwnerUserId()).isEqualTo(1081L);
        assertThat(document.getEventId()).isEqualTo(52L);
        assertThat(document.getReservationId()).isEqualTo(141L);
        assertThat(document.getContent())
            .contains("=== 개인 예약 내역 ===")
            .contains("2025 트렌드페어")
            .contains("멋쟁이 티켓")
            .contains("예약 상태: 완료");
    }

    private Reservation reservation(Long reservationId, Long userId, Long eventId) {
        Event event = new Event();
        event.setEventId(eventId);
        event.setTitleKr("2025 트렌드페어");
        event.setTitleEng("2025 TREND FAIR");

        Ticket ticket = new Ticket();
        ticket.setTicketId(99L);
        ticket.setName("멋쟁이 티켓");
        ticket.setPrice(100000);

        Users user = new Users(userId);
        user.setName("송치호");
        user.setEmail("user@example.com");

        ReservationStatusCode statusCode = new ReservationStatusCode(1);
        statusCode.setCode("CONFIRMED");
        statusCode.setName("완료");

        Reservation reservation = new Reservation(event, null, ticket, user, 2, 200000);
        reservation.setReservationId(reservationId);
        reservation.setReservationStatusCode(statusCode);
        reservation.setCreatedAt(LocalDateTime.of(2025, 8, 21, 13, 0));
        return reservation;
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
