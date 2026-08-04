package com.themis.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@SpringBootApplication
@EnableCaching
@EnableRetry
public class ThemisEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThemisEngineApplication.class, args);
	}

	@Bean
	public RandomGenerator randomGenerator() {
		return new SecureRandom();
	}
}
