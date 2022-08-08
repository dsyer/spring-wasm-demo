package com.example.driver;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

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

}
