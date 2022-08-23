package org.springframework.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.driver.SpringMessage;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

public class WasmLoaderTests {

	private static final byte[] WAT_BYTES_REFLECT = ("(module"
			+ "  (import \"env\" \"debug\" (func (param i32)))"
			+ "  (memory (export \"memory\") 2 3)"
			+ "  (func (export \"reflect\") (param i32) (result i32)"
			+ "    local.get 0"
			+ "    call 0"
			+ "    local.get 0)"
			+ ")").getBytes();

	private static final byte[] WAT_BYTES_MALLOC = ("(module"
			+ "  (memory (export \"memory\") 2 3)"
			+ "  (func (export \"reflect\") (param i32) (result i32)"
			+ "    local.get 0)"
			+ "  (func (export \"malloc\") (param i32) (result i32)"
			+ "    i32.const 16)"
			+ "  (func (export \"free\") (param i32))"
			+ ")").getBytes();

	private static final byte[] WAT_BYTES_MULTI = ("(module"
			+ "  (memory (export \"memory\") 2 3)"
			+ "  (func (export \"echo\") (param i32) (param i32)"
			+ "    local.get 0"
			+ "    local.get 1"
			+ "    i32.load"
			+ "    i32.store"
			+ "    local.get 0"
			+ "    local.get 1"
			+ "    i32.load offset=4"
			+ "    i32.store offset=4"
			+ "  )"
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
	public void testPrecompiledFilter() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "three").build();
				SpringMessage result = runner.call("filter", message, SpringMessage.class);
				assertThat(result.getHeadersMap()).containsKey("one");
			}
		}
	}

	@Test
	public void testSingleValue() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ByteArrayResource(WAT_BYTES_REFLECT))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				int result = runner.call("reflect", message, Integer.class);
				assertThat(result).isEqualTo(0);
			}
		}
	}

	@Test
	public void testMallocFree() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ByteArrayResource(WAT_BYTES_MALLOC))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				int result = runner.call("reflect", message, Integer.class);
				assertThat(result).isEqualTo(16);
			}
		}
	}

	@Test
	public void testTuple() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			// This WASM has an "echo" function that returns a tuple of (pointer, len) so it
			// can be unpacked into a protobuf Message
			try (WasmRunner runner = loader.runner(new ByteArrayResource(WAT_BYTES_MULTI))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two").build();
				SpringMessage result = runner.call("echo", message, SpringMessage.class);
				assertThat(result).isEqualTo(message);
			}
		}
	}

}
