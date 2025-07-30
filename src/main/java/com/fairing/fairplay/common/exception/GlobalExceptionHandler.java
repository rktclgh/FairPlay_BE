package com.fairing.fairplay.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // 1. IllegalArgumentException - 잘못된 인자 전달
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex){
    return buildErrorResponse(HttpStatus.BAD_REQUEST,ex.getMessage());
  }

  // 2. IllegalStateException - 잘못된 상태에서 호출
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex){
    return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  // 3. AccessDeniedException - 권한 없음
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex){
    return buildErrorResponse(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
  }

  // 4. AuthenticationCredentialsNotFoundException - 인증 예외
  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex){
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
  }

  // 5. MissingServletRequestParameterException - 필수 요청 파라미터 누락
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex){
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "필수 요청 파라미터가 누락되었습니다: " + ex.getParameterName());
  }

  // 6. HttpMessageNotReadableException - 잘못된 JSON 형식. body를 객체로 변환 시 발생
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex){
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "요청 본문의 JSON 형식이 올바르지 않습니다.");
  }

  // 7. MethodArgumentTypeMismatchException - 잘못된 파라미터가 들어왔을 경우
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    String message = String.format("파라미터 '%s'의 타입이 올바르지 않습니다. 기대 타입: %s",
        ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "알 수 없음");

    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  // 8. ConstraintViolationException - Bean Validation 실패 처리
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex) {
    // 여러 검증 실패 메시지를 한 문자열로 합침
    String errors = ex.getConstraintViolations()
        .stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining("; "));
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "입력 값 검증에 실패했습니다: " + errors);
  }

  // 9. HttpRequestMethodNotSupportedException - 지원하지 않는 HTTP 메서드
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    String message = String.format("지원하지 않는 HTTP 메서드입니다: %s", ex.getMethod());
    return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
  }

  // 10. CustomException
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
    String message = ex.getMessage() != null ? ex.getMessage() : "에러가 발생했습니다.";
    return buildErrorResponse(ex.getStatus(), message);
  }


  // 11. HttpClientErrorException - RestTemplate이 4xx 오류를 응답받을 때 사용됨
  @ExceptionHandler(HttpClientErrorException.class)
  public ResponseEntity<ErrorResponse> handleHttpClientError(HttpClientErrorException ex) {
    return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()),
        extractPortOneErrorMessage(ex.getResponseBodyAsString()));
  }

  // 12. HttpServerErrorException - RestTemplate이 5xx 서버 오류 응답받을 때
  @ExceptionHandler(HttpServerErrorException.class)
  public ResponseEntity<ErrorResponse> handleHttpServerError(HttpServerErrorException ex) {
    return buildErrorResponse(HttpStatus.valueOf(ex.getStatusCode().value()),
        "외부 결제 시스템 오류: " + extractPortOneErrorMessage(ex.getResponseBodyAsString()));
  }

  // 그 외 모든 예외 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error("예외가 발생했습니다.", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
  }


  // 공통 응답
  private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(status.value())
        .error(status.getReasonPhrase())
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();

    return ResponseEntity.status(status).body(errorResponse);
  }

  // 포트원 응답 body에서 메세지 파싱
  private String extractPortOneErrorMessage(String responseBody) {
    try {
      JsonNode node = new ObjectMapper().readTree(responseBody);
      if (node.has("message")) {
        JsonNode messageNode = node.get("message");
        return messageNode.isTextual() ? messageNode.asText() : messageNode.toString();
      }
      return "에러 메시지를 찾을 수 없습니다.";
    } catch (Exception e) {
      return "에러 응답 파싱 실패";
    }
  }
}
