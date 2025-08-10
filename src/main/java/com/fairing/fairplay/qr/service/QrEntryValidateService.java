package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.qr.dto.EntryPolicyDto;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrActionCodeRepository;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;
import com.fairing.fairplay.qr.repository.QrCheckStatusCodeRepository;
import com.fairing.fairplay.qr.repository.QrLogRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrEntryValidateService {

  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrLogService qrLogService;

  // 재입장 가능 여부 검토
  public void verifyReEntry(QrTicket qrTicket) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY) != null;
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT) != null;

    if (entryPolicy.getReentryAllowed()) {
      verifyAllowedReEntry(entryPolicy, hasEntry, hasExit);
    } else {
      verifyDisallowedReEntry(hasEntry, hasExit);
    }
  }

  // 재입장 가능한 행사일 경우 재입장 가능 여부 검토
  public void verifyAllowedReEntry(EntryPolicyDto entryPolicy, boolean hasEntry, boolean hasExit) {
    if (entryPolicy.getCheckInAllowed() && entryPolicy.getCheckOutAllowed()) {
      // 입/퇴장 모두 스캔
      if (!hasEntry || !hasExit) {
        throw new CustomException(HttpStatus.UNAUTHORIZED, "입장 및 퇴장 기록이 모두 있어야 재입장 가능합니다.");
      }
    } else if (entryPolicy.getCheckInAllowed()) {
      // 입장만 스캔
      if (!hasEntry) {
        throw new CustomException(HttpStatus.UNAUTHORIZED, "입장 기록이 없어 재입장할 수 없습니다.");
      }
    } else if (entryPolicy.getCheckOutAllowed()) {
      // 퇴장만 스캔
      if (!hasExit) {
        throw new CustomException(HttpStatus.UNAUTHORIZED, "퇴장 기록이 없어 재입장할 수 없습니다.");
      }
    }
  }

  // 재입장 불가능한 행사일 경우 재입장 가능 여부 검토
  public void verifyDisallowedReEntry(boolean hasEntry, boolean hasExit) {
    if (hasEntry || hasExit) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "이미 입장 또는 퇴장된 티켓이므로 입장할 수 없습니다.");
    }
  }

  // 잘못된 순서로 스캔 ( checkStatus - 시도하려는 동작 )
  public void preventInvalidScan(QrTicket qrTicket, String checkStatus) {
    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketOrderByCreatedAtDesc(
        qrTicket);

    // 처음 입장하여 직전 로그가 없는데 EXIT 스캔하는 경우
    if (lastLogOpt.isEmpty()) {
      if (QrCheckStatusCode.EXIT.equals(checkStatus)) {
        qrLogService.invalidQrLog(qrTicket);
        throw new CustomException(HttpStatus.BAD_REQUEST, "");
      }
      return;
    }

    boolean isValid = isQrCheckStatusCodeValid(checkStatus, lastLogOpt.get());

    // 만약 isValid가 false로 변경 -> 직전 statuscode가 올바르지 않음
    if (!isValid) {
      qrLogService.invalidQrLog(qrTicket);
      throw new CustomException(HttpStatus.BAD_REQUEST,
          "잘못된 입퇴장 동작입니다. 마지막 상태: "
              + lastLogOpt.get().getCheckStatusCode().getName());
    }
  }

  public boolean isQrCheckStatusCodeValid(String checkStatus, QrCheckLog lastLogOpt) {
    String lastStatus = lastLogOpt.getCheckStatusCode().getCode();

    boolean isValid = true;
    if (QrCheckStatusCode.ENTRY.equals(checkStatus)) {
      // ENTRY는 직전이 EXIT 여야 입장 가능
      isValid = QrCheckStatusCode.EXIT.equals(lastStatus);
    } else if (QrCheckStatusCode.REENTRY.equals(checkStatus)) {
      // REENTRY는 직전이 EXIT 여야 입장 가능
      isValid = QrCheckStatusCode.EXIT.equals(lastStatus);
    } else if (QrCheckStatusCode.EXIT.equals(checkStatus)) {
      // EXIT는 직전이 ENTRY, REENTRY 여야 입장 가능
      isValid = QrCheckStatusCode.ENTRY.equals(lastStatus) || QrCheckStatusCode.REENTRY.equals(
          lastStatus);
    }
    return isValid;
  }

  // 중복 스캔 방지 - 최근 기록을 보고 너무 짧은 시간 내의 동일 상태 코드를 막음
  public void preventDuplicateScan(QrTicket qrTicket, String statusCode) {
    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
        qrTicket, statusCode);

    // 5초 간격으로 빠르게 스캔했을 경우
    if (lastLogOpt.isPresent()) {
      QrCheckLog lastLog = lastLogOpt.get();
      if (lastLog.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5))) {
        qrLogService.duplicateQrLog(qrTicket);
        throw new CustomException(HttpStatus.CONFLICT, "QR 코드를 너무 빠르게 스캔하였습니다.");
      }
    }
  }

  private EntryPolicyDto buildEntryPolicy(QrTicket qrTicket) {
    EventDetail eventDetail = qrTicket.getEventSchedule().getEvent().getEventDetail();

    // 입장 스캔 여부, 퇴장 스캔 여부, 재입장 가능 여부 정책 dto
    return EntryPolicyDto.builder()
        .checkInAllowed(true)
        .checkOutAllowed(eventDetail.getCheckOutAllowed())
        .reentryAllowed(eventDetail.getReentryAllowed())
        .build();
  }
}
