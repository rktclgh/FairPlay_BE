package com.fairing.fairplay.scheduler;

import com.fairing.fairplay.qr.service.QrTicketBatchService;
import com.querydsl.core.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrTicketScheduler {

  private final QrTicketBatchService qrTicketBatchService;

  // 매일 오전 6시 티켓 전송
  @Scheduled(cron = "0 0 6 * * *")
  public void runSendQrTicket() {
    List<Tuple> batch = qrTicketBatchService.fetchQrTicketBatch();
    qrTicketBatchService.generateQrLink(batch);
  }
}
