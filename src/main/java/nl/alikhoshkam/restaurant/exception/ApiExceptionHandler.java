package nl.alikhoshkam.restaurant.exception;

import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  ResponseEntity<ProblemDetail> status(org.springframework.web.server.ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason() == null ? "Request failed" : e.getReason()));
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> domain(ApiException e) {
    return problem(e.getStatus(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
    return problem(
        HttpStatus.BAD_REQUEST,
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .distinct()
            .sorted()
            .reduce((a, b) -> a + "; " + b)
            .orElse("Invalid input"));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestPartException.class
  })
  ResponseEntity<ProblemDetail> invalid(Exception e) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request or missing file");
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<ProblemDetail> conflict(Exception e) {
    return problem(
        HttpStatus.CONFLICT, "Data already exists, is in use or was modified concurrently");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> forbidden(Exception e) {
    return problem(HttpStatus.FORBIDDEN, "Access to this data is denied");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ProblemDetail> tooLarge(Exception e) {
    return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum file size is 2 MiB");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ProblemDetail> media(Exception e) {
    return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ProblemDetail> missing(Exception e) {
    return problem(HttpStatus.NOT_FOUND, "Endpoint not found");
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ProblemDetail> methodNotAllowed(
      org.springframework.web.HttpRequestMethodNotSupportedException e) {
    var supported = e.getSupportedHttpMethods();
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .allow(supported == null ? new HttpMethod[0] : supported.toArray(HttpMethod[]::new))
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception e) {
    LoggerFactory.getLogger(getClass()).error("Unexpected error", e);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal error; contact the administrator");
  }

  private ResponseEntity<ProblemDetail> problem(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, message));
  }
}
