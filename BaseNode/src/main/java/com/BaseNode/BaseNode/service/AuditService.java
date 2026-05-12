package com.BaseNode.BaseNode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger logger =
            LoggerFactory.getLogger(AuditService.class);

    // Centralized audit logging service for security related events #A

    public void logLoginSuccess(String username) {

        logger.info(
                "[LOGIN SUCCESS] User '{}' logged in",
                username
        );
    }

    public void logLoginFailure(String username) {

        logger.warn(
                "[LOGIN FAILED] Invalid login attempt for '{}'",
                username
        );
    }

    public void logRegistration(String username) {

        logger.info(
                "[REGISTER] New user registered '{}'",
                username
        );
    }

    public void logLogout(String username) {

        logger.info(
                "[LOGOUT] User '{}' logged out",
                username
        );
    }

    public void logFileUpload(
            String username,
            String filename
    ) {

        logger.info(
                "[UPLOAD] User '{}' uploaded '{}'",
                username,
                filename
        );
    }

    public void logFileDelete(
            String username,
            String filename
    ) {

        logger.info(
                "[DELETE] User '{}' deleted '{}'",
                username,
                filename
        );
    }
}
