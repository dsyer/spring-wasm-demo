package com.example.driver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.google.protobuf.ByteString;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.ReadBodyRoutePredicateFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ServerWebExchange;

import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.Val;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;
import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder;
import reactor.core.publisher.Mono;

@SpringBootApplication
class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Component
class CustomWASMPredicate extends AbstractRoutePredicateFactory<CustomWASMPredicate.Config> {

	private ServerCodecConfigurer codecConfigurer;

	public CustomWASMPredicate(ServerCodecConfigurer codecConfigurer) {
		super(Config.class);
		this.codecConfigurer = codecConfigurer;
	}

	private boolean matches(Config config, ServerHttpRequest request, byte[] data) {
		WasiCtx wasi = new WasiCtxBuilder().inheritStdio().inheritStderr().inheritStdin().build();
		var store = Store.withoutData(wasi);
		Engine engine = store.engine();
		Linker linker = new Linker(store.engine());
		WasiCtx.addToLinker(linker);
		try {
			byte[] wasm = StreamUtils.copyToByteArray(new ClassPathResource("message.wasm").getInputStream());
			var module = io.github.kawamuray.wasmtime.Module.fromBinary(engine, wasm);
			linker.module(store, "", module);
			var memory = linker.get(store, "", "memory").get().memory();
			SpringMessage msg = message(request, data);
			var buffer = memory.buffer(store);
			var bytes = msg.toByteArray();
			buffer.put(bytes);
			return (int) linker.get(store, "", "predicate").get().func().call(store, Val.fromI32(0), Val.fromI32(bytes.length))[0].getValue() != 0;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load WASM", e);
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
		return Arrays.asList("readBody");
	}

	public static class Config {
		private boolean readBody = false;

		public boolean isReadBody() {
			return readBody;
		}

		public void setReadBody(boolean readBody) {
			this.readBody = readBody;
		}
	}

}