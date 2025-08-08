package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.qr.util.CodeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// QR 링크 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class QrLinkService {
  private final CodeGenerator codeGenerator;
  private final CodeValidator codeValidator;

  public String generateQrLink(QrTicketRequestDto dto){
    String token = codeGenerator.generateQrUrlToken(dto);
    return "https://your-site.com/qr-tickets/" + token; // 수정 예정
  }

  public QrTicketRequestDto decodeToDto(String token){
    return codeValidator.decodeToDto(token);
  }
}
