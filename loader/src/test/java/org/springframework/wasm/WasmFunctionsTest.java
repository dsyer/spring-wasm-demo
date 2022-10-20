package org.springframework.wasm;

import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmFunctions.Function0;
import io.github.kawamuray.wasmtime.WasmFunctions.Function2;

public class WasmFunctionsTest {
    private static final byte[] WAT_BYTES_ADD = ("(module"
            + "  (func (export \"add\") (param $p1 i32) (param $p2 i32) (result i32)"
            + "    local.get $p1"
            + "    local.get $p2"
            + "    i32.add)"
            + ')').getBytes();

    private static final byte[] WAT_BYTES_TRAMPOLINE = ("(module"
            + "  (func $callback (import \"\" \"callback\") (param i64 i64) (result i64))"
            + "  (func (export \"trampoline\") (param $p1 i64) (param $p2 i64) (result i64)"
            + "    local.get $p1"
            + "    local.get $p2"
            + "    call $callback)"
            + ')').getBytes();

    private static final byte[] WAT_BYTES_ECHO = ("(module"
            + "  (func $get (import \"env\" \"get\") (result i32))"
            + "  (func (export \"echo\") (result i32)"
            + "    call $get)"
            + ')').getBytes();

    @Test
    public void testFunc() {
        try (Store<Void> store = Store.withoutData();
                Engine engine = store.engine();
                Module module = new Module(engine, WAT_BYTES_ADD);
                Instance instance = new Instance(store, module, Collections.emptyList())) {
            try (Func func = instance.getFunc(store, "add").get()) {
                Function2<Integer, Integer, Integer> add = WasmFunctions.func(store, func, I32, I32, I32);
                assertEquals(3, add.call(1, 2).intValue());
            }
        }
    }

    @Test
    public void testWrapFunction() {
        try (Store<Void> store = Store.withoutData();
                Engine engine = store.engine();
                Module module = new Module(engine, WAT_BYTES_TRAMPOLINE);
                Func callback = WasmFunctions.wrap(store, I64, I64, I64, (lhs, rhs) -> lhs + rhs);
                Instance instance = new Instance(store, module, Arrays.asList(Extern.fromFunc(callback)))) {
            try (Func func = instance.getFunc(store, "trampoline").get()) {
                Function2<Long, Long, Long> trampoline = WasmFunctions.func(store, func, I64, I64, I64);
                long sum = trampoline.call(1L, 2L);
                assertEquals(3L, sum);
            }
        }
    }

    @Test
    public void testWrapEcho() {
        try (Store<Void> store = Store.withoutData();
                Engine engine = store.engine();
                Module module = new Module(engine, WAT_BYTES_ECHO);
                Linker linker = new Linker(store.engine())) {
            linker.define("env", "get", Extern.fromFunc(WasmFunctions.wrap(store, I32, () -> 1234)));
            linker.module(store, "", module);
            try (Func func = linker.get(store, "", "echo").get().func()) {
                Function0<Integer> echo = WasmFunctions.func(store, func, I32);
                int sum = echo.call();
                assertEquals(1234, sum);
            }
        }
    }
}
