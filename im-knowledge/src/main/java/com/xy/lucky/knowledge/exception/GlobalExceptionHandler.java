//package com.xy.lucky.knowledge.exception;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.bind.support.WebExchangeBindException;
//import reactor.core.publisher.Mono;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(Exception.class)
//    public Mono<ResponseEntity<Map<String, Object>>> handleException(Exception e) {
//        log.error("Unhandled exception", e);
//        Map<String, Object> response = new HashMap<>();
//        response.put("code", 500);
//        response.put("message", "Internal Server Error: " + e.getMessage());
//        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgumentException(IllegalArgumentException e) {
//        log.warn("Illegal argument: {}", e.getMessage());
//        Map<String, Object> response = new HashMap<>();
//        response.put("code", 400);
//        response.put("message", e.getMessage());
//        return Mono.just(ResponseEntity.badRequest().body(response));
//    }
//
//    @ExceptionHandler(WebExchangeBindException.class)
//    public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(WebExchangeBindException e) {
//        String errors = e.getBindingResult().getFieldErrors().stream()
//                .map(error -> error.getField() + ": " + error.getDefaultMessage())
//                .collect(Collectors.joining(", "));
//        log.warn("Validation error: {}", errors);
//        Map<String, Object> response = new HashMap<>();
//        response.put("code", 400);
//        response.put("message", "Validation failed: " + errors);
//        return Mono.just(ResponseEntity.badRequest().body(response));
//    }
//}
