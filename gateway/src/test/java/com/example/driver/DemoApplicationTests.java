package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void match() {
		webTestClient
			.get().uri("/github/")
			.header("one", "two")
			.exchange()
			.expectStatus().isOk();
	}

	@Test
	void noMatch() {
		webTestClient
			.get().uri("/github/")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody(String.class).value(value -> assertThat(value).isNotEmpty());
	}

}
