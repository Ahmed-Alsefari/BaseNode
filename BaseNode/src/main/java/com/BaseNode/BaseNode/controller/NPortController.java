package com.BaseNode.BaseNode.controller;

import com.BaseNode.BaseNode.service.NPortService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nport")
public class NPortController {

    @Autowired
    private NPortService nPortService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start(
            @RequestParam String name,
            @RequestParam(defaultValue = "8080") int port,
            HttpSession session) {

        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(401).body(Map.of("status", "unauthorized"));
        }

        if (name == null || name.isBlank() || !name.matches("^[a-zA-Z0-9-]+$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error",
                            "message", "Server name must contain only letters, numbers, and hyphens"));
        }

        try {
            nPortService.start(name.toLowerCase(), port);
            return ResponseEntity.ok(Map.of("status", "started", "name", name.toLowerCase()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stop(HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(401).body(Map.of("status", "unauthorized"));
        }
        nPortService.stop();
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        String tunnelUrl = nPortService.getTunnelUrl();
        String dbUrl = nPortService.getDbUrl();
        String serverName = nPortService.getCurrentServerName();

        return ResponseEntity.ok(Map.of(
                "running", nPortService.isRunning(),
                "tunnelUrl", tunnelUrl != null ? tunnelUrl : "",
                "dbUrl", dbUrl != null ? dbUrl : "",
                "serverName", serverName != null ? serverName : ""
        ));
    }
}