package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.model.FolderEntity;
import com.BaseNode.BaseNode.request.LoginRequest;
import com.BaseNode.BaseNode.request.RegisterRequest;
import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import com.BaseNode.BaseNode.service.FileService;
import com.BaseNode.BaseNode.service.FileSystemWatcherService;
import com.BaseNode.BaseNode.service.FolderService;
import com.BaseNode.BaseNode.service.SecureFileServiceProxy;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.BaseNode.BaseNode.factory.EntityFactory;

@Controller
public class WebControllerImpl implements WebController {

    private static final String SESSION_USER = "loggedInUser";

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileSystemWatcherService watcherService;

    public WebControllerImpl(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", EntityFactory.createLoginRequest());
        return "login";
    }
    // take the name and pass 1# search for the user in the database 2# chack for the password 3# create session for user .... #A
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
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", EntityFactory.createRegisterRequest());
        return "register";
    }

    @Override
    @PostMapping("/register")
    // if validation errors exist #A
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                  BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        // check if the user is already exists #A
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "register";
        }
        // if the user not exists #A
        UserEntity user = EntityFactory.createUser(
                registerRequest.getUsername(),
                //encode pass #A
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
    public String showFileManager(
            @RequestParam(value = "folderId", required = false) Long folderId,
            Model model,
            HttpSession session) {

        FileService secureService = new SecureFileServiceProxy(fileService, session);

        List<FileEntity> fileEntities = (folderId == null)
                ? secureService.getAllFiles().stream()
                .filter(f -> f.getFolderId() == null)
                .toList()
                : secureService.getFilesByFolder(folderId);

        List<FolderEntity> folders = folderService.getFoldersByParent(folderId);

        List<java.util.Map<String, Object>> fileItems = fileEntities.stream().map(f -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", f.getId());
            item.put("fileName", f.getFileName());
            item.put("contentType", f.getContentType());
            item.put("fileSize", formatFileSize(f.getFileSize()));
            item.put("uploadDate", f.getUploadDate());
            return item;
        }).toList();

        model.addAttribute("files", fileItems);
        model.addAttribute("folders", folders);
        model.addAttribute("currentFolderId", folderId);
        model.addAttribute("breadcrumbs", buildBreadcrumbs(folderId));
        return "index";
    }

    private List<BreadcrumbItem> buildBreadcrumbs(Long folderId) {
        List<BreadcrumbItem> crumbs = new ArrayList<>();
        Long current = folderId;

        while (current != null) {
            FolderEntity f = folderService.getFolder(current);
            if (f == null) break;
            crumbs.add(0, new BreadcrumbItem(f.getName(), f.getId()));
            current = f.getParentId();
        }
        return crumbs;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(java.util.Locale.ENGLISH))
                .format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, HttpSession session) throws IOException {
        FileService secureService = new SecureFileServiceProxy(fileService, session);

        FileEntity entity = secureService.getFile(id);
        Long folderId = (entity != null) ? entity.getFolderId() : null;

        secureService.deleteFile(id);

        return (folderId != null) ? "redirect:/?folderId=" + folderId : "redirect:/";
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
    public static class BreadcrumbItem {
        private final String name;
        private final Long folderId;

        public BreadcrumbItem(String name, Long folderId) {
            this.name = name;
            this.folderId = folderId;
        }

        public String getName()    { return name; }
        public Long getFolderId()  { return folderId; }
    }
    @GetMapping("/events")
    public SseEmitter streamEvents(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return watcherService.subscribe();
    }
}
