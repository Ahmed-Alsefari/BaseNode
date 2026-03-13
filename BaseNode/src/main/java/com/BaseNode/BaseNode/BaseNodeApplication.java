package com.BaseNode.BaseNode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BaseNodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BaseNodeApplication.class, args);
		System.out.println("\n" +
				"╔═════════════════════════════════════╗\n" +
				"║         BaseNode Started!           ║\n" +
				"║-------------------------------------║\n" +
				"║  >>> Local: http://localhost:8080   ║\n" +
				"╚═════════════════════════════════════╝\n"
		);

	}

}
