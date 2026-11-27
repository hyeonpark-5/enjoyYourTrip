package com.hyeon.enjoyyourtrip.global.exception;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모든 오류 응답을 RFC 9457 ProblemDetail(application/problem+json) 하나의 형식으로 만든다.
 *
 * <pre>
 * {
 *   "type": "about:blank", "title": "Not Found", "status": 404,
 *   "detail": "회원을 찾을 수 없습니다.", "instance": "/api/v1/members/3",
 *   "code": "MEMBER_NOT_FOUND", "timestamp": "2026-11-27T12:00:00+09:00"
 * }
 * </pre>
 *
 * 입력값 검증 실패(400)에는 필드별 오류 목록 "errors"가 추가된다.
 * Spring MVC 기본 예외(404 경로 없음, 405 등)는 상위 클래스가 처리하고 createResponseEntity에서 code/timestamp만 덧붙인다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";
    private static final String TIMESTAMP = "timestamp";

    private final Clock clock;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException e, HttpServletRequest request) {
        HttpStatus status = statusOf(e);
        if (e instanceof ExternalServiceException) {
            log.warn("외부 API 호출 실패: {}", e.getMessage(), e.getCause());
        }
        return problem(status, e.getCode(), e.getMessage(), request);
    }

    /** 컨트롤러 안의 @PreAuthorize 등 메서드 보안에서 던져진 권한 예외 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.", request);
    }

    /** 보안 필터의 인증 실패(SecurityConfig의 EntryPoint가 이쪽으로 위임) */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException e, HttpServletRequest request) {
        if (e instanceof OAuth2AuthenticationException) {
            // 토큰이 있지만 만료·위조·로그아웃됨 → 클라이언트는 재발급(/auth/reissue)을 시도한다.
            return problem(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않거나 만료된 토큰입니다.", request);
        }
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다.", request);
    }

    /** 쿼리 파라미터가 UTF-8로 인코딩되지 않아 Tomcat이 해석하지 못함 (예: CP949로 보낸 한글) */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ProblemDetail> handleInvalidParameter(InvalidParameterException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ENCODING", "요청 파라미터는 UTF-8로 인코딩해야 합니다.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setDetail("입력값이 올바르지 않습니다.");
        body.setProperty(CODE, "INVALID_INPUT");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** 상위 클래스가 만든 모든 ProblemDetail 응답에 code/timestamp를 채운다. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            Map<String, Object> properties = problemDetail.getProperties();
            if (properties == null || !properties.containsKey(CODE)) {
                problemDetail.setProperty(CODE, defaultCode(statusCode));
            }
            problemDetail.setProperty(TIMESTAMP, now());
            if (problemDetail.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
                problemDetail.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail,
            HttpServletRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty(CODE, code);
        body.setProperty(TIMESTAMP, now());
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus statusOf(BusinessException e) {
        return switch (e) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case UnauthorizedException ignored -> HttpStatus.UNAUTHORIZED;
            case ForbiddenException ignored -> HttpStatus.FORBIDDEN;
            case InvalidValueException ignored -> HttpStatus.BAD_REQUEST;
            case ExternalServiceException ignored -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String defaultCode(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.name() : "HTTP_" + statusCode.value();
    }

    private String now() {
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
