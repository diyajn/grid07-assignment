package com.org.grid07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Grid07Application {

	public static void main(String[] args) {
		SpringApplication.run(Grid07Application.class, args);
		System.out.println("Project runs");
	}

}
