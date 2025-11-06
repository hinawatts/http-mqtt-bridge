package com.hivemq.httpmqttbridge.common.web;

import com.hivemq.httpmqttbridge.exception.BrokerNotFoundException;
import com.hivemq.httpmqttbridge.exception.MqttPublishInputException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice(basePackages = {"com.hivemq.httpmqttbridge.publisher",
        "com.hivemq.httpmqttbridge.brokerconfig"})
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
						(existing, replacement) -> existing));

		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error("Validation Failed")
				.message("Request validation failed")
				.errors(fieldErrors)
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		Map<String, String> violations = ex.getConstraintViolations()
				.stream()
				.collect(Collectors.toMap(
						cv -> cv.getPropertyPath().toString(),
						cv -> cv.getMessage(),
						(existing, replacement) -> existing));

		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error("Constraint Violation")
				.message("Validation failed for parameters")
				.errors(violations)
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
		ex.getMostSpecificCause();
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.error("Malformed JSON")
				.message(ex.getMostSpecificCause().getMessage())
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler({NoSuchElementException.class})
	public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.NOT_FOUND.value())
				.error("Not Found")
				.message(ex.getMessage() != null ? ex.getMessage() : "Resource not found")
				.build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler({Exception.class, RuntimeException.class})
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error(ex.getMessage(), ex);
		ErrorResponse body = ErrorResponse.builder()
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error("Internal Server Error")
				.message("An unexpected error occurred - " + ex.getMessage())
				.build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

    @ExceptionHandler(BrokerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBrokerNotFound(Exception ex) {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Invalid Broker Id")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MqttPublishInputException.class)
    public ResponseEntity<ErrorResponse> handleMqttPublishInputException(Exception ex) {
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Mqtt Publish Input Error")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
