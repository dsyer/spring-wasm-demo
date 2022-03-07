package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

public class WasmLoaderTests {

	@Test
	public void testPrecompiledMatch() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				boolean result = runner.call("predicate", message, Boolean.class);
				assertThat(result).isTrue();
			}
		}
	}

	@Test
	public void testPrecompiledNoMatch() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("two", "three").build();
				boolean result = runner.call("predicate", message, Boolean.class);
				assertThat(result).isFalse();
			}
		}
	}

}
