package com.example.driver;

import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest.Builder;

import com.google.protobuf.ByteString;

public class MessageUtils {

	public static SpringMessage fromRequest(HttpHeaders headers, byte[] data) {
		SpringMessage.Builder msg = SpringMessage.newBuilder();
		for (String key : headers.keySet()) {
			msg.putHeaders(key, headers.getFirst(key));
		}
		msg.setPayload(ByteString.copyFrom(data));
		return msg.build();
	}

	public static ServerHttpRequest toRequest(ServerHttpRequest request, SpringMessage msg) {
		Builder builder = request.mutate();
		for (String key : msg.getHeadersMap().keySet()) {
			String value = msg.getHeadersMap().get(key);
			builder.headers(headers -> {
				headers.put(key, Arrays.asList(value));
			});
		}
		return builder.build();
	}

}
