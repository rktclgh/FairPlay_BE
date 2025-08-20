package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.qr.util.CodeValidator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// QR 링크 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class QrLinkService {
  private final CodeGenerator codeGenerator;
  private final CodeValidator codeValidator;

  @Value("${qr.url}")
  private String url;

  public String generateQrLink(QrTicketRequestDto dto){
    String token = codeGenerator.generateQrUrlToken(dto);
    String qrParam = URLEncoder.encode(token, StandardCharsets.UTF_8);
    return url + qrParam;
  }

  public QrTicketRequestDto decodeToDto(String token){
    return codeValidator.decodeToDto(token);
  }
}
