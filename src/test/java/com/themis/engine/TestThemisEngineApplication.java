package com.themis.engine;

import org.springframework.boot.SpringApplication;

public class TestThemisEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(ThemisEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
