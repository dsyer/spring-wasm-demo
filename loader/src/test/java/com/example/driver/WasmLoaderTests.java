package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

public class WasmLoaderTests {

	private static final byte[] WAT_BYTES_REFLECT = ("(module"
			+ "  (memory (export \"memory\") 2 3)"
			+ "  (func (export \"add\") (param i32) (param i32) (result i32)"
			+ "    local.get 0)"
			+ ")").getBytes();

	private static final byte[] WAT_BYTES_MULTI = ("(module"
			+ "  (memory (export \"memory\") 2 3)"
			+ "  (func (export \"echo\") (param i32) (param i32) (result i32) (result i32)"
			+ "    local.get 0"
			+ "    local.get 1)"
			+ ")").getBytes();

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

	@Test
	public void testSingleValue() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ByteArrayResource(WAT_BYTES_REFLECT))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				int result = runner.call("add", message, Integer.class);
				assertThat(result).isEqualTo(0);
			}
		}
	}

	@Test
	public void testTuple() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			// This WASM has an "echo" function that returns a tuple of (pointer, len) so it
			// can be
			// unpacked into a protobuf Message
			try (WasmRunner runner = loader.runner(new ByteArrayResource(WAT_BYTES_MULTI))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				SpringMessage result = runner.call("echo", message, SpringMessage.class);
				assertThat(result).isEqualTo(message);
			}
		}
	}

}
