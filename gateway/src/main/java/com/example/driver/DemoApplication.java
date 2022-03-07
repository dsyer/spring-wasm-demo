package com.example.driver;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.google.protobuf.ByteString;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.ReadBodyRoutePredicateFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@SpringBootApplication
class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Component
class CustomWASMPredicate extends AbstractRoutePredicateFactory<CustomWASMPredicate.Config> implements AutoCloseable {

	private ServerCodecConfigurer codecConfigurer;
	private WasmLoader wasmLoader;

	public CustomWASMPredicate(ServerCodecConfigurer codecConfigurer) {
		super(Config.class);
		this.codecConfigurer = codecConfigurer;
		this.wasmLoader = new WasmLoader();
	}

	@Override
	public void close() throws Exception {
		this.wasmLoader.close();
	}

	private boolean matches(Config config, ServerHttpRequest request, byte[] data) {
		try (WasmRunner runner = wasmLoader.runner(config.getResource())) {
			return runner == null ? false : runner.call("predicate", message(request, data), Boolean.class);
		}
	}

	private SpringMessage message(ServerHttpRequest request, byte[] data) {
		SpringMessage.Builder msg = SpringMessage.newBuilder();
		HttpHeaders headers = request.getHeaders();
		for (String key : headers.keySet()) {
			msg.putHeaders(key, headers.getFirst(key));
		}
		msg.setPayload(ByteString.copyFrom(data));
		return msg.build();
	}

	@Override
	public AsyncPredicate<ServerWebExchange> applyAsync(Config config) {
		if (config.isReadBody()) {
			return exchange -> {
				ReadBodyRoutePredicateFactory factory = new ReadBodyRoutePredicateFactory(codecConfigurer.getReaders());
				ReadBodyRoutePredicateFactory.Config body = factory.newConfig();
				ServerHttpRequest request = exchange.getRequest();
				body.setPredicate(byte[].class, data -> matches(config, request, data));
				return factory.applyAsync(body).apply(exchange);
			};
		} else {
			return exchange -> Mono.just(matches(config, exchange.getRequest(), new byte[0]));
		}
	}

	@Override
	public Predicate<ServerWebExchange> apply(Config config) {
		throw new UnsupportedOperationException("CustomWASMPredicate is only async.");
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("resource", "readBody");
	}

	public static class Config {
		private boolean readBody = false;
		private Resource resource;

		public boolean isReadBody() {
			return readBody;
		}

		public Resource getResource() {
			return resource;
		}

		public void setResource(Resource resource) {
			this.resource = resource;
		}

		public void setReadBody(boolean readBody) {
			this.readBody = readBody;
		}
	}

}