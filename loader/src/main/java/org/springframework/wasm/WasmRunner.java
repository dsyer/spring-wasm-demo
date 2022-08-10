package org.springframework.wasm;

import java.lang.reflect.Method;
import java.util.Optional;

import com.google.protobuf.Message;

import org.springframework.util.ReflectionUtils;

import io.github.kawamuray.wasmtime.Extern;
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

	private int malloc(int size) {
		Optional<Extern> func = linker.get(store, "", "malloc");
		if (func.isEmpty()) {
			// If malloc is not provided we can cross fingers and hope this works
			return 0;
		}
		return (int) func.get().func().call(store, Val.fromI32(size))[0].getValue();
	}

	private void free(int ptr) {
		Optional<Extern> func = linker.get(store, "", "free");
		if (ptr > 0 && func.isPresent()) {
			func.get().func().call(store, Val.fromI32(ptr));
		}
	}

	public <S extends Message, T> T call(String function, S message, Class<T> returnType) {
		var memory = linker.get(store, "", "memory").get().memory();
		var buffer = memory.buffer(store);
		var bytes = message.toByteArray();
		int input = malloc(bytes.length);
		buffer.position(input);
		buffer.put(bytes);
		Val[] values = linker.get(store, "", function).get().func().call(store, Val.fromI32(input),
				Val.fromI32(bytes.length));
		Object obj = values[0].getValue();
		if (returnType == Boolean.class) {
			obj = (int) obj != 0;
		} else if (Message.class.isAssignableFrom(returnType) && values.length > 1) {
			int ptr = (int) obj;
			int len = (int) values[1].getValue();
			buffer.position(ptr);
			byte[] output = new byte[len];
			buffer.get(output);
			try {
				@SuppressWarnings("unchecked")
				Class<Message> type = (Class<Message>) returnType;
				Method method = ReflectionUtils.findMethod(type, "parseFrom", byte[].class);
				obj = method.invoke(null, output);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot unpack return from function: " + function + "() at " + ptr, e);
			}
		}
		@SuppressWarnings("unchecked")
		T result = (T) obj;
		buffer.position(input);
		buffer.put(new byte[bytes.length]);
		free(input);
		return result;
	}

}