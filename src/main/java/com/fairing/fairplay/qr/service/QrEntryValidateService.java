package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.qr.dto.EntryPolicyDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrActionCodeRepository;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;
import com.fairing.fairplay.qr.repository.QrCheckStatusCodeRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrEntryValidateService {

  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrLogService qrLogService;
  private final QrActionCodeRepository qrActionCodeRepository;
  private final QrCheckStatusCodeRepository qrCheckStatusCodeRepository;

  // 재입장 가능 여부 검토
   @Transactional(readOnly = true)
  public void verifyReEntry(QrTicket qrTicket) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY) != null;
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT) != null;

    if (entryPolicy.isReentryAllowed()) {
      verifyAllowedReEntry(entryPolicy, hasEntry, hasExit);
    } else {
      verifyDisallowedReEntry(hasEntry, hasExit);
    }
  }

  // 재입장 가능한 행사일 경우 재입장 가능 여부 검토
  public void verifyAllowedReEntry(EntryPolicyDto entryPolicy, boolean hasEntry, boolean hasExit) {
    if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
      // 입/퇴장 모두 스캔
      if ((hasEntry && !hasExit) || (!hasEntry && hasExit)) {
        throw new CustomException(HttpStatus.UNAUTHORIZED, "입장 및 퇴장 기록이 모두 있어야 재입장 가능합니다.");
      }
    } else if (entryPolicy.isCheckInAllowed()) {
      // 입장만 스캔
      if (!hasEntry) {
        throw new CustomException(HttpStatus.UNAUTHORIZED, "입장 기록이 없어 재입장할 수 없습니다.");
      }
    } else if (entryPolicy.isCheckOutAllowed()) {
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
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.INVALID);
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.INVALID);

    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketOrderByCreatedAtDesc(
        qrTicket);

    // 이전 로그가 없는 경우(초기 상태)
    if (lastLogOpt.isEmpty()) {
      // EXIT은 불가.단. 퇴장 스캔만하고 입장 스캔을 하지 않는 행사일 경우가 아니어야함.
      if (QrCheckStatusCode.EXIT.equals(checkStatus) && !(!entryPolicy.isCheckInAllowed()
          && entryPolicy.isCheckOutAllowed())) {
        qrLogService.invalidQrLog(qrTicket, qrActionCode, qrCheckStatusCode);
        throw new CustomException(HttpStatus.BAD_REQUEST, "입장 처리가 완료된 티켓이 아닙니다.");
      }
      // 재입장 허용되지 않는 행사인데 재입장 스캔할 경우 예외 발생
      if (QrCheckStatusCode.REENTRY.equals(checkStatus) && !entryPolicy.isReentryAllowed()) {
        qrLogService.invalidQrLog(qrTicket, qrActionCode, qrCheckStatusCode);
        throw new CustomException(HttpStatus.UNAUTHORIZED, "재입장이 허용되지 않는 행사입니다.");
      }
      return;
    }

    // 상태 전이 + 이벤트 정책(입장,퇴장,재입장 허용)을 같이 검사
    QrCheckLog qrCheckLog = lastLogOpt.get();
    if (!isQrCheckStatusCodeValid(checkStatus, qrCheckLog, entryPolicy)) {
      qrLogService.invalidQrLog(qrTicket, qrActionCode, qrCheckStatusCode);
      throw new CustomException(HttpStatus.BAD_REQUEST,
          "잘못된 입퇴장 동작입니다. 마지막 상태: "
              + lastLogOpt.get().getCheckStatusCode().getName());
    }
  }

  public boolean isQrCheckStatusCodeValid(String checkStatus, QrCheckLog lastLogOpt,
      EntryPolicyDto entryPolicy) {
    String lastStatus = lastLogOpt.getCheckStatusCode().getCode();

    boolean reentryAllowed = entryPolicy.isReentryAllowed();
    boolean checkInAllowed = entryPolicy.isCheckInAllowed();
    boolean checkOutAllowed = entryPolicy.isCheckOutAllowed();

    /*
     * 입장 스캔 퇴장 스캔 재입장가능
     *
     * 재입장 가능
     * 입장, 퇴장
     * -> 입장스캔 -> 아무기록 없으면 가능
     * -> 재입장스캔 -> 이전 퇴장 스캔 기록 O 가능(필요하면 입장스캔기록ㄲ자ㅣ..?)
     * -> 퇴장 스캔 -> 이전 입장 또는 재입장 스캔 있어야 가능
     * 입장만
     * -> 입장 스캔 -> 아무 기록 없으면 언제나 가능
     * -> 재입장 스캔 -> 이전 입장 기록 있어야 가능
     * 퇴장만
     * -> 퇴장 스캔 -> 아무기록없거나 퇴장 기록이 있으면 가능. 대신 퇴장 기록 있으면 입장했다고 판단
     *
     * 재입장 불가능
     * 입장, 퇴장
     * -> 입장스캔 -> 아무기록 없으면 가능
     * -> 재입장스캔 -> 이전 입장스캔 기록 있으면 안됨
     * -> 퇴장 스캔 -> 이전 입장스캔 기록 있어야됨. 이전 퇴장 스캔 기록 있으면 안됨
     * 입장만
     * -> 입장 스캔 -> 아무 기록 없으면 언제나 가능
     * -> 재입장 스캔 -> 이전 입장스캔 기록 있으면 안됨
     * 퇴장만
     * -> 퇴장 스캔 -> 아무 기록 없으면 가능. 단, 한개의 퇴장기록이라도 있으면 재입장 안됨
     * */
    // --- 재입장 허용 ---
    if (reentryAllowed) {
      if (checkInAllowed && checkOutAllowed) {
        // 입·퇴장 모두 스캔
        if (QrCheckStatusCode.ENTRY.equals(checkStatus)) { // 만약 현재 동작이 입장일 경우
          return QrCheckStatusCode.EXIT.equals(lastStatus); // 이전 기록이 퇴장이면 가능 true
        } else if (QrCheckStatusCode.REENTRY.equals(checkStatus)) { //현재 동작 재입장
          return QrCheckStatusCode.EXIT.equals(lastStatus); // 이전 기록 퇴장이면 가능
        } else if (QrCheckStatusCode.EXIT.equals(checkStatus)) { // 현재 동작 퇴장
          return QrCheckStatusCode.ENTRY.equals(lastStatus) // 이전 기록 입장 또는 재입장이면 가능
              || QrCheckStatusCode.REENTRY.equals(lastStatus);
        }
      } else if (checkInAllowed) {
        // 입장만 스캔
        if (QrCheckStatusCode.ENTRY.equals(checkStatus)) { // 현재 동작이 입장일 경우
          return true; // 아무 기록 없어도 가능
        } else if (QrCheckStatusCode.REENTRY.equals(checkStatus)) { // 현재 동작이 재입장일 경우
          return QrCheckStatusCode.ENTRY.equals(lastStatus); // 이전 동작이 입장이면 가능
        }
      } else if (checkOutAllowed) {
        // 퇴장만 스캔
        return QrCheckStatusCode.EXIT.equals(checkStatus); // 현재 동작이 퇴장일 경우
      }
    }

    // --- 재입장 불가 ---
    else {
      if (checkInAllowed && checkOutAllowed) {
        // 입·퇴장 모두 스캔
        if (QrCheckStatusCode.ENTRY.equals(checkStatus)) { // 현재 동작이 입장일 경우
          return true; // 첫 입장 가능
        } else if (QrCheckStatusCode.REENTRY.equals(checkStatus)) { // 현재 동작이 재입장일 경우
          return false; // 재입장 불가
        } else if (QrCheckStatusCode.EXIT.equals(checkStatus)) { // 현재 동작이 퇴장일 경우
          return QrCheckStatusCode.ENTRY.equals(lastStatus); // 이전 동작이 입장이면 가능
        }
      } else if (checkInAllowed) {
        // 입장만 스캔
        if (QrCheckStatusCode.ENTRY.equals(checkStatus)) { // 현재 동작이 입장일 경우
          return true; // 첫 입장 가능
        } else if (QrCheckStatusCode.REENTRY.equals(checkStatus)) { // 현재 동작이 재입장일 경우
          return false; // 입장 불가
        }
      } else if (checkOutAllowed) {
        // 퇴장만 스캔
        if (QrCheckStatusCode.EXIT.equals(checkStatus)) { // 현재 동작이 퇴장일 경우
          return !QrCheckStatusCode.EXIT.equals(lastStatus); // 이전 동작이 없으면 가능. 있으면 재입장임
        }
      }
    }

    return false;
  }

  // 중복 스캔 방지 - 최근 기록을 보고 너무 짧은 시간 내의 동일 상태 코드를 막음
  public void preventDuplicateScan(QrTicket qrTicket, String statusCode) {
    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
        qrTicket, statusCode);
    QrActionCode invalidQrActionCode = validateQrActionCode(
        QrActionCode.INVALID);
    QrCheckStatusCode duplicateQrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.DUPLICATE);

    // 5초 간격으로 빠르게 스캔했을 경우
    if (lastLogOpt.isPresent()) {
      QrCheckLog lastLog = lastLogOpt.get();
      if (lastLog.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5))) {
        qrLogService.duplicateQrLog(qrTicket, invalidQrActionCode, duplicateQrCheckStatusCode);
        throw new CustomException(HttpStatus.CONFLICT, "QR 코드를 너무 빠르게 스캔하였습니다.");
      }
    }
  }

  // QR 로그 코드 검증
  public QrActionCode validateQrActionCode(String qrActionCode) {
    return qrActionCodeRepository.findByCode(qrActionCode)
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "QR 상태 코드가 올바르지 않습니다."));
  }

  // QR 체크인 코드 검증
  public QrCheckStatusCode validateQrCheckStatusCode(String qrCheckStatusCode) {
    return qrCheckStatusCodeRepository.findByCode(qrCheckStatusCode)
        .orElseThrow(() -> new CustomException(
            HttpStatus.BAD_REQUEST, "QR 체크인 상태 코드가 올바르지 않습니다."));
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
