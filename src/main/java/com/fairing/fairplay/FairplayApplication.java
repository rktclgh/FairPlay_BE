package com.fairing.fairplay;

import com.fairing.fairplay.core.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FairplayApplication {

	public static void main(String[] args) {
		EnvLoader.loadEnv();
		SpringApplication.run(FairplayApplication.class, args);
		System.out.println("test1");
	}

}
