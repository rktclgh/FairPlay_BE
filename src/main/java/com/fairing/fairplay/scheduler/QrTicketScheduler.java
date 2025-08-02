package com.fairing.fairplay.scheduler;


import com.fairing.fairplay.qr.service.QrTicketBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrTicketScheduler {

  private final QrTicketBatchService qrTicketBatchService;

//  // 매일 오전 9시 티켓 전송
//  @Scheduled(cron = "0 0 09 * * *")
//  public void runSendQrTicket(){
//    List<Tuple> batch = qrTicketBatchService.fetchQrTicketBatch();
//    qrTicketBatchService.generateQrLink(batch);
//  }
}
