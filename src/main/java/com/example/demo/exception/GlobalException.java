package com.example.demo.exception;

import com.example.demo.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;


@ControllerAdvice
public class GlobalException {

    // Xử lý tất cả các RuntimeException
    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException exception) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(exception.getMessage());
        apiResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    // Xử lý IllegalArgumentException
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException exception) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(exception.getMessage());
        apiResponse.setCode(HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    // Xử lý AppException
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<String>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    // Xử lý NullPointerException
    @ExceptionHandler(value = NullPointerException.class)
    public ResponseEntity<ApiResponse<String>> handleNullPointerException(NullPointerException exception) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Null value encountered: " + exception.getMessage());
        apiResponse.setCode(HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    // Xử lý DataIntegrityViolationException
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Data integrity violation: " + exception.getMessage());
        apiResponse.setCode(HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
    }
    @ExceptionHandler(value = NoSuchElementException.class)
    public ResponseEntity<ApiResponse<String>> handleNoSuchElementException(NoSuchElementException exception) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Không tìm thấy dữ liệu: " + exception.getMessage());
        apiResponse.setCode(HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

}
