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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrEntryValidateService {

  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrLogService qrLogService;
  private final QrActionCodeRepository qrActionCodeRepository;
  private final QrCheckStatusCodeRepository qrCheckStatusCodeRepository;

  // 정책 따른 체크인 가능 자체 판단
  public void checkEntryExitPolicy(QrTicket qrTicket, String entryType) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);
    log.info("reentry:{} checkIn:{} checkOut:{}", entryPolicy.isReentryAllowed(),
        entryPolicy.isCheckInAllowed(), entryPolicy.isCheckOutAllowed());

    if (!entryPolicy.isCheckInAllowed() && entryType.equals(QrCheckStatusCode.ENTRY)) {
      // 입장 스캔 허용 안하는데 입장 스캔 하는 경우
      throw new CustomException(HttpStatus.FORBIDDEN, "입장 스캔을 허용하지 않은 행사입니다.");
    } else if (!entryPolicy.isCheckOutAllowed() && entryType.equals(QrCheckStatusCode.EXIT)) {
      // 퇴장 스캔 허용 안하는데 퇴장 스캔 하는 경우
      throw new CustomException(HttpStatus.FORBIDDEN, "퇴장 스캔을 허용하지 않은 행사입니다.");
    }
  }

  // 정책 따른 이전 상태 확인
  public QrCheckStatusCode determineRequiredQrStatusForBoothEntry(QrTicket qrTicket) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    QrCheckStatusCode entryCheckStatus = qrCheckStatusCodeRepository.findByCode(
        QrCheckStatusCode.ENTRY).orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "상태 코드가 올바르지 않습니다.")
    );
    QrCheckStatusCode reEntryCheckStatus = qrCheckStatusCodeRepository.findByCode(
        QrCheckStatusCode.REENTRY).orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "상태 코드가 올바르지 않습니다.")
    );

    QrCheckLog prevQrCheckLog = qrCheckLogRepository.findTop1ByQrTicketOrderByCreatedAtDesc(
        qrTicket).orElse(null);

    if (entryPolicy.isReentryAllowed()) {
      // 재입장 가능 행사
      if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
        // 입퇴장 모두 스캔
        if (prevQrCheckLog == null) {
          // 이전 스캔 기록이 없을 경우 입장 기록 저장
          return entryCheckStatus;
        } else if (prevQrCheckLog.getCheckStatusCode().getCode().equals(QrCheckStatusCode.EXIT)) {
          // 이전 기록이 EXIT면 재입장 기록 저장
          return reEntryCheckStatus;
        }
      } else if (entryPolicy.isCheckInAllowed()) {
        // 입장만 스캔
        if (prevQrCheckLog == null) {
          // 이전 스캔 기록이 없을 경우 입장 기록 저장
          return entryCheckStatus;
        }
      }
    } else {
      // 재입장 불가능 행사
      if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
        // 입퇴장 모두 스캔
        // 이전 기록 없으면 입장 기록, 있으면 null
        return prevQrCheckLog == null ? entryCheckStatus : null;
      } else if (entryPolicy.isCheckInAllowed()) {
        // 입장만 스캔
        // 이전 기록 없으면 입장. 있으면 null
        return prevQrCheckLog == null ? entryCheckStatus : null;
      }
    }

    return null;
  }

  // 체크인 재입장 가능 여부
  public void verifyCheckInReEntry(QrTicket qrTicket) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    // 입장 기록 조회
    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY) != null;
    // 퇴장 기록 조회
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT) != null;
    log.info("hasEntry: {} hasExit : {}", hasEntry, hasExit);

    if (entryPolicy.isReentryAllowed()) {
      // 재입장 가능한 행사
      log.info("재입장 가능한 행사");
      if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
        log.info("체크인, 체크아웃 모두 허용");
        // 체크인, 체크아웃 모두 허용 시
        if (hasEntry && !hasExit) {
          log.info("퇴장 스캔이 되지 않음");
          // 퇴장 스캔이 되지 않음
          throw new CustomException(HttpStatus.FORBIDDEN, "퇴장 처리가 되지 않아 입장하실 수 없습니다");
        }
      }
    } else if (!entryPolicy.isReentryAllowed()) {
      log.info("재입장 불가능 행사");
      if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
        // 체크인, 체크아웃 모두 허용 시
        log.info("체크인, 체크아웃 모두 허용");
        if (hasEntry || hasExit) {
          log.info("입장 또는 퇴장 기록이 있음");
          // 입 또는 퇴장 기록이 있을 때 체크인 하려는 경우 -> 재입장 불가하므로 안됨
          throw new CustomException(HttpStatus.FORBIDDEN, "입장 또는 퇴장 처리가 완료된 티켓이므로 입장하실 수 없습니다");
        }
      } else if (entryPolicy.isCheckInAllowed()) {
        log.info("입장 스캔만함");
        if (hasEntry) {
          log.info("입장 기록이 있음");
          throw new CustomException(HttpStatus.FORBIDDEN, "이미 입장 처리가 완료된 티켓이므로 입장하실 수 없습니다.");
        }
      }
    }
  }

  // 체크인 재입장 가능 여부
  public void verifyCheckOutReEntry(QrTicket qrTicket) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    // 입장 기록 조회
    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY) != null;
    boolean hasReEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.REENTRY) != null;
    // 퇴장 기록 조회
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT) != null;
    log.info("hasEntry: {} hasExit : {}", hasEntry, hasExit);

    if (entryPolicy.isReentryAllowed()) {
      // 재입장 가능한 행사
      if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
        // 체크인, 체크아웃 모두 허용 시
        if (!hasEntry) {
          // 퇴장하는데 입장 스캔이 되지 않음
          throw new CustomException(HttpStatus.FORBIDDEN, "입장 처리가 되지 않아 퇴장 처리가 되지 않습니다.");
        }
      }
    } else if (!entryPolicy.isReentryAllowed()) {
      if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
        // 체크인, 체크아웃 모두 허용 시
        if (!hasEntry) {
          // 재입장 허용 안되는데 퇴장하려는데 이전 입장 기록이 없음
          throw new CustomException(HttpStatus.FORBIDDEN, "입장 처리가 되지 않아 퇴장 처리가 되지 않습니다.");
        }
      } else if (entryPolicy.isCheckInAllowed()) {
        if (hasEntry) {
          throw new CustomException(HttpStatus.FORBIDDEN, "이미 입장 처리가 완료된 티켓이므로 입장하실 수 없습니다.");
        }
      } else if (entryPolicy.isCheckOutAllowed()) {
        if (hasExit) {
          throw new CustomException(HttpStatus.FORBIDDEN, "이미 퇴장 처리가 완료된 티켓이므로 재입장입니다.");
        }
      }
    }
  }

  // 재입장 가능 여부 검토
  @Transactional(readOnly = true)
  public void verifyReEntry(QrTicket qrTicket) {
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY) != null
        || qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.REENTRY) != null;
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT) != null;

    log.info("hasEntry={} hasExit={}", hasEntry, hasExit);

    if (entryPolicy.isReentryAllowed()) {
      log.info("재입장={}", entryPolicy.isReentryAllowed());
      verifyAllowedReEntry(entryPolicy, hasEntry, hasExit);
    } else {
      log.info("재입장={}", entryPolicy.isReentryAllowed());
      verifyDisallowedReEntry(hasEntry, hasExit);
    }
  }

  // 재입장 가능한 행사일 경우 재입장 가능 여부 검토
  public void verifyAllowedReEntry(EntryPolicyDto entryPolicy, boolean hasEntry, boolean hasExit) {
    // 입/퇴장 모두 스캔
    if (entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed()) {
      if ((hasEntry && !hasExit) || (!hasEntry && hasExit)) {
        log.info("verifyAllowedReEntry 입퇴장모두스캔");
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
    log.info("preventInvalidScan 시작, checkStatus={}", checkStatus);
    EntryPolicyDto entryPolicy = buildEntryPolicy(qrTicket);

    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.INVALID);
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.INVALID);

    log.info("preventInvalidScan qrActionCode={} qrCheckStatusCode={}", qrActionCode,
        qrCheckStatusCode);

    // 바로 직전 동작 조회 (entry, reentry, exit)
    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketOrderByCreatedAtDesc(
        qrTicket);

    // 이전 로그가 없는 경우(초기 상태)
    if (lastLogOpt.isEmpty()) {
      log.info("마지막 QrCheckLog 상태: 없음");

      if (QrCheckStatusCode.REENTRY.equals(checkStatus)) {
        log.info("이전 로그가 없는데 재입장 하려함 또는 재입장이 허용되지 않는 행사임");
        qrLogService.invalidQrLog(qrTicket, qrActionCode, qrCheckStatusCode);
        throw new CustomException(HttpStatus.BAD_REQUEST, "입장 기록이 없습니다. 최초입장입니다.");
      }

      // checkin은 사용하지 않지만 checkout은 사용하는 경우
      boolean isExitAllowedInitially =
          !entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed();

      if (QrCheckStatusCode.EXIT.equals(checkStatus)) {
        if (!isExitAllowedInitially) {
          log.info("퇴장하려는데 퇴장스캔만 허용하는 행사가 아님에도 이전 로그가 없음");
          qrLogService.invalidQrLog(qrTicket, qrActionCode, qrCheckStatusCode);
          throw new CustomException(HttpStatus.BAD_REQUEST, "이전 기록이 없으므로 퇴장 처리가 되지 않습니다.");
        }
      }
      return;
    }

    // 상태 전이 + 이벤트 정책(입장,퇴장,재입장 허용)을 같이 검사
    QrCheckLog qrCheckLog = lastLogOpt.get();
    QrCheckStatusCode lastStatus = qrCheckLog.getCheckStatusCode();
    ;

    boolean invalidTransition = switch (lastStatus.getCode()) {
      case "ENTRY" -> QrCheckStatusCode.ENTRY.equals(checkStatus); // ENTRY 후 ENTRY 불가
      case "REENTRY" -> QrCheckStatusCode.REENTRY.equals(checkStatus); // REENTRY 후 REENTRY 불가
      case "EXIT" -> {
        // 퇴장만 허용하고 입장 스캔은 안되는 행사라면 연속 EXIT 허용
        // true -> 연속 EXIT 허용, false -> 연속 EXIT 불가
        boolean exitOnly = !entryPolicy.isCheckInAllowed() && entryPolicy.isCheckOutAllowed();
        yield !exitOnly && QrCheckStatusCode.EXIT.equals(checkStatus);
      }
      default -> false;
    };

    if (invalidTransition) {
      qrLogService.invalidQrLog(qrTicket,
          validateQrActionCode(QrActionCode.INVALID),
          validateQrCheckStatusCode(QrCheckStatusCode.INVALID));
      throw new CustomException(HttpStatus.BAD_REQUEST,
          "잘못된 입퇴장 동작입니다. 마지막 상태: " + lastStatus.getName());
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
        .checkInAllowed(eventDetail.getCheckInAllowed())
        .checkOutAllowed(eventDetail.getCheckOutAllowed())
        .reentryAllowed(eventDetail.getReentryAllowed())
        .build();
  }
}
