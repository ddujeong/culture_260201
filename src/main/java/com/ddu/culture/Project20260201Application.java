package com.ddu.culture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class Project20260201Application {

	public static void main(String[] args) {
		SpringApplication.run(Project20260201Application.class, args);
	}

}
