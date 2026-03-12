package com.BaseNode.BaseNode.config;

import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.model.FileEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import com.BaseNode.BaseNode.repository.FileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    public DataLoader(UserRepository userRepository, FileRepository fileRepository) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            UserEntity admin = new UserEntity("admin", "123456", "ADMIN");
            userRepository.save(admin);
            System.out.println(">>> DB SEEDED: admin user created");
        } else {
            System.out.println(">>> DB already has users, count=" + userRepository.count());
        }
    }
}