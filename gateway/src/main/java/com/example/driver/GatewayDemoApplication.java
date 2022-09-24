package com.example.driver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.ReadBodyRoutePredicateFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.wasm.WasmLoader;
import org.springframework.wasm.WasmRunner;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;

import com.example.driver.CustomWASMPredicate.Config;

import reactor.core.publisher.Mono;

@SpringBootApplication
class GatewayDemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(GatewayDemoApplication.class, args);
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

	private byte[] response(Config config, ServerWebExchange exchange, byte[] body) {
		try (WasmRunner runner = wasmLoader.runner(config.getResource())) {
			if (runner == null) {
				return body;
			}
			return MessageUtils.toResponse(exchange, runner.call("response",
					MessageUtils.fromResponse(exchange, body), SpringMessage.class));
		}
	}

	@Override
	public GatewayFilter apply(Config config) {
		if (config.isReadBody()) {
			ModifyResponseBodyGatewayFilterFactory.Config bodyConfig = new ModifyResponseBodyGatewayFilterFactory.Config();
			bodyConfig.setRewriteFunction(byte[].class, byte[].class, (exchange, body) -> {
				return Mono.just(response(config, exchange, body));
			});
			GatewayFilter bodyFilter = filter.apply(bodyConfig);
			return bodyFilter;
		} else {
			return (exchange, chain) -> {
				response(config, exchange, new byte[0]);
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

	private static String ATTR = "WASM_CACHED_REQUEST";
	private WasmLoader wasmLoader;
	private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

	public CustomWASMRequestFilter(ServerCodecConfigurer codecConfigurer) {
		super(Config.class);
		this.wasmLoader = new WasmLoader();
	}

	private ServerHttpRequest request(Config config, ServerHttpRequest request, byte[] data) {
		try (WasmRunner runner = wasmLoader.runner(config.getResource())) {
			return runner == null ? request
					: MessageUtils.toRequest(request, runner.call("request",
							MessageUtils.fromRequest(request.getHeaders(), data), SpringMessage.class));
		}
	}

	@Override
	public GatewayFilter apply(Config config) {
		if (config.isReadBody()) {
			return (exchange, chain) -> ServerWebExchangeUtils
					.cacheRequestBodyAndRequest(exchange,
							serverHttpRequest -> ServerRequest.create(exchange, messageReaders).bodyToMono(byte[].class)
									.doOnNext(requestPayload -> exchange.getAttributes()
											.put(ATTR, request(config, serverHttpRequest, requestPayload)))
									.then(Mono.defer(() -> {
										ServerHttpRequest cachedRequest = exchange.getAttribute(ATTR);
										Assert.notNull(cachedRequest, "cached request shouldn't be null");
										exchange.getAttributes().remove(ATTR);
										return chain.filter(exchange.mutate().request(cachedRequest).build());
									})));
		} else {
			return (exchange, chain) -> {
				return chain
						.filter(exchange.mutate().request(request(config, exchange.getRequest(), new byte[0])).build());
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
			return runner == null ? false
					: runner.call("predicate", MessageUtils.fromRequest(request.getHeaders(), data), Boolean.class);
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
