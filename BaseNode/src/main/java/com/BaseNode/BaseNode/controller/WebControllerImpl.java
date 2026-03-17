package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.request.LoginRequest;
import com.BaseNode.BaseNode.request.RegisterRequest;
import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import com.BaseNode.BaseNode.service.FileService;
import com.BaseNode.BaseNode.service.FileSystemWatcherService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import java.io.IOException;
import java.text.DecimalFormatSymbols;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class WebControllerImpl implements WebController {

    private static final String SESSION_USER = "loggedInUser";

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Autowired
    private FileService fileService;

    public WebControllerImpl(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @Override
    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
                               BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (userOpt.isPresent()) {
            String dbPassword = userOpt.get().getPassword();
            if (encoder.matches(loginRequest.getPassword(), dbPassword)) {
                session.setAttribute(SESSION_USER, loginRequest.getUsername());
                return "redirect:/";
            }
        }
        model.addAttribute("error", "Invalid username or password.");
        return "login";
    }

    @Override
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @Override
    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                  BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "register";
        }

        UserEntity user = new UserEntity(
                registerRequest.getUsername(),
                encoder.encode(registerRequest.getPassword()),
                "USER"
        );

        userRepository.save(user);
        return "redirect:/login";
    }

    @Override
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @Override
    @GetMapping("/")
    public String showFileManager(Model model, HttpSession session) {
        if (session.getAttribute(SESSION_USER) == null) {
            return "redirect:/login";
        }

        List<FileEntity> fileEntities = fileService.getAllFiles();
        List<FileItem> files = new ArrayList<>();

        for (FileEntity entity : fileEntities) {
            String size = formatFileSize(entity.getFileSize());
            String modified = entity.getUploadDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm"));
            files.add(new FileItem(
                    entity.getId(),
                    entity.getFileName(),
                    entity.getContentType(),
                    size,
                    modified
            ));
        }

        model.addAttribute("files", files);
        model.addAttribute("currentPath", "/");
        model.addAttribute("username", session.getAttribute(SESSION_USER));

        return "index";
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(java.util.Locale.ENGLISH))
                .format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, HttpSession session) throws IOException {
        if (session.getAttribute(SESSION_USER) == null) {
            return "redirect:/login";
        }
        fileService.deleteFile(id);
        return "redirect:/";
    }

    @Autowired
    private FileSystemWatcherService watcherService;

    @GetMapping("/sse")
    public SseEmitter subscribe() {
        return watcherService.subscribe();
    }

    public static class FileItem {
        private final Long id;
        private final String name;
        private final String type;
        private final String size;
        private final String modified;

        public FileItem(Long id, String name, String type, String size, String modified) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.size = size;
            this.modified = modified;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getSize() { return size; }
        public String getModified() { return modified; }
    }
}