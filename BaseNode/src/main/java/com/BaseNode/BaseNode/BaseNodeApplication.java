package com.BaseNode.BaseNode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
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

		boolean isDocker = System.getenv("DOCKER") != null;

		if (!isDocker) {
			System.setProperty("java.awt.headless", "false");
			SwingUtilities.invokeLater(() -> {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception ignored) {}

				new BaseNodeLauncher(
						() -> SpringApplication.run(BaseNodeApplication.class, args)
				);
			});
		} else {
			new DockerLauncher(
					() -> SpringApplication.run(BaseNodeApplication.class, args)
			).start();
		}
	}
}