package com.example.driver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.ReadBodyRoutePredicateFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;
import org.springframework.web.server.ServerWebExchange;

import com.example.driver.CustomWASMPredicate.Config;
import com.google.protobuf.ByteString;

import reactor.core.publisher.Mono;

@SpringBootApplication
class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
	}

}

@Component
class CustomWASMResponseFilter extends AbstractGatewayFilterFactory<CustomWASMPredicate.Config>
		implements AutoCloseable {

	private WasmLoader wasmLoader;
	private ModifyResponseBodyGatewayFilterFactory filter;

	public CustomWASMResponseFilter(ServerCodecConfigurer codecConfigurer) {
		super(Config.class);
		this.wasmLoader = new WasmLoader();
		this.filter = new ModifyResponseBodyGatewayFilterFactory(codecConfigurer.getReaders(), Collections.emptySet(),
				Collections.emptySet());
	}

	private byte[] response(ServerWebExchange exchange, byte[] body) {
		HttpHeaders headers = (HttpHeaders) exchange.getResponse().getHeaders();
		for (String name : headers.keySet()) {
			exchange.getResponse().getHeaders().put(name, headers.get(name));
		}
		exchange.getResponse().getHeaders().add("one", "two");
		return body;
	}

	@Override
	public GatewayFilter apply(Config config) {
		if (config.isReadBody()) {
			ModifyResponseBodyGatewayFilterFactory.Config bodyConfig = new ModifyResponseBodyGatewayFilterFactory.Config();
			bodyConfig.setRewriteFunction(byte[].class, byte[].class, (exchange, body) -> {
				return Mono.just(response(exchange, body));
			});
			GatewayFilter bodyFilter = filter.apply(bodyConfig);
			return bodyFilter;
		} else {
			return (exchange, chain) -> {
				response(exchange, new byte[0]);
				return chain.filter(exchange);
			};
		}
	}

	@Override
	public void close() throws Exception {
		this.wasmLoader.close();
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("resource", "readBody");
	}

}

@Component
class CustomWASMRequestFilter extends AbstractGatewayFilterFactory<CustomWASMPredicate.Config>
		implements AutoCloseable {

	private WasmLoader wasmLoader;

	public CustomWASMRequestFilter(ServerCodecConfigurer codecConfigurer) {
		super(Config.class);
		this.wasmLoader = new WasmLoader();
	}

	private ServerHttpRequest request(ServerHttpRequest request) {
		return request.mutate().build();
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			return chain.filter(exchange.mutate().request(request(exchange.getRequest())).build());
		};
	}

	@Override
	public void close() throws Exception {
		this.wasmLoader.close();
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("resource", "readBody");
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
			return runner == null ? false : runner.call("predicate", MessageUtils.fromRequest(request.getHeaders(), data), Boolean.class);
		}
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