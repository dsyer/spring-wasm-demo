package com.example.driver;

import java.util.function.Function;

import com.google.protobuf.ByteString;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;

@SpringBootApplication
class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Component
class WASMFunction implements AutoCloseable, Function<SpringMessage, SpringMessage> {

	private WasmLoader wasmLoader = new WasmLoader();

	@Override
	public void close() throws Exception {
		this.wasmLoader.close();
	}

	@Override
	public SpringMessage apply(SpringMessage input) {
		try (WasmRunner runner = wasmLoader.runner(new ClassPathResource("message.wasm"))) {
			return runner == null ? input : runner.call("filter", input, SpringMessage.class);
		}
	}

}

@Component
class SpringMessageConverter implements MessageConverter {

	@Override
	public Object fromMessage(Message<?> request, Class<?> type) {
		byte[] payload = bytes(request.getPayload());
		if (type.isAssignableFrom(SpringMessage.class) && payload != null) {
			SpringMessage.Builder msg = SpringMessage.newBuilder();
			MessageHeaders headers = request.getHeaders();
			for (String key : headers.keySet()) {
				msg.putHeaders(key, headers.get(key).toString());
			}
			msg.setPayload(ByteString.copyFrom(payload));
			return msg.build();
		}
		return null;
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		SpringMessage input = (SpringMessage) payload;
		MessageBuilder<byte[]> msg = MessageBuilder.withPayload(input.getPayload().toByteArray());
		msg.copyHeaders(headers);
		msg.copyHeaders(input.getHeadersMap());
		return msg.build();
	}

	private byte[] bytes(Object payload) {
		if (payload instanceof String) {
			return ((String) payload).getBytes();
		}
		if (payload instanceof byte[]) {
			return (byte[]) payload;
		}
		return null;
	}

}