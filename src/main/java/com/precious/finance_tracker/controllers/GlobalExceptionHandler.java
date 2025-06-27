package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.error.ExceptionResponseDto;
import com.precious.finance_tracker.dtos.error.ValidationResponseDto;
import com.precious.finance_tracker.exceptions.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HashMap<String, List<ValidationResponseDto>>> handleValidationException(
            MethodArgumentNotValidException e
    ) {
        List<ValidationResponseDto> errorList = e.getBindingResult().getFieldErrors().stream()
                .map(error ->
                        ValidationResponseDto.builder()
                                .field(error.getField())
                                .message(error.getDefaultMessage())
                                .build()
                )
                .toList();

        HashMap<String, List<ValidationResponseDto>> errorResponse = new HashMap<String, List<ValidationResponseDto>>();
        errorResponse.put("errors", errorList);

        return ResponseEntity.status(422).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<HashMap<String, List<ValidationResponseDto>>> handleConstraintViolation(
            ConstraintViolationException e
    ) {
        List<ValidationResponseDto> errorList = e.getConstraintViolations().stream()
                .map(error ->
                        ValidationResponseDto.builder()
                                .field(error.getPropertyPath().toString())
                                .message(error.getMessage())
                                .build()
                ).toList();

        HashMap<String, List<ValidationResponseDto>> errorResponse = new HashMap<String, List<ValidationResponseDto>>();
        errorResponse.put("errors", errorList);

        return ResponseEntity.status(422).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ExceptionResponseDto> handleUnauthorizedException(
            UnauthorizedException e
    ) {
        ExceptionResponseDto errorResponse = ExceptionResponseDto.builder()
                .status("Unauthorized")
                .message(e.getMessage())
                .statusCode(e.getStatusCode())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ExceptionResponseDto> handleBadRequestException(
            BadRequestException e
    ) {
        ExceptionResponseDto errorResponse = ExceptionResponseDto.builder()
                .message(e.getMessage())
                .status("Bad Request")
                .statusCode(e.getStatusCode())
                .build();

        return  ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ExceptionResponseDto> handleForbiddenException(
            ForbiddenException e
    ) {
        ExceptionResponseDto errorResponse = ExceptionResponseDto.builder()
                .message(e.getMessage())
                .status("Forbidden")
                .statusCode(e.getStatusCode())
                .build();

        return  ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ExceptionResponseDto> handleNotFoundException(
            NotFoundException e
    ) {
        ExceptionResponseDto errorResponse = ExceptionResponseDto.builder()
                .message(e.getMessage())
                .status("Not found")
                .statusCode(e.getStatusCode())
                .build();

        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(InternalServerError.class)
    public ResponseEntity<ExceptionResponseDto> handleInternalServerError(InternalServerError e) {
        ExceptionResponseDto errorResponse = ExceptionResponseDto.builder()
                .status("Internal Server Error")
                .message(e.getMessage())
                .statusCode(e.getStatusCode())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponseDto> handleGeneric(Exception e) {
        String message = e.getMessage().split("\\R", 2)[0];

        if (message.contains("duplicate key") && e.getMessage().contains("email")) {
            message = "Email already exists.";
        }

        ExceptionResponseDto errorResponse = ExceptionResponseDto.builder()
                .status("Internal Server Error")
                .message(message)
                .statusCode(500)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
