package org.springframework.wasm;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;
import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder;

public class WasmLoader implements AutoCloseable {

	private static Log logger = LogFactory.getLog(WasmLoader.class);

	private Engine engine;
	private Store<Void> store;
	private Map<Resource, Module> modules = new ConcurrentHashMap<>();

	public WasmLoader() {
		WasiCtx wasi = new WasiCtxBuilder().inheritStdio().inheritStderr().inheritStdin().build();
		store = Store.withoutData(wasi);
		engine = store.engine();
	}

	public WasmRunner runner(Resource resource) {
		modules.computeIfAbsent(resource,
				key -> {
					try {
						byte[] bytes = StreamUtils.copyToByteArray(key.getInputStream());
						if (bytes[0]==0 && bytes[1] == 97) {
							return Module.fromBinary(engine, bytes);
						}
						return new Module(engine, bytes);
					} catch (IOException e) {
						return null;
					}
				});
		if (modules.get(resource) != null) {
			Module module = modules.get(resource);
			Linker linker = new Linker(store.engine());
			WasiCtx.addToLinker(linker);
			linker.define("env", "debug", Extern.fromFunc(WasmFunctions.wrap(store, WasmValType.I32, val -> logger.debug("wasm: val=" + val))));
			linker.module(store, "", module);
			return new WasmRunner(linker, store);
		}
		return null;
	}

	@Override
	public void close() throws Exception {
		if (engine != null) {
			engine.dispose();
			engine = null;
		}
		if (store != null) {
			store.dispose();
			store = null;
		}
	}

}
