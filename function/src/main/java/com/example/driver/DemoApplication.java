package com.example.driver;

import java.util.function.Function;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.spring.messaging.CloudEventMessageConverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;

@SpringBootApplication
class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	Function<CloudEvent, CloudEvent> decorateEventWithWasm(WasmLoader wasmLoader) {
		return (input) -> {
			CloudEvent output = null;
			try (WasmRunner runner = wasmLoader.runner(new ClassPathResource("hello-world.wasm"))) {
				Assert.notNull(runner, "Unable to find WASM resource");
				output = runner.call("filter", input, CloudEvent.class);
			}
			return CloudEventBuilder.from(output)
					.withId(output.getId() + "-5150")
					.build();
		};
	}

	@Bean
	CloudEventMessageConverter cloudEventMessageConverter() {
		return new CloudEventMessageConverter();
	}

	@Bean
	WasmLoader wasmLoader() {
		return new WasmLoader();
	}
}
