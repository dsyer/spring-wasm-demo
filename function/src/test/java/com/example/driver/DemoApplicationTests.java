package com.example.driver;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationTests {

	@Test
	void match(@Autowired TestRestTemplate restTemplate) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("ce-id", "1");
		headers.set("ce-source", "spring-wasm-function-sample");
		headers.set("ce-type", "com.example.driver.Foo");
		headers.set("ce-specversion", "1.0");
		HttpEntity<Foo> entity = new HttpEntity<>(new Foo("bar-5150"), headers);
		ResponseEntity<Foo> response = restTemplate
				.postForEntity("/decorateEventWithWasm", entity, Foo.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getFirst("ce-id")).isEqualTo("1-5150");
		assertThat(response.getHeaders().getFirst("ce-decoratedby")).isEqualTo("function");
		assertThat(response.getBody()).isEqualTo(new Foo("bar-5150"));
	}
}
