package uk.gov.justice.hmiprobation.casesampler.controllers

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.hmiprobation.casesampler.dto.ErrorResponse
import javax.validation.ValidationException

@RestControllerAdvice(basePackages = ["uk.gov.justice.hmipprobation.casesampler.controllers"])
class ControllerAdvice {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException?): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned", e)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(status = (HttpStatus.FORBIDDEN.value())))
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.debug("Bad Request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleValidationException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
    log.debug("Bad Request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = (HttpStatus.BAD_REQUEST.value()), developerMessage = (e.message)))
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = (HttpStatus.INTERNAL_SERVER_ERROR.value()), developerMessage = (e.message)))
  }

  companion object {
    private val log = LoggerFactory.getLogger(ControllerAdvice::class.java)
  }
}
