package com.example.driver;

import java.lang.reflect.Method;

import com.google.protobuf.Message;

import org.springframework.util.ReflectionUtils;

import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.Val;

public class WasmRunner implements AutoCloseable {

	private Linker linker;
	private Store<Void> store;

	public WasmRunner(Linker linker, Store<Void> store) {
		this.linker = linker;
		this.store = store;
	}

	@Override
	public void close() {
		if (linker != null) {
			linker.close();
		}
	}

	public <S extends Message, T> T call(String function, S message, Class<T> returnType) {
		var memory = linker.get(store, "", "memory").get().memory();
		var buffer = memory.buffer(store);
		var bytes = message.toByteArray();
		buffer.put(bytes);
		Val[] values = linker.get(store, "", function).get().func().call(store, Val.fromI32(0), Val.fromI32(bytes.length));
		Object obj = values[0].getValue();
		if (returnType == Boolean.class) {
			obj = (int) obj != 0;
		} else if (Message.class.isAssignableFrom(returnType) && values.length>1) {
			int ptr = (int) obj;
			int len = (int) values[1].getValue();
			buffer.position(ptr);
			byte[] output = new byte[len];
			buffer.get(output);
			try {
				@SuppressWarnings("unchecked")
				Class<Message> type = (Class<Message>)returnType;
				Method method = ReflectionUtils.findMethod(type, "parseFrom", byte[].class);
				obj = method.invoke(null, output);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot unpack return from function: " + function + "() at " + ptr, e);
			}
		}
		@SuppressWarnings("unchecked")
		T result = (T) obj;
		buffer.position(0);
		buffer.put(new byte[bytes.length]);
		return result;
	}

}