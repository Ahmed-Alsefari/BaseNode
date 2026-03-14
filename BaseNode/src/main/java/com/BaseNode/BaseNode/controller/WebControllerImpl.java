package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.dto.LoginRequest;
import com.BaseNode.BaseNode.dto.RegisterRequest;
import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Controller
public class WebControllerImpl implements WebController {

    private static final String SESSION_USER = "loggedInUser";

    private final UserRepository userRepository;
    // call PasswordEncoder for creation #A
    private final PasswordEncoder encoder;

    public WebControllerImpl(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }


    @Override
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    // take the name and pass 1# search for the user in the database. 2# chack for the password 3# create session for user .... #A
    @Override
    public String processLogin( @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest, BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (userOpt.isPresent()) {
            String dbPassword = userOpt.get().getPassword();
            // match the password hash with stored db_hash #A
            if (encoder.matches(loginRequest.getPassword(), dbPassword)) {
                session.setAttribute(SESSION_USER, loginRequest.getUsername());
                return "redirect:/";
            }
        }
        model.addAttribute("error", "Invalid username or password.");
        return "login";
    }

    @Override
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }
// take the name and pass #A
    @Override
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                  BindingResult bindingResult,
                                  Model model) {
    // if validation errors exist #A
        if (bindingResult.hasErrors()) {
            return "register";
        }
// check if the user is already exists #A
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "register";
        }
// if the user not exists #A
        UserEntity user = new UserEntity(
                registerRequest.getUsername(),
        //encode pass #A
        encoder.encode(registerRequest.getPassword()),
                "USER"
    );


        userRepository.save(user);

        return "redirect:/login";
    }

    @Override
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }


    @Override
    public String showFileManager(Model model, HttpSession session) {

        if (session.getAttribute(SESSION_USER) == null) {
            return "redirect:/login";
        }

        List<FileItem> files = new ArrayList<>();

        files.add(new FileItem("document.pdf", "PDF", "120 KB", "2025-03-10"));
        files.add(new FileItem("photo.jpg", "Image", "2 MB", "2025-03-09"));
        files.add(new FileItem("notes.txt", "Text", "1 KB", "2025-03-08"));
        files.add(new FileItem("projects", "Folder", "-", "2025-03-07"));

        model.addAttribute("files", files);
        model.addAttribute("currentPath", "/");
        model.addAttribute("username", session.getAttribute(SESSION_USER));

        return "index";
    }

    public static class FileItem {

        private final String name;
        private final String type;
        private final String size;
        private final String modified;

        public FileItem(String name, String type, String size, String modified) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.modified = modified;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getSize() {
            return size;
        }

        public String getModified() {
            return modified;
        }
    }

}