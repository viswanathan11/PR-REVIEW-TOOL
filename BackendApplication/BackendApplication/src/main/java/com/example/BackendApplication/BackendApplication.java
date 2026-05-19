package com.example.BackendApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Load .env file BEFORE Spring starts, so ${GITHUB_CLIENT_ID} etc. resolve
		loadEnvFile();
		SpringApplication.run(BackendApplication.class, args);
		System.out.println("Server has started on http://localhost:8080");
	}

	/**
	 * Reads each line of the .env file and sets them as System properties.
	 * 
	 * Why System properties?
	 *   Spring's ${VARIABLE_NAME} syntax checks:
	 *     1. System properties  ← we load .env here
	 *     2. Environment variables
	 *     3. application.yml defaults
	 * 
	 * In production, you'd use real env vars (Docker/Kubernetes).
	 * The .env approach is a dev-only convenience.
	 */
	private static void loadEnvFile() {
		File envFile = new File(".env");
		if (!envFile.exists()) return;
		try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
			reader.lines()
				.filter(line -> !line.startsWith("#") && !line.isBlank() && line.contains("="))
				.forEach(line -> {
					String[] parts = line.split("=", 2);
					// parts[0] = key (e.g., "GITHUB_CLIENT_ID")
					// parts[1] = value (e.g., "abc123")
					System.setProperty(parts[0].trim(), parts[1].trim());
				});
		} catch (Exception e) {
			System.err.println("Could not load .env: " + e.getMessage());
		}
	}
}
