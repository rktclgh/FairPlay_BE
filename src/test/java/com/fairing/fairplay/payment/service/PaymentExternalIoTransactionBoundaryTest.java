package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.attendeeform.service.AttendeeFormAttendeeService;
import com.fairing.fairplay.banner.repository.BannerApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.core.email.service.PaymentCompletionEmailService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.payment.repository.PaymentTargetTypeRepository;
import com.fairing.fairplay.payment.repository.PaymentTypeCodeRepository;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.reservation.repository.ReservationStatusCodeRepository;
import com.fairing.fairplay.reservation.service.ReservationService;
import com.fairing.fairplay.support.RecordingTransactionManager;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.ScheduleTicket;
import com.fairing.fairplay.ticket.entity.ScheduleTicketId;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentExternalIoTransactionBoundaryTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentStatusCodeRepository paymentStatusCodeRepository;
    @Mock
    private PaymentTypeCodeRepository paymentTypeCodeRepository;
    @Mock
    private PaymentTargetTypeRepository paymentTargetTypeRepository;
    @Mock
    private ReservationStatusCodeRepository reservationStatusCodeRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventScheduleRepository eventScheduleRepository;
    @Mock
    private ScheduleTicketRepository scheduleTicketRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentCompletionEmailService paymentCompletionEmailService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private BoothApplicationRepository boothApplicationRepository;
    @Mock
    private BoothRepository boothRepository;
    @Mock
    private BannerApplicationRepository bannerApplicationRepository;
    @Mock
    private AttendeeFormAttendeeService attendeeFormAttendeeService;
    @Mock
    private IamportPaymentVerifier iamportPaymentVerifier;
    @Mock
    private ReservationPaymentIntentStore reservationPaymentIntentStore;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RecordingTransactionManager transactionManager;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
        paymentService = new PaymentService(
                paymentRepository,
                refundRepository,
                eventRepository,
                reservationRepository,
                userRepository,
                paymentStatusCodeRepository,
                paymentTypeCodeRepository,
                paymentTargetTypeRepository,
                reservationStatusCodeRepository,
                ticketRepository,
                eventScheduleRepository,
                scheduleTicketRepository,
                notificationService,
                paymentCompletionEmailService,
                reservationService,
                boothApplicationRepository,
                boothRepository,
                bannerApplicationRepository,
                attendeeFormAttendeeService,
                iamportPaymentVerifier,
                reservationPaymentIntentStore,
                redisTemplate,
                transactionManager
        );
    }

    @Test
    void completePaymentSendsCompletionEmailAfterDatabaseTransactionCommits() {
        Payment payment = pendingReservationPayment();
        PaymentRequestDto request = reservationCompletionRequest();
        ReservationPaymentIntent intent = new ReservationPaymentIntent(
                10L, 11L, 22L, 1, BigDecimal.valueOf(100), "RESERVATION", 300L, "merchant-1");

        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByIdForCompletionNotification(1L)).thenReturn(Optional.of(payment));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(status(2, "COMPLETED")));
        when(iamportPaymentVerifier.findPayment("imp-1"))
                .thenReturn(new IamportPaymentInfo("imp-1", "merchant-1", "paid", BigDecimal.valueOf(100)));
        when(reservationPaymentIntentStore.find("merchant-1", 300L)).thenReturn(Optional.of(intent));
        when(eventScheduleRepository.findById(11L)).thenReturn(Optional.of(schedule(11L, payment.getEvent())));
        when(scheduleTicketRepository.findById(new ScheduleTicketId(22L, 11L))).thenReturn(Optional.of(scheduleTicket()));

        doAnswer(invocation -> {
            assertThat(transactionManager.activeCount()).isZero();
            return null;
        }).when(notificationService).createNotification(any());
        doAnswer(invocation -> {
            assertThat(transactionManager.activeCount()).isZero();
            return null;
        }).when(paymentCompletionEmailService).sendPaymentCompletionEmail(payment, 77L);

        paymentService.completePayment(request, user(300L));

        assertThat(transactionManager.activeCount()).isZero();
        var notificationsThenEmail = inOrder(notificationService, paymentCompletionEmailService);
        notificationsThenEmail.verify(notificationService).createNotification(any());
        notificationsThenEmail.verify(paymentCompletionEmailService).sendPaymentCompletionEmail(payment, 77L);
    }

    private PaymentRequestDto reservationCompletionRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setMerchantUid("merchant-1");
        request.setImpUid("imp-1");
        request.setPaymentTargetType("RESERVATION");
        request.setScheduleId(11L);
        request.setTicketId(22L);
        request.setQuantity(1);
        request.setPrice(BigDecimal.valueOf(100));
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    private Payment pendingReservationPayment() {
        Event event = event(10L);
        return Payment.builder()
                .paymentId(1L)
                .event(event)
                .user(userEntity(300L))
                .paymentTargetType(PaymentTargetType.builder()
                        .paymentTargetCode("RESERVATION")
                        .paymentTargetName("예약")
                        .build())
                .targetId(77L)
                .merchantUid("merchant-1")
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(100))
                .refundedAmount(BigDecimal.ZERO)
                .paymentTypeCode(PaymentTypeCode.builder().paymentTypeCodeId(1).code("CARD").name("카드").build())
                .paymentStatusCode(status(1, "PENDING"))
                .build();
    }

    private Event event(Long eventId) {
        Event event = new Event();
        event.setEventId(eventId);
        event.setTitleKr("행사");
        EventDetail detail = new EventDetail();
        detail.setStartDate(LocalDate.now());
        detail.setEndDate(LocalDate.now().plusDays(1));
        event.setEventDetail(detail);
        return event;
    }

    private EventSchedule schedule(Long scheduleId, Event event) {
        return EventSchedule.builder()
                .scheduleId(scheduleId)
                .event(event)
                .build();
    }

    private ScheduleTicket scheduleTicket() {
        Ticket ticket = Ticket.builder()
                .ticketId(22L)
                .price(100)
                .maxPurchase(5)
                .build();
        return ScheduleTicket.builder()
                .id(new ScheduleTicketId(22L, 11L))
                .ticket(ticket)
                .build();
    }

    private PaymentStatusCode status(Integer id, String code) {
        return PaymentStatusCode.builder()
                .paymentStatusCodeId(id)
                .code(code)
                .name(code)
                .build();
    }

    private Users userEntity(Long userId) {
        Users user = new Users(userId);
        user.setName("사용자");
        user.setEmail("user@example.com");
        return user;
    }

    private CustomUserDetails user(Long userId) {
        CustomUserDetails userDetails = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        return userDetails;
    }
}
