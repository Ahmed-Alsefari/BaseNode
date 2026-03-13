package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Controller
public class WebControllerImpl implements WebController {

    private static final String SESSION_USER = "loggedInUser";

    private final UserRepository userRepository;
// new object encoder used for password hash #A
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public WebControllerImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public String showLoginPage(Model model) {
        return "login";
    }


    @Override
    public String processLogin(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            String dbPassword = userOpt.get().getPassword();
            // match the password hash with stored db_hash #A
            if (encoder.matches(password, dbPassword)) {
                session.setAttribute(SESSION_USER, username);
                return "redirect:/";
            }
        }
        model.addAttribute("error", "Invalid username or password.");
        return "login";
    }

    @Override
    public String showRegisterPage(Model model) {
        return "register";
    }
// take the name and pass #A
    @Override
    public String processRegister(@RequestParam String username,
                                  @RequestParam String password,
                                  Model model) {
// check if the user is already exists #A
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "register";
        }
// if the user not exists #A
        UserEntity user = new UserEntity();
        user.setUsername(username);
        //encode pass #A
        user.setPassword(encoder.encode(password));
        user.setRole("USER");

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