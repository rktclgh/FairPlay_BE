package com.fairing.fairplay.realtime.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimeSseServiceTest {

    private final QrTicketRepository qrTicketRepository = mock(QrTicketRepository.class);
    private final RealtimeSseService service = new RealtimeSseService(qrTicketRepository);

    @Test
    void openQrTicketStreamRejectsMissingTicket() {
        when(qrTicketRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.openQrTicketStream(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void openQrTicketStreamRejectsTicketOwnedByAnotherUser() {
        when(qrTicketRepository.existsById(1L)).thenReturn(true);
        when(qrTicketRepository.existsByIdAndReservationUserId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.openQrTicketStream(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void openQrTicketStreamAllowsTicketOwner() {
        when(qrTicketRepository.existsById(1L)).thenReturn(true);
        when(qrTicketRepository.existsByIdAndReservationUserId(1L, 10L)).thenReturn(true);

        SseEmitter emitter = service.openQrTicketStream(1L, 10L);

        assertThat(emitter).isNotNull();
    }

    @Test
    void sendingWithoutSubscribersIsNoop() {
        assertThatCode(() -> service.sendQrTicketEvent(1L, "체크인 완료"))
                .doesNotThrowAnyException();
    }
}
