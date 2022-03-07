package com.example.driver;

import java.util.Map;
import java.util.function.Function;

import com.google.protobuf.ByteString;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
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
class WASMFunction implements AutoCloseable, Function<Message<byte[]>, Message<byte[]>> {

	private WasmLoader wasmLoader = new WasmLoader();

	@Override
	public void close() throws Exception {
		this.wasmLoader.close();
	}

	private SpringMessage message(Message<byte[]> request) {
		SpringMessage.Builder msg = SpringMessage.newBuilder();
		MessageHeaders headers = request.getHeaders();
		for (String key : headers.keySet()) {
			msg.putHeaders(key, headers.get(key).toString());
		}
		msg.setPayload(ByteString.copyFrom(request.getPayload()));
		return msg.build();
	}

	private Message<byte[]> egassem(SpringMessage input) {
		MessageBuilder<byte[]> msg = MessageBuilder.withPayload(input.getPayload().toByteArray());
		Map<String, String> headers = input.getHeadersMap();
		msg.copyHeaders(headers);
		return msg.build();
	}

	@Override
	public Message<byte[]> apply(Message<byte[]> input) {
		try (WasmRunner runner = wasmLoader.runner(new ClassPathResource("message.wasm"))) {
			return runner == null ? input : egassem(runner.call("filter", message(input), SpringMessage.class));
		}
	}

}