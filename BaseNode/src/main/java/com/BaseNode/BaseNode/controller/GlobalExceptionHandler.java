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

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(jakarta.servlet.http.HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String redirect = (referer != null && !referer.isEmpty()) ? referer : "/";
        String separator = redirect.contains("?") ? "&" : "?";
        String message = "File+exceeds+the+upload+limit";
        return "redirect:" + redirect + separator + "uploadError=" + message;
    }

}
