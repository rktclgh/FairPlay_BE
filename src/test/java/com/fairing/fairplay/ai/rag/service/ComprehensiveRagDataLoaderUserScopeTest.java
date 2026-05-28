package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.booth.repository.BoothExperienceRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.event.repository.EventDetailRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.repository.MainCategoryRepository;
import com.fairing.fairplay.event.repository.SubCategoryRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.review.repository.ReviewRepository;
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

import static org.mockito.ArgumentMatchers.any;
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
