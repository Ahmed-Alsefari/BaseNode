package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.request.LoginRequest;
import com.BaseNode.BaseNode.request.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

public interface WebController {

    @GetMapping("/login")
    String showLoginPage(Model model);

    @PostMapping("/login")
    String processLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            Model model,
            HttpSession session
    );
    @GetMapping("/register")
    String showRegisterPage(Model model);

    @PostMapping("/register")
    String processRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model
    );

    @GetMapping("/logout")
    String logout(HttpSession session);

    @GetMapping("/")
    String showFileManager(
            @org.springframework.web.bind.annotation.RequestParam(value = "folderId", required = false) Long folderId,
            Model model,
            HttpSession session);

    @PostMapping("/delete/{id}")
    String deleteFile(@PathVariable Long id, HttpSession session) throws IOException;
}