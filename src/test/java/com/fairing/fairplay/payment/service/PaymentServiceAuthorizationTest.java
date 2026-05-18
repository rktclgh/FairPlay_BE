package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.attendeeform.dto.AttendeeFormSaveResponseDto;
import com.fairing.fairplay.attendeeform.service.AttendeeFormAttendeeService;
import com.fairing.fairplay.banner.entity.BannerApplication;
import com.fairing.fairplay.banner.repository.BannerApplicationRepository;
import com.fairing.fairplay.booth.entity.BoothApplication;
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
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.reservation.repository.ReservationStatusCodeRepository;
import com.fairing.fairplay.reservation.service.ReservationService;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.ScheduleTicket;
import com.fairing.fairplay.ticket.entity.ScheduleTicketId;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.UserRoleCode;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PaymentServiceAuthorizationTest {

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
    @Mock
    private PlatformTransactionManager transactionManager;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(paymentRepository.findByMerchantUid(anyString()))
                .thenAnswer(invocation -> paymentRepository.findByMerchantUidForUpdate(invocation.getArgument(0)));
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
    void commonOwnerCanBindNullTargetAndRepeatSameTarget() {
        CustomUserDetails owner = user(300L, "COMMON");
        Payment nullTargetPayment = payment("merchant-1", 300L, null, null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(nullTargetPayment));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation(10L, 300L)));

        paymentService.updatePaymentTargetId("merchant-1", 10L, owner);

        assertThat(nullTargetPayment.getTargetId()).isEqualTo(10L);
        verify(paymentRepository).save(nullTargetPayment);

        Payment sameTargetPayment = payment("merchant-2", 300L, 10L, null);
        when(paymentRepository.findByMerchantUid("merchant-2")).thenReturn(Optional.of(sameTargetPayment));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation(10L, 300L)));

        assertThatCode(() -> paymentService.updatePaymentTargetId("merchant-2", 10L, owner))
                .doesNotThrowAnyException();
        assertThat(sameTargetPayment.getTargetId()).isEqualTo(10L);
    }

    @Test
    void commonCannotBindOtherUserPayment() {
        CustomUserDetails common = user(300L, "COMMON");
        Payment otherPayment = payment("merchant-1", 301L, null, null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(otherPayment));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-1", 10L, common))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(otherPayment.getTargetId()).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void commonCannotBindOwnPaymentToOtherUserReservation() {
        CustomUserDetails common = user(300L, "COMMON");
        Payment payment = payment("merchant-1", 300L, null, null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(payment));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation(10L, 301L)));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-1", 10L, common))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(payment.getTargetId()).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void commonCanBindOwnBannerApplicationButCannotBindOtherUserBannerApplication() {
        CustomUserDetails common = user(300L, "COMMON");
        Payment ownPayment = payment("merchant-1", 300L, null, "BANNER_APPLICATION", null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(ownPayment));
        when(bannerApplicationRepository.findById(10L)).thenReturn(Optional.of(bannerApplication(10L, 300L)));

        paymentService.updatePaymentTargetId("merchant-1", 10L, common);

        assertThat(ownPayment.getTargetId()).isEqualTo(10L);

        Payment otherTargetPayment = payment("merchant-2", 300L, null, "BANNER_APPLICATION", null);
        when(paymentRepository.findByMerchantUid("merchant-2")).thenReturn(Optional.of(otherTargetPayment));
        when(bannerApplicationRepository.findById(11L)).thenReturn(Optional.of(bannerApplication(11L, 301L)));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-2", 11L, common))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(otherTargetPayment.getTargetId()).isNull();
    }

    @Test
    void commonCanBindOwnBoothApplicationButCannotBindOtherUserBoothApplication() {
        CustomUserDetails common = user(300L, "COMMON");
        Payment ownPayment = payment("merchant-1", 300L, null, "BOOTH_APPLICATION", null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(ownPayment));
        when(boothApplicationRepository.findById(10L)).thenReturn(Optional.of(boothApplication(10L, "owner@example.com")));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(userEntity(300L, "owner@example.com")));

        paymentService.updatePaymentTargetId("merchant-1", 10L, common);

        assertThat(ownPayment.getTargetId()).isEqualTo(10L);

        Payment otherTargetPayment = payment("merchant-2", 300L, null, "BOOTH_APPLICATION", null);
        when(paymentRepository.findByMerchantUid("merchant-2")).thenReturn(Optional.of(otherTargetPayment));
        when(boothApplicationRepository.findById(11L)).thenReturn(Optional.of(boothApplication(11L, "other@example.com")));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(userEntity(301L, "other@example.com")));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-2", 11L, common))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(otherTargetPayment.getTargetId()).isNull();
    }

    @Test
    void commonCannotBindUnsupportedTargetCodes() {
        CustomUserDetails common = user(300L, "COMMON");
        Payment boothPayment = payment("merchant-1", 300L, null, "BOOTH", null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(boothPayment));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-1", 10L, common))
                .isInstanceOf(AccessDeniedException.class);

        Payment adPayment = payment("merchant-2", 300L, null, "AD", null);
        when(paymentRepository.findByMerchantUid("merchant-2")).thenReturn(Optional.of(adPayment));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-2", 20L, common))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(boothPayment);
        verify(paymentRepository, never()).save(adPayment);
    }

    @Test
    void commonCannotBindPaymentWithNullTargetCodeButDoesNotThrowNullPointerException() {
        CustomUserDetails common = user(300L, "COMMON");
        Payment payment = payment("merchant-1", 300L, null, null, null);
        when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-1", 10L, common))
                .isInstanceOf(AccessDeniedException.class)
                .isNotInstanceOf(NullPointerException.class);

        assertThat(payment.getTargetId()).isNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentRejectsForeignReservationTargetBeforeSave() {
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation(10L, 301L)));

        assertThatThrownBy(() -> paymentService.savePayment(paymentRequest("RESERVATION", 10L), 300L))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentRejectsForeignBannerApplicationTargetBeforeSave() {
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("BANNER_APPLICATION"))
                .thenReturn(Optional.of(targetType("BANNER_APPLICATION")));
        when(bannerApplicationRepository.findById(10L)).thenReturn(Optional.of(bannerApplication(10L, 301L)));

        assertThatThrownBy(() -> paymentService.savePayment(paymentRequest("BANNER_APPLICATION", 10L), 300L))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentRejectsForeignBoothApplicationTargetBeforeSave() {
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("BOOTH_APPLICATION"))
                .thenReturn(Optional.of(targetType("BOOTH_APPLICATION")));
        when(boothApplicationRepository.findById(10L)).thenReturn(Optional.of(boothApplication(10L, "other@example.com")));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(userEntity(301L, "other@example.com")));

        assertThatThrownBy(() -> paymentService.savePayment(paymentRequest("BOOTH_APPLICATION", 10L), 300L))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentAllowsOwnedTargets() {
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("BANNER_APPLICATION"))
                .thenReturn(Optional.of(targetType("BANNER_APPLICATION")));
        when(paymentTypeCodeRepository.getReferenceByCode("CARD")).thenReturn(paymentTypeCode());
        when(paymentStatusCodeRepository.getReferenceByCode("PENDING")).thenReturn(paymentStatusCode("PENDING"));
        when(bannerApplicationRepository.findById(10L)).thenReturn(Optional.of(bannerApplication(10L, 300L)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(1L);
            return payment;
        });

        assertThat(paymentService.savePayment(paymentRequest("BANNER_APPLICATION", 10L), 300L).getTargetId())
                .isEqualTo(10L);
    }

    @Test
    void savePaymentStoresReservationIntentAfterServerSideValidation() {
        Event event = event(1L, 100L);
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));
        when(paymentTypeCodeRepository.getReferenceByCode("CARD")).thenReturn(paymentTypeCode());
        when(paymentStatusCodeRepository.getReferenceByCode("PENDING")).thenReturn(paymentStatusCode("PENDING"));
        stubReservationRelation(event, 1L, 10L, 100);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(1L);
            return payment;
        });

        PaymentRequestDto request = reservationCreateRequest();

        paymentService.savePayment(request, 300L);

        verify(reservationPaymentIntentStore).save(new ReservationPaymentIntent(
                1L, 1L, 10L, 1, BigDecimal.valueOf(100), "RESERVATION", 300L, "merchant-create"));
    }

    @Test
    void savePaymentRejectsReservationClientPriceMismatch() {
        Event event = event(1L, 100L);
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));
        stubReservationRelation(event, 1L, 10L, 100);

        PaymentRequestDto request = reservationCreateRequest();
        request.setPrice(BigDecimal.valueOf(99));

        assertThatThrownBy(() -> paymentService.savePayment(request, 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가격");

        verify(paymentRepository, never()).save(any());
        verify(reservationPaymentIntentStore, never()).save(any());
    }

    @Test
    void savePaymentRejectsReservationClientAmountMismatch() {
        Event event = event(1L, 100L);
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));
        stubReservationRelation(event, 1L, 10L, 100);

        PaymentRequestDto request = reservationCreateRequest();
        request.setAmount(BigDecimal.valueOf(101));

        assertThatThrownBy(() -> paymentService.savePayment(request, 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("총액");

        verify(paymentRepository, never()).save(any());
        verify(reservationPaymentIntentStore, never()).save(any());
    }

    @Test
    void savePaymentRejectsReservationQuantityAboveMaxPurchase() {
        Event event = event(1L, 100L);
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));
        stubReservationRelation(event, 1L, 10L, 100, 1);

        PaymentRequestDto request = reservationCreateRequest();
        request.setQuantity(2);
        request.setAmount(BigDecimal.valueOf(200));

        assertThatThrownBy(() -> paymentService.savePayment(request, 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 구매 수량");

        verify(paymentRepository, never()).save(any());
        verify(reservationPaymentIntentStore, never()).save(any());
    }

    @Test
    void processFreeTicketDeletesReservationIntentAfterSuccessfulCompletion() {
        Event event = event(1L, 100L);
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));
        when(paymentTypeCodeRepository.getReferenceByCode("CARD")).thenReturn(paymentTypeCode());
        when(paymentStatusCodeRepository.getReferenceByCode("PENDING")).thenReturn(paymentStatusCode("PENDING"));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        stubReservationRelation(event, 1L, 10L, 0);
        Payment[] savedPayment = new Payment[1];
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(1L);
            savedPayment[0] = payment;
            return payment;
        });
        when(paymentRepository.findByMerchantUid("merchant-free"))
                .thenAnswer(invocation -> Optional.ofNullable(savedPayment[0]));
        when(reservationService.createReservation(any(), any(), any()))
                .thenReturn(reservation(77L, 300L));
        when(attendeeFormAttendeeService.saveAttendeeFormAndAttendee(any(), any()))
                .thenReturn(AttendeeFormSaveResponseDto.builder()
                        .reservationId(77L)
                        .token("token")
                        .build());

        PaymentRequestDto request = reservationCreateRequest();
        request.setMerchantUid("merchant-free");
        request.setPrice(BigDecimal.ZERO);
        request.setAmount(BigDecimal.ZERO);

        paymentService.processFreeTicket(request, 300L);

        verify(reservationPaymentIntentStore).save(new ReservationPaymentIntent(
                1L, 1L, 10L, 1, BigDecimal.ZERO, "RESERVATION", 300L, "merchant-free"));
        verify(reservationPaymentIntentStore).delete("merchant-free", 300L);
    }

    @Test
    void savePaymentRejectsUnsupportedTargetCodeWithTargetIdBeforeSave() {
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("AD"))
                .thenReturn(Optional.of(targetType("AD")));

        assertThatThrownBy(() -> paymentService.savePayment(paymentRequest("AD", 10L), 300L))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentRejectsResolvedNullTargetCodeButDoesNotThrowNullPointerException() {
        Users currentUser = userEntity(300L, "owner@example.com", "COMMON");
        when(userRepository.findById(300L)).thenReturn(Optional.of(currentUser));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType(null)));

        assertThatThrownBy(() -> paymentService.savePayment(paymentRequest("RESERVATION", 10L), 300L))
                .isInstanceOf(AccessDeniedException.class)
                .isNotInstanceOf(NullPointerException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentRejectsEventManagerTargetIdBeforeSave() {
        Users eventManager = userEntity(100L, "manager@example.com", "EVENT_MANAGER");
        when(userRepository.findById(100L)).thenReturn(Optional.of(eventManager));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("RESERVATION"))
                .thenReturn(Optional.of(targetType("RESERVATION")));

        assertThatThrownBy(() -> paymentService.savePayment(paymentRequest("RESERVATION", 10L), 100L))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void savePaymentAllowsAdminTargetId() {
        Users admin = userEntity(1L, "admin@example.com", "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(paymentTargetTypeRepository.findByPaymentTargetCode("AD"))
                .thenReturn(Optional.of(targetType("AD")));
        when(paymentTypeCodeRepository.getReferenceByCode("CARD")).thenReturn(paymentTypeCode());
        when(paymentStatusCodeRepository.getReferenceByCode("PENDING")).thenReturn(paymentStatusCode("PENDING"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(1L);
            return payment;
        });

        assertThat(paymentService.savePayment(paymentRequest("AD", 10L), 1L).getTargetId())
                .isEqualTo(10L);
    }

    @Test
    void nonNullDifferentTargetIdIsRejectedBeforeSave() {
        CustomUserDetails admin = user(1L, "ADMIN");
        Payment payment = payment("merchant-1", 300L, 10L, null);
        lenient().when(paymentRepository.findByMerchantUid("merchant-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.updatePaymentTargetId("merchant-1", 20L, admin))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(payment.getTargetId()).isEqualTo(10L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void eventManagerCannotManageListForOtherEvent() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 999L)));

        assertThatThrownBy(() -> paymentService.getAllPayments(1L, manager))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).findByEvent_EventId(any());
    }

    @Test
    void eventManagerCanListOwnEvent() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        Event event = event(1L, 100L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(paymentRepository.findByEvent_EventId(1L)).thenReturn(List.of(payment("merchant-1", 300L, 10L, event)));

        assertThat(paymentService.getAllPayments(1L, manager)).hasSize(1);
    }

    @Test
    void commonCannotCallManagementList() {
        assertThatThrownBy(() -> paymentService.getAllPayments(null, user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).findAll();
        verify(paymentRepository, never()).findByEvent_EventId(any());
    }

    @Test
    void completePaymentRejectsNullPrincipal() {
        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), null))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).findByMerchantUidForUpdate(any());
    }

    @Test
    void completePaymentRejectsOtherUsersPayment() {
        Payment payment = pendingPayment("merchant-complete", 301L, 10L, event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsNonPendingPayment() {
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event(1L, 100L));
        payment.setPaymentStatusCode(paymentStatusCode("CANCELED"));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 상태");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsMissingImpUid() {
        PaymentRequestDto request = completeRequest();
        request.setImpUid(" ");

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("impUid");

        verify(paymentRepository, never()).findByMerchantUidForUpdate(any());
    }

    @Test
    void completePaymentRejectsDuplicateCompletedImpUidForAnotherPayment() {
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByImpUidAndPaymentStatusCode_CodeAndMerchantUidNot(
                "imp-valid", "COMPLETED", "merchant-complete")).thenReturn(true);

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsConcurrentSameImpUidBeforePgLookup() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리 중");

        verify(paymentRepository, never()).findByMerchantUid(any());
        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRevalidatesLockedStatusAfterPgLookup() {
        Event event = event(1L, 100L);
        Payment snapshot = pendingPayment("merchant-complete", 300L, 10L, event);
        Payment locked = pendingPayment("merchant-complete", 300L, 10L, event);
        locked.setPaymentStatusCode(paymentStatusCode("COMPLETED"));
        doReturn(Optional.of(snapshot)).when(paymentRepository).findByMerchantUid("merchant-complete");
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(locked));
        stubValidReservationIntent(event, 1L, 10L);
        stubReservationRelation(event, 1L, 10L, 100);
        stubValidIamport();

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 상태");

        verify(iamportPaymentVerifier).findPayment("imp-valid");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentLoadsSnapshotAndLockedPaymentInSeparateTransactionBoundaries() {
        Event event = event(1L, 100L);
        Payment snapshot = pendingPayment("merchant-complete", 300L, 10L, event);
        Payment locked = pendingPayment("merchant-complete", 300L, 10L, event);
        doReturn(Optional.of(snapshot)).when(paymentRepository).findByMerchantUid("merchant-complete");
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(locked));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(locked));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubValidReservationIntent(event, 1L, 10L);
        stubValidIamport();
        stubReservationRelation(event, 1L, 10L, 100);

        paymentService.completePayment(completeRequest(), user(300L, "COMMON"));

        var ordered = inOrder(transactionManager, paymentRepository, iamportPaymentVerifier, redisTemplate);
        ordered.verify(transactionManager).getTransaction(argThat(definition -> definition.isReadOnly()));
        ordered.verify(paymentRepository).findByMerchantUid("merchant-complete");
        ordered.verify(transactionManager).commit(any());
        ordered.verify(iamportPaymentVerifier).findPayment("imp-valid");
        ordered.verify(transactionManager).getTransaction(argThat(definition -> !definition.isReadOnly()));
        ordered.verify(paymentRepository).findByMerchantUidForUpdate("merchant-complete");
        ordered.verify(transactionManager).commit(any());
        ordered.verify(redisTemplate).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void completePaymentRejectsPgMerchantMismatch() {
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(payment.getEvent(), 1L, 10L);
        stubReservationRelation(payment.getEvent(), 1L, 10L, 100);
        when(iamportPaymentVerifier.findPayment("imp-valid"))
                .thenReturn(new IamportPaymentInfo("imp-valid", "merchant-other", "paid", BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("아임포트");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsPgAmountMismatch() {
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(payment.getEvent(), 1L, 10L);
        stubReservationRelation(payment.getEvent(), 1L, 10L, 100);
        when(iamportPaymentVerifier.findPayment("imp-valid"))
                .thenReturn(new IamportPaymentInfo("imp-valid", "merchant-complete", "paid", BigDecimal.valueOf(101)));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("아임포트");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsPgStatusNotPaid() {
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(payment.getEvent(), 1L, 10L);
        stubReservationRelation(payment.getEvent(), 1L, 10L, 100);
        when(iamportPaymentVerifier.findPayment("imp-valid"))
                .thenReturn(new IamportPaymentInfo("imp-valid", "merchant-complete", "ready", BigDecimal.valueOf(100)));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("아임포트");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsTicketPricePaymentAmountMismatch() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);
        stubReservationRelation(event, 1L, 10L, 50);

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsScheduleOutsidePaymentEvent() {
        Event paymentEvent = event(1L, 100L);
        Event otherEvent = event(2L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, paymentEvent);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(paymentEvent, 1L, 10L);
        when(eventScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, otherEvent)));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스케줄");

        verify(scheduleTicketRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsMissingScheduleOrTicketForReservation() {
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        PaymentRequestDto request = completeRequest();
        request.setScheduleId(null);

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheduleId");

        verify(eventScheduleRepository, never()).findById(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsMissingScheduleTicketRelation() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);
        when(eventScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, event)));
        when(scheduleTicketRepository.findById(new ScheduleTicketId(10L, 1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스케줄");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsDifferentTicketFromStoredIntentEvenWithSameEventAndPrice() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);

        PaymentRequestDto request = completeRequest();
        request.setTicketId(11L);

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intent");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsDifferentScheduleFromStoredIntentEvenWithSameEventAndPrice() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);

        PaymentRequestDto request = completeRequest();
        request.setScheduleId(2L);

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intent");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsMissingReservationIntent() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(reservationPaymentIntentStore.find("merchant-complete", 300L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intent");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsReservationIntentWithDifferentMerchantUid() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(reservationPaymentIntentStore.find("merchant-complete", 300L))
                .thenReturn(Optional.of(new ReservationPaymentIntent(
                        1L, 1L, 10L, 1, BigDecimal.valueOf(100), "RESERVATION", 300L, "merchant-other")));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("intent");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsReservationAmountOrQuantityMismatch() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        payment.setQuantity(2);
        payment.setAmount(BigDecimal.valueOf(200));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액 또는 수량");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsRequestQuantityMismatchWithStoredIntent() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);

        PaymentRequestDto request = completeRequest();
        request.setQuantity(2);

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수량");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsRequestAmountMismatchWithStoredIntent() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);

        PaymentRequestDto request = completeRequest();
        request.setAmount(BigDecimal.valueOf(101));

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsRequestPriceMismatchWithStoredIntent() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        stubValidReservationIntent(event, 1L, 10L);

        PaymentRequestDto request = completeRequest();
        request.setPrice(BigDecimal.valueOf(101));

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가격");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentRejectsNonExactReservationUnitPrice() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        payment.setQuantity(3);
        payment.setAmount(BigDecimal.valueOf(100));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(reservationPaymentIntentStore.find("merchant-complete", 300L))
                .thenReturn(Optional.of(new ReservationPaymentIntent(
                        event.getEventId(),
                        1L,
                        10L,
                        3,
                        BigDecimal.valueOf(100),
                        "RESERVATION",
                        300L,
                        "merchant-complete")));

        PaymentRequestDto request = completeRequest();
        request.setQuantity(3);
        request.setAmount(BigDecimal.valueOf(100));
        request.setPrice(new BigDecimal("33.33"));

        assertThatThrownBy(() -> paymentService.completePayment(request, user(300L, "COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정확히 나누어");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentPropagatesReservationCompletionException() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, null, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubValidReservationIntent(event, 1L, 10L);
        stubValidIamport();
        stubReservationRelation(event, 1L, 10L, 100);
        when(reservationService.createReservation(any(), any(), any()))
                .thenThrow(new IllegalStateException("stock failed"));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예매 생성에 실패");
    }

    @Test
    void completePaymentAllowsValidReservationCompletion() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubValidReservationIntent(event, 1L, 10L);
        stubValidIamport();
        stubReservationRelation(event, 1L, 10L, 100);

        paymentService.completePayment(completeRequest(), user(300L, "COMMON"));

        assertThat(payment.getPaymentStatusCode().getCode()).isEqualTo("COMPLETED");
        assertThat(payment.getImpUid()).isEqualTo("imp-valid");
        verify(reservationPaymentIntentStore).delete("merchant-complete", 300L);
    }

    @Test
    void completePublicPaymentAllowsBoothApplicationWithoutPrincipal() {
        Payment payment = pendingPayment("merchant-public", 300L, 10L, "BOOTH_APPLICATION", null);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-public")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(iamportPaymentVerifier.findPayment("imp-public"))
                .thenReturn(new IamportPaymentInfo("imp-public", "merchant-public", "paid", BigDecimal.valueOf(100)));

        paymentService.completePublicPayment(publicCompleteRequest(), "BOOTH_APPLICATION");

        assertThat(payment.getPaymentStatusCode().getCode()).isEqualTo("COMPLETED");
        assertThat(payment.getImpUid()).isEqualTo("imp-public");
    }

    @Test
    void completePublicPaymentAllowsBannerApplicationWithoutPrincipal() {
        Payment payment = pendingPayment("merchant-public", 300L, 10L, "BANNER_APPLICATION", null);
        when(paymentRepository.findByMerchantUidForUpdate("merchant-public")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(iamportPaymentVerifier.findPayment("imp-public"))
                .thenReturn(new IamportPaymentInfo("imp-public", "merchant-public", "paid", BigDecimal.valueOf(100)));

        paymentService.completePublicPayment(publicCompleteRequest(), "BANNER_APPLICATION");

        assertThat(payment.getPaymentStatusCode().getCode()).isEqualTo("COMPLETED");
        assertThat(payment.getImpUid()).isEqualTo("imp-public");
    }

    @Test
    void completePublicPaymentRejectsWrongTargetType() {
        Payment payment = pendingPayment("merchant-public", 300L, 10L, "RESERVATION", event(1L, 100L));
        when(paymentRepository.findByMerchantUidForUpdate("merchant-public")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.completePublicPayment(publicCompleteRequest(), "BOOTH_APPLICATION"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("공개 결제 완료 대상");

        verify(iamportPaymentVerifier, never()).findPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completePaymentUsesLockedLookupSoSecondConfirmDoesNotRunCompletionAgain() {
        Event event = event(1L, 100L);
        Payment payment = pendingPayment("merchant-complete", 300L, 10L, event);
        doReturn(Optional.of(payment)).when(paymentRepository).findByMerchantUid("merchant-complete");
        when(paymentRepository.findByMerchantUidForUpdate("merchant-complete")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentStatusCodeRepository.findByCode("COMPLETED")).thenReturn(Optional.of(paymentStatusCode("COMPLETED")));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubValidReservationIntent(event, 1L, 10L);
        stubValidIamport();
        stubReservationRelation(event, 1L, 10L, 100);

        paymentService.completePayment(completeRequest(), user(300L, "COMMON"));

        assertThatThrownBy(() -> paymentService.completePayment(completeRequest(), user(300L, "COMMON")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 상태");

        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).findByMerchantUidForUpdate("merchant-complete");
        verify(paymentRepository, org.mockito.Mockito.times(1)).save(any(Payment.class));
        verify(iamportPaymentVerifier, org.mockito.Mockito.times(1)).findPayment("imp-valid");
    }

    private Payment payment(String merchantUid, Long userId, Long targetId, Event event) {
        return payment(merchantUid, userId, targetId, "RESERVATION", event);
    }

    private Payment payment(String merchantUid, Long userId, Long targetId, String targetCode, Event event) {
        return Payment.builder()
                .paymentId(1L)
                .event(event)
                .user(new Users(userId))
                .paymentTargetType(PaymentTargetType.builder()
                        .paymentTargetCode(targetCode)
                        .paymentTargetName(targetCode)
                        .build())
                .targetId(targetId)
                .merchantUid(merchantUid)
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(100))
                .paymentTypeCode(PaymentTypeCode.builder()
                        .paymentTypeCodeId(1)
                        .code("CARD")
                        .name("카드")
                        .build())
                .paymentStatusCode(PaymentStatusCode.builder()
                        .paymentStatusCodeId(1)
                        .code("COMPLETED")
                        .name("완료")
                        .build())
                .build();
    }

    private Payment pendingPayment(String merchantUid, Long userId, Long targetId, Event event) {
        Payment payment = payment(merchantUid, userId, targetId, "RESERVATION", event);
        payment.setPaymentStatusCode(paymentStatusCode("PENDING"));
        return payment;
    }

    private Payment pendingPayment(String merchantUid, Long userId, Long targetId, String targetCode, Event event) {
        Payment payment = payment(merchantUid, userId, targetId, targetCode, event);
        payment.setPaymentStatusCode(paymentStatusCode("PENDING"));
        return payment;
    }

    private PaymentRequestDto completeRequest() {
        return PaymentRequestDto.builder()
                .merchantUid("merchant-complete")
                .impUid("imp-valid")
                .scheduleId(1L)
                .ticketId(10L)
                .build();
    }

    private PaymentRequestDto publicCompleteRequest() {
        return PaymentRequestDto.builder()
                .merchantUid("merchant-public")
                .impUid("imp-public")
                .build();
    }

    private void stubValidIamport() {
        when(iamportPaymentVerifier.findPayment("imp-valid"))
                .thenReturn(new IamportPaymentInfo("imp-valid", "merchant-complete", "paid", BigDecimal.valueOf(100)));
    }

    private void stubValidReservationIntent(Event event, Long scheduleId, Long ticketId) {
        when(reservationPaymentIntentStore.find("merchant-complete", 300L))
                .thenReturn(Optional.of(new ReservationPaymentIntent(
                        event.getEventId(),
                        scheduleId,
                        ticketId,
                        1,
                        BigDecimal.valueOf(100),
                        "RESERVATION",
                        300L,
                        "merchant-complete")));
    }

    private void stubReservationRelation(Event event, Long scheduleId, Long ticketId, Integer ticketPrice) {
        stubReservationRelation(event, scheduleId, ticketId, ticketPrice, null);
    }

    private void stubReservationRelation(Event event, Long scheduleId, Long ticketId, Integer ticketPrice, Integer maxPurchase) {
        EventSchedule schedule = schedule(scheduleId, event);
        Ticket ticket = Ticket.builder()
                .ticketId(ticketId)
                .name("ticket")
                .price(ticketPrice)
                .maxPurchase(maxPurchase)
                .build();
        ScheduleTicket scheduleTicket = ScheduleTicket.builder()
                .id(new ScheduleTicketId(ticketId, scheduleId))
                .eventSchedule(schedule)
                .ticket(ticket)
                .build();

        when(eventScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(scheduleTicketRepository.findById(new ScheduleTicketId(ticketId, scheduleId)))
                .thenReturn(Optional.of(scheduleTicket));
        lenient().when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
    }

    private EventSchedule schedule(Long scheduleId, Event event) {
        return EventSchedule.builder()
                .scheduleId(scheduleId)
                .event(event)
                .build();
    }

    private PaymentRequestDto paymentRequest(String targetCode, Long targetId) {
        return PaymentRequestDto.builder()
                .paymentTargetType(targetCode)
                .targetId(targetId)
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .pgProvider("html5_inicis")
                .merchantUid("merchant-create")
                .build();
    }

    private PaymentRequestDto reservationCreateRequest() {
        return PaymentRequestDto.builder()
                .paymentTargetType("RESERVATION")
                .eventId(1L)
                .scheduleId(1L)
                .ticketId(10L)
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(100))
                .pgProvider("html5_inicis")
                .merchantUid("merchant-create")
                .build();
    }

    private Reservation reservation(Long reservationId, Long userId) {
        Reservation reservation = new Reservation(null, null, null, new Users(userId), 1, 100);
        reservation.setReservationId(reservationId);
        return reservation;
    }

    private BannerApplication bannerApplication(Long applicationId, Long applicantUserId) {
        return BannerApplication.builder()
                .id(applicationId)
                .applicantId(new Users(applicantUserId))
                .build();
    }

    private BoothApplication boothApplication(Long applicationId, String boothEmail) {
        BoothApplication boothApplication = new BoothApplication();
        boothApplication.setId(applicationId);
        boothApplication.setBoothEmail(boothEmail);
        return boothApplication;
    }

    private Users userEntity(Long userId, String email) {
        Users user = new Users(userId);
        user.setEmail(email);
        return user;
    }

    private Users userEntity(Long userId, String email, String roleCode) {
        Users user = userEntity(userId, email);
        UserRoleCode role = mock(UserRoleCode.class);
        lenient().when(role.getCode()).thenReturn(roleCode);
        user.setRoleCode(role);
        return user;
    }

    private PaymentTargetType targetType(String targetCode) {
        return PaymentTargetType.builder()
                .paymentTargetCode(targetCode)
                .paymentTargetName(targetCode)
                .build();
    }

    private PaymentTypeCode paymentTypeCode() {
        return PaymentTypeCode.builder()
                .paymentTypeCodeId(1)
                .code("CARD")
                .name("카드")
                .build();
    }

    private PaymentStatusCode paymentStatusCode(String code) {
        return PaymentStatusCode.builder()
                .paymentStatusCodeId(1)
                .code(code)
                .name(code)
                .build();
    }

    private Event event(Long eventId, Long managerUserId) {
        EventAdmin eventAdmin = new EventAdmin();
        eventAdmin.setUserId(managerUserId);
        eventAdmin.setUser(new Users(managerUserId));

        Event event = new Event();
        event.setEventId(eventId);
        event.setTitleKr("행사");
        event.setTitleEng("Event");
        event.setManager(eventAdmin);
        EventDetail detail = new EventDetail();
        detail.setStartDate(java.time.LocalDate.now());
        detail.setEndDate(java.time.LocalDate.now().plusDays(1));
        event.setEventDetail(detail);
        return event;
    }

    private CustomUserDetails user(Long userId, String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        lenient().when(userDetails.getRoleCode()).thenReturn(roleCode);
        return userDetails;
    }
}
