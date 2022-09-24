package com.example.driver;

import java.util.function.Function;

import com.google.protobuf.InvalidProtocolBufferException;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.protobuf.ProtobufFormat;
import io.cloudevents.spring.messaging.CloudEventMessageConverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;

@SpringBootApplication
class FunctionDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(FunctionDemoApplication.class, args);
	}

	private static final String DECORATED_BY_EXTENSION = "decoratedby";

	@Bean
	Function<CloudEvent, CloudEvent> decorateEventWithWasm(WasmLoader wasmLoader) {
		return (input) -> {
			// Convert the incoming CE to proto CE
			io.cloudevents.v1.proto.CloudEvent inputProto;
			try {
				byte[] inputProtoBytes = new ProtobufFormat().serialize(input);
				inputProto = io.cloudevents.v1.proto.CloudEvent.parseFrom(inputProtoBytes);
			}
			catch (InvalidProtocolBufferException ex) {
				throw new RuntimeException("Failed to convert CloudEvent to proto", ex);
			}

			// Invoke WASM with incoming proto CE
			io.cloudevents.v1.proto.CloudEvent outputProto;
			try (WasmRunner runner = wasmLoader.runner(new ClassPathResource("message.wasm"))) {
				Assert.notNull(runner, "Unable to find WASM resource");
				outputProto = runner.call("filter", inputProto, io.cloudevents.v1.proto.CloudEvent.class);
			}

			// Convert to outgoing CE from proto CE
			CloudEvent output = EventFormatProvider
					.getInstance()
					.resolveFormat(ProtobufFormat.PROTO_CONTENT_TYPE)
					.deserialize(outputProto.toByteArray());

			// Decorate the outgoing CE
			String decoratedBy;
			if (output.getExtensionNames().contains(DECORATED_BY_EXTENSION)) {
				decoratedBy = output.getExtension(DECORATED_BY_EXTENSION) + ",function";
			}
			else {
				decoratedBy = "function";
			}

			return CloudEventBuilder.from(output)
					.withExtension(DECORATED_BY_EXTENSION, decoratedBy)
					.build();
		};
	}

	@Bean
	public CloudEventMessageConverter cloudEventMessageConverter() {
		return new CloudEventMessageConverter();
	}

	@Bean
	WasmLoader wasmLoader() {
		return new WasmLoader();
	}
}
