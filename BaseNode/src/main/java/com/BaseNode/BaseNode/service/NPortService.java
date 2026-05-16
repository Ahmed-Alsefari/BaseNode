package com.BaseNode.BaseNode.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NPortService {

    private Process nportProcess;
    private final AtomicReference<String> tunnelUrl = new AtomicReference<>(null);
    private final AtomicReference<String> currentServerName = new AtomicReference<>(null);

    public synchronized void start(String serverName, int port) throws Exception {
        System.out.println("[NPort] Starting with name=" + serverName + " port=" + port);
        if (nportProcess != null && nportProcess.isAlive()) {
            return;
        }

        tunnelUrl.set(null);
        currentServerName.set(serverName);

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "nport.cmd", String.valueOf(port), "-s", serverName);
        pb.redirectErrorStream(false);
        nportProcess = pb.start();

        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(nportProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[NPort RAW] " + line);
                    if (line.contains(".nport.link")) {
                        for (String part : line.split("\\s+")) {
                            if (part.startsWith("https://") && part.contains(".nport.link")) {
                                tunnelUrl.set(part.trim());
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[NPort] Reader stopped: " + e.getMessage());
            }
        });
        reader.setDaemon(true);
        reader.start();
        Thread errReader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(nportProcess.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[NPort ERR] " + line);
                }
            } catch (Exception e) {}
        });
        errReader.setDaemon(true);
        errReader.start();
    }

    public synchronized void stop() {
        if (nportProcess != null && nportProcess.isAlive()) {
            nportProcess.destroyForcibly();
        }
        nportProcess = null;
        tunnelUrl.set(null);
        currentServerName.set(null);
    }

    public boolean isRunning() {
        return nportProcess != null && nportProcess.isAlive();
    }

    public String getTunnelUrl() {
        return tunnelUrl.get();
    }


    public String getDbUrl() {
        String url = tunnelUrl.get();
        return (url != null) ? url + "/h2-console" : null;
    }

    public String getCurrentServerName() {
        return currentServerName.get();
    }
}