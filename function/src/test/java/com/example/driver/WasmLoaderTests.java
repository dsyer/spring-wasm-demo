package com.example.driver;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;

import io.cloudevents.v1.proto.CloudEvent;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;

public class WasmLoaderTests {

	@Test
	public void testPrecompiledMatch() throws Exception {
		try (WasmLoader loader = new WasmLoader()) {
			try (WasmRunner runner = loader.runner(new ClassPathResource("message.wasm"))) {
				CloudEvent cloudEventIn = CloudEvent.newBuilder()
						.setId("1")
						.setSource("spring-wasm-function-sample")
						.setType("com.example.driver.Foo")
						.setSpecVersion("1.0")
						.setBinaryData(ByteString.copyFrom("{\"value\": \"bar5150\"}".getBytes()))
						.build();
				CloudEvent cloudEventOut = runner.call("filter", cloudEventIn, CloudEvent.class);
				assertThat(cloudEventOut.getId()).isEqualTo("1-5150");
				assertThat(cloudEventOut.getSource()).isEqualTo(cloudEventIn.getSource());
				assertThat(cloudEventOut.getType()).isEqualTo(cloudEventIn.getType());
				assertThat(cloudEventOut.getSpecVersion()).isEqualTo(cloudEventIn.getSpecVersion());
				assertThat(cloudEventOut.getBinaryData()).isEqualTo(cloudEventIn.getBinaryData());
			}
		}
	}

}
