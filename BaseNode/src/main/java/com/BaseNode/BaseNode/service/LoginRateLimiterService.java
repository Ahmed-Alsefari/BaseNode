package com.BaseNode.BaseNode.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Rate limiting service to prevent brute force login attacks #A
@Service
public class LoginRateLimiterService {

    private final Map<String, Integer> attempts =
            new ConcurrentHashMap<>();

    private final Map<String, Long> blockedUntil =
            new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;

    private static final long BLOCK_TIME =
            60_000;

    public boolean isBlocked(String username) {

        Long blockedTime =
                blockedUntil.get(username);

        if (blockedTime == null) {
            return false;
        }

        if (System.currentTimeMillis() > blockedTime) {

            blockedUntil.remove(username);
            attempts.remove(username);

            return false;
        }

        return true;
    }

    public long getRemainingBlockSeconds(
            String username
    ) {

        Long blockedTime =
                blockedUntil.get(username);

        if (blockedTime == null) {
            return 0;
        }

        long remainingMillis =
                blockedTime - System.currentTimeMillis();

        return Math.max(
                remainingMillis / 1000,
                0
        );
    }

    public void recordFailedAttempt(String username) {

        int count =
                attempts.getOrDefault(username, 0) + 1;

        attempts.put(username, count);

        if (count >= MAX_ATTEMPTS) {

            blockedUntil.put(
                    username,
                    System.currentTimeMillis() + BLOCK_TIME
            );
        }
    }

    public void resetAttempts(String username) {

        attempts.remove(username);
        blockedUntil.remove(username);
    }
}
