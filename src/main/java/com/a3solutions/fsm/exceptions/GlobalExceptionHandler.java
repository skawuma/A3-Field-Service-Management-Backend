package com.a3solutions.fsm.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.exceptions
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
@RestControllerAdvice
public class GlobalExceptionHandler {


//    @ExceptionHandler(NotFoundException.class)
//    public ResponseEntity<Object> handleNotFound(NotFoundException ex, HttpServletRequest req) {
//        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
//    }
//
//    @ExceptionHandler(BadRequestException.class)
//    public ResponseEntity<Object> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
//        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
//    }

//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex,
//                                                   HttpServletRequest req) {
//        Map<String, String> errors = new HashMap<>();
//        for (var err : ex.getBindingResult().getAllErrors()) {
//            String field = ((FieldError) err).getField();
//            String msg = err.getDefaultMessage();
//            errors.put(field, msg);
//        }
//        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, req.getRequestURI());
//        body.put("errors", errors);
//        return ResponseEntity.badRequest().body(body);
//    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest req) {
//        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", req.getRequestURI());
//    }

    private ResponseEntity<Object> build(HttpStatus status, String message, String path) {
        Map<String, Object> body = baseBody(status, path);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> baseBody(HttpStatus status, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("path", path);
        return body;
    }


}
