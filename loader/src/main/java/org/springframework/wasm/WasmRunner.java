package org.springframework.wasm;

import java.nio.ByteBuffer;
import java.util.Optional;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
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

	public <T> T call(String function, CloudEvent input, Class<T> returnType) {
		EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
		var wasmInOutBuffer = byteBuffer();

		// Setup wasm func input (put into byte buffer)
		var inputBytes = format.serialize(input);
		int inputBytesPtr = malloc(inputBytes.length);
		wasmInOutBuffer.position(inputBytesPtr);
		wasmInOutBuffer.put(inputBytes);

		// Invoke wasm function
		Val[] funcOutputValues = linker.get(store, "", function).get()
				.func()
				.call(store, Val.fromI32(inputBytesPtr), Val.fromI32(inputBytes.length));

		// Extract wasm func output values
		Object obj = funcOutputValues[0].getValue();
		if (returnType == Boolean.class) {
			obj = (int) obj != 0;
		}
		else if (CloudEvent.class.isAssignableFrom(returnType) && funcOutputValues.length > 1) {
			try {
				// Retrieve wasm func output (get from byte buffer)
				int outputBytesPtr = (int) obj;
				int outputBytesLen = (int) funcOutputValues[1].getValue();
				byte[] outputBytes = new byte[outputBytesLen];
				wasmInOutBuffer.position(outputBytesPtr);
				wasmInOutBuffer.get(outputBytes);
				obj = format.deserialize(outputBytes);
			} catch (Exception ex) {
				ex.printStackTrace();
				obj = input;
			}
		}
		@SuppressWarnings("unchecked")
		T result = (T) obj;

		// Cleanup after ourselves - clear buffer and free up malloc'ed memory
		wasmInOutBuffer.position(inputBytesPtr);
		wasmInOutBuffer.put(new byte[inputBytes.length]);
		free(inputBytesPtr);

		return result;
	}

	@Override
	public void close() {
		if (linker != null) {
			linker.close();
		}
	}

	private ByteBuffer byteBuffer() {
		var memory = linker.get(store, "", "memory").get().memory();
		return memory.buffer(store);
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
}
