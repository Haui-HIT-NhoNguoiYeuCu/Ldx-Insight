package io.ldxinsight.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        boolean wantsCsv = false;
        
        // Kiểm tra request path có chứa .csv không
        if (request instanceof ServletWebRequest) {
            String requestPath = ((ServletWebRequest) request).getRequest().getRequestURI();
            if (requestPath != null && (requestPath.contains("/download.csv") || requestPath.contains("/csv"))) {
                wantsCsv = true;
            }
        }
        
        // Kiểm tra Accept header
        if (!wantsCsv) {
            String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
            if (acceptHeader != null) {
                wantsCsv = acceptHeader.contains("text/csv");
            }
        }
        
        if (wantsCsv) {
            // Trả về CSV error message
            String csvError = String.format("error,message\n%d,\"%s\"", 
                    HttpStatus.NOT_FOUND.value(), 
                    ex.getMessage().replace("\"", "\"\""));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                    .body(csvError);
        }
        
        // Trả về JSON error message (mặc định)
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(DuplicateResourceException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}

