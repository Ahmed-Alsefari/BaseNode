package com.BaseNode.BaseNode.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface WebController {

    @GetMapping("/login")
    String showLoginPage(Model model);

    @PostMapping("/login")
    String processLogin(
            @RequestParam String username,
            @RequestParam String password,
            Model model,
            HttpSession session
    );
    // adding register path #A
    @GetMapping("/register")
    String showRegisterPage(Model model);

    @PostMapping("/register")
    String processRegister(@RequestParam String username,
                           @RequestParam String password,
                           Model model);

    @GetMapping("/logout")
    String logout(HttpSession session);

    @GetMapping("/")
    String showFileManager(Model model, HttpSession session);
}