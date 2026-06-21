package com.themis.engine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Disabled("Requires Docker environment for Testcontainers")
class ThemisEngineApplicationTests {

	@Test
	void contextLoads() {
	}

}
