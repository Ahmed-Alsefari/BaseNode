package com.BaseNode.BaseNode;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DockerLauncher {

    private static final int    APP_PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + APP_PORT;

    private final Runnable springStarter;
    private Process nportProcess;

    public DockerLauncher(Runnable springStarter) {
        this.springStarter = springStarter;
    }

    public void start() {
        System.out.println("[BaseNode] Running in Docker mode");

        new Thread(() -> {
            try {
                // 1. Start Spring Boot
                new Thread(springStarter, "spring-boot").start();

                System.out.println("[BaseNode] Waiting for Spring Boot...");
                waitForSpring();
                System.out.println("[BaseNode] Spring Boot is up");

                // 2. Start NPort
                startNPort();

            } catch (Exception ex) {
                System.err.println("[BaseNode] Startup failed: " + ex.getMessage());
            }
        }, "docker-starter").start();
    }

    private void waitForSpring() throws Exception {
        for (int i = 0; i < 90; i++) {
            try {
                HttpURLConnection c = (HttpURLConnection)
                        new URL(BASE_URL + "/login").openConnection();
                c.setConnectTimeout(1000);
                c.setReadTimeout(1000);
                c.connect();
                if (c.getResponseCode() > 0) return;
            } catch (Exception ignored) {}
            Thread.sleep(1000);
        }
        throw new RuntimeException("Spring Boot did not respond within 90 s.");
    }

    private void startNPort() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String name = "basenode";

        while (true) {
            System.out.println("[BaseNode] Trying subdomain: " + name);

            ProcessBuilder pb = new ProcessBuilder("nport",
                    String.valueOf(APP_PORT), "-s", name);
            pb.redirectErrorStream(true);
            nportProcess = pb.start();

            boolean taken = false;

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(nportProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[NPort] " + line);

                    if (line.contains("already in use") || line.contains("Failed to connect")) {
                        taken = true;
                        nportProcess.destroyForcibly();
                        break;
                    }

                    for (String part : line.split("\\s+")) {
                        if (part.startsWith("https://") && part.contains(".nport.link")) {
                            String tunnelUrl = part.trim();
                            System.out.println("──────────────────────────────");
                            System.out.println("URL web:  " + tunnelUrl);
                            System.out.println("DB web:   " + tunnelUrl + "/h2-console");
                            System.out.println("──────────────────────────────");
                        }
                    }
                }
            }

            if (!taken) break;

            System.out.println("[NPort] Subdomain \"" + name + "\" is taken. Enter a new name:");
            name = scanner.nextLine().trim().toLowerCase();
            while (!name.matches("[a-z0-9-]+")) {
                System.out.println("[NPort] Invalid name. Use only letters, numbers, and hyphens:");
                name = scanner.nextLine().trim().toLowerCase();
            }
        }
    }
}
