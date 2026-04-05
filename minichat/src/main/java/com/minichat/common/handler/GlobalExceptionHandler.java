package com.minichat.common.handler;

import com.minichat.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理业务异常
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<String> handleRuntimeException(RuntimeException e, HttpServletRequest request, HttpServletResponse response) {
        // 确保响应 Content-Type 为 application/json
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        return Result.error(e.getMessage());
    }

    // 处理 @RequestBody + @Validated 场景的参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletResponse response) {
        // 确保响应 Content-Type 为 application/json
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 遍历所有错误，优先找 @NotBlank 类型的错误
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .filter(fieldError -> "NotBlank".equals(fieldError.getCode()))
                .findFirst()
                .map(FieldError::getDefaultMessage)
                // 若没有 @NotBlank 错误，再取第一个错误
                .orElseGet(() -> Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage());
        return Result.error(errorMsg);
    }

    // 处理普通参数（如 @RequestParam）的校验异常
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletResponse response) {
        // 确保响应 Content-Type 为 application/json
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        String errorMsg = e.getConstraintViolations().iterator().next().getMessage();
        return Result.error(errorMsg);
    }
}
