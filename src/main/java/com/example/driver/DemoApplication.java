package com.example.driver;

import java.io.IOException;
import java.util.function.Predicate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.core.io.ClassPathResource;
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

@SpringBootApplication
class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Component
class CustomWASMPredicate extends AbstractRoutePredicateFactory<CustomWASMPredicate.Config> {

	public CustomWASMPredicate() {
		super(Config.class);
	}

	private boolean matches(Config config, ServerHttpRequest request) {
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
			SpringMessage msg = message(request);
			var buffer = memory.buffer(store);
			var bytes = msg.toByteArray();
			buffer.put(bytes);
			return (int) linker.get(store, "", "predicate").get().func().call(store, Val.fromI32(0), Val.fromI32(bytes.length))[0].getValue() != 0;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load WASM", e);
		}
	}

	private SpringMessage message(ServerHttpRequest request) {
		SpringMessage.Builder msg = SpringMessage.newBuilder();
		for (String key : request.getHeaders().keySet()) {
			msg.putHeaders(key, request.getHeaders().getFirst(key));
		}
		return msg.build();
	}

	@Override
	public Predicate<ServerWebExchange> apply(Config config) {
		return exchange -> {
			ServerHttpRequest request = exchange.getRequest();
			return matches(config, request);
		};
	}

	public static class Config {
	}

}