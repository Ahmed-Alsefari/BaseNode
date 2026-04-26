package com.BaseNode.BaseNode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.*;

@SpringBootApplication
public class BaseNodeApplication {

	public static void main(String[] args) {
		try {

			Path uploadsPath = Paths.get("..", "Uploads").toAbsolutePath().normalize();

			if (!Files.exists(uploadsPath))
				Files.createDirectories(uploadsPath);

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		SpringApplication.run(BaseNodeApplication.class, args);
		System.out.println("\n" +
				"╔════════════════════════════════════════════════════╗\n" +
				"║               ~ BaseNode Started ~                 ║\n" +
				"║----------------------------------------------------║\n" +
				"║   >>> Local: http://localhost:8080                 ║\n" +
				"║   >>> Local DB: http://localhost:8080/h2-console   ║\n" +
				"╚════════════════════════════════════════════════════╝\n"
		);

	}

}
