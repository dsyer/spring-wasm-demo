package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;

public class WasmLoaderTests {

	@Test
	public void testPrecompiledMatch() throws Exception {
		System.err.println(new ClassPathResource("message.wasm").getURI());
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

	@Test
	public void testPrecompiledFilter() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				SpringMessage result = runner.call("request", message, SpringMessage.class);
				assertThat(result.getHeadersMap()).containsKey("one");
			}
		}
	}

	@Test
	public void testPrecompiledResponse() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				SpringMessage message = SpringMessage.newBuilder().build();
				SpringMessage result = runner.call("response", message, SpringMessage.class);
				assertThat(result.getHeadersMap()).containsKey("one");
			}
		}
	}

}
