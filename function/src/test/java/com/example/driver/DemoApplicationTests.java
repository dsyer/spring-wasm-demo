package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void match() {
		webTestClient
				.post().uri("/")
				.header("one", "two")
				.bodyValue("Hello World")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().value("one", value -> assertThat(value).isEqualTo("two"));
	}

}
