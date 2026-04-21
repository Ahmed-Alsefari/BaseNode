package com.BaseNode.BaseNode.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    // class for handle exception to redirect user to login page #A
    @ExceptionHandler(SecurityException.class)
    public String handleSecurityException() {
        return "redirect:/login";
    }
}
