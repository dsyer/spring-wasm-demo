package org.springframework.wasm;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import org.springframework.util.ReflectionUtils;

import com.google.protobuf.Message;

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

	private int malloc(ByteBuffer buffer, int size) {
		Optional<Extern> func = linker.get(store, "", "malloc");
		if (func.isEmpty()) {
			// If malloc is not provided we can cross fingers and hope this works
			return buffer.position();
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
		var input = new Wrapper(buffer, message.toByteArray());
		input.bytes();
		Object obj = null;
		if (Message.class.isAssignableFrom(returnType)) {
			var output = new Wrapper(buffer);
			linker.get(store, "", function).get().func().call(store, Val.fromI32(output.ptr()),
					Val.fromI32(input.ptr()));
			obj = output.get(returnType);
			output.free();
		} else {
			Val[] values = linker.get(store, "", function).get().func().call(store,
					Val.fromI32(input.ptr()));
			obj = values[0].getValue();
			if (returnType == Boolean.class) {
				obj = (int) obj != 0;
			}
		}
		@SuppressWarnings("unchecked")
		T result = (T) obj;
		input.free();
		return result;
	}

	class Wrapper {

		private static final int SIZE = 8;
		private byte[] bytes = null;
		private ByteBuffer buffer;
		private int data;
		private int ptr;

		public Wrapper(ByteBuffer buffer) {
			this(buffer, null);
		}

		public byte[] bytes() {
			int pos = this.buffer.position();
			try {
				buffer.position(data());
				byte[] bytes = new byte[len()];
				buffer.get(bytes);
				if (this.bytes != null) {
					return this.bytes;
				}
				return bytes;
			} finally {
				this.buffer.position(pos);
			}
		}

		public <T> T get(Class<T> returnType) {
			byte[] bytes = bytes();
			try {
				@SuppressWarnings("unchecked")
				Class<Message> type = (Class<Message>) returnType;
				Method method = ReflectionUtils.findMethod(type, "parseFrom", byte[].class);
				@SuppressWarnings("unchecked")
				T result = (T) method.invoke(null, bytes);
				return result;
			} catch (Exception e) {
				throw new IllegalStateException("Cannot unpack return from function at " + ptr, e);
			}
		}

		public int len() {
			if (bytes != null) {
				return this.bytes.length;
			}
			int pos = this.buffer.position();
			this.buffer.position(this.ptr + 4);
			int result = this.buffer.getInt();
			this.buffer.position(pos);
			return result;
		}

		public int data() {
			if (bytes != null) {
				return this.data;
			}
			int pos = this.buffer.position();
			this.buffer.position(this.ptr);
			int result = this.buffer.getInt();
			this.buffer.position(pos);
			return result;
		}

		public int ptr() {
			return this.ptr;
		}

		public Wrapper(ByteBuffer buffer, byte[] bytes) {
			this.buffer = buffer;
			int pos = this.buffer.position();
			this.bytes = bytes;
			this.ptr = malloc(buffer, SIZE);
			this.buffer.position(this.ptr + SIZE);
			if (bytes != null) {
				this.data = malloc(buffer, bytes.length);
				this.buffer.position(this.data);
				this.buffer.put(bytes);
				this.buffer.position(this.ptr);
				this.buffer.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(new int[] { this.data, bytes.length });
			}
			this.buffer.position(pos);
		}

		public void free() {
			if (bytes != null) {
				this.buffer.position(this.data);
				this.buffer.put(new byte[this.bytes.length]);
				WasmRunner.this.free(this.data);
				bytes = null;
			}
			this.buffer.position(this.ptr);
			this.buffer.put(new byte[SIZE]);
			WasmRunner.this.free(this.ptr);
		}

	}

}