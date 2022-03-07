package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;

public class WasmLoaderTests {

	@Test
	public void testPrecompiledMatch() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				SpringMessage message = SpringMessage.newBuilder().putHeaders("one", "two")
						.setPayload(ByteString.copyFrom("Hello World".getBytes())).build();
				SpringMessage result = runner.call("filter", message, SpringMessage.class);
				assertThat(result).isEqualTo(message);
			}
		}
	}

}
