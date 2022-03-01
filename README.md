Spring Cloud Gateway with a custom predicate implemented as a WASM.

Run the app and then send a request (e.g. with curl) to `localhost:8080/github/`. If there is no header with key "one" the predicate does not match and you get a 404. If there is a header called "one" then the request is routed to github. More effort required to make a useful feature:

* There is only one "plugin" in this project and it is hard coded in the custom predicate, but the idea is that it could be parameterized.
* Extend to filters as well as predicates.
* Resource management (prevent leaks and re-use instances of the WASM)
* Pass some configuration down from the JVM into the WASM

The predicate here was written in C and compiled with [emscripten](https://emscripten.org/), but any language (e.g. Rust, Kotlin, Python) and any compiler that creates [WASI](https://github.com/bytecodealliance/wasi) bindings would work with the same JVM integration. It's pretty simple:

```c
#include <stdbool.h>
#include <string.h>
#include "message.pb-c.h"

bool predicate(uint8_t *data, int len) {
    SpringMessage *msg = spring_message__unpack(NULL, len, data);
    SpringMessage__HeadersEntry **headers = msg->headers;
    bool result = false;
    for (int i=0; i<msg->n_headers; i++) {
        if (!strcmp("one", headers[i]->key)) {
            result = true;
            break;
        }
    }
    spring_message__free_unpacked(msg, NULL);
    return result;
}
```

The implementation uses [protobufs](https://developers.google.com/protocol-buffers) to communicate between the JVM and the WASM. This is a pattern that appears in [proxy-wasm](https://github.com/proxy-wasm), a standardization effort for gateway plugins growing out of the implementation in [Envoy](https://www.envoyproxy.io/).

## Playing with JShell

Here's a JShell REPL session that shows how you can play with the WASM module:

```java
jshell> /env -class-path target/wasmtime-0.0.1-SNAPSHOT.jar
jshell> import org.springframework.util.*;
  import org.springframework.core.io.*;
  import io.github.kawamuray.wasmtime.*;
  import io.github.kawamuray.wasmtime.wasi.*;
jshell> WasiCtx wasi = new WasiCtxBuilder().inheritStdio().inheritStderr().inheritStdin().build();
  var store = Store.withoutData(wasi);
  Engine engine = store.engine();
  Linker linker = new Linker(store.engine());
  WasiCtx.addToLinker(linker);
  byte[] wasm = StreamUtils.copyToByteArray(new ClassPathResource("message.wasm").getInputStream());
  var module = io.github.kawamuray.wasmtime.Module.fromBinary(engine, wasm);
  linker.module(store, "", module);
```

## Compiling the WASM

You need WASM-compiled libraries for `protobuf` and `protobuf-c`. Those require some work.

### Building Protobuf

Building `protobuf` for WASM (https://github.com/protocolbuffers/protobuf) using emscripten wasn't too complicated. First let's set up a build area:

```
$ mkdir tmp
$ cd tmp
$ git clone https://github.com/protocolbuffers/protobuf
$ cd protobuf
```

Check the `PROTOBUF_VERSION` in the system:

```
$ grep PROTOBUF_VERSION /usr/include/google/protobuf/stubs/common.h 
#define GOOGLE_PROTOBUF_VERSION 3012004
```

That means `3.12.4` is installed, so we'll grab that and compile it:

```
$ git checkout v3.12.4
$ ./autogen.sh
$ emconfigure ./configure --host=none-none-none
$ emmake make
$ find . -name \*.a
./src/.libs/libprotobuf-lite.a
./src/.libs/libprotobuf.a
./src/.libs/libprotoc.a
```

There are loads of warnings about `LLVM version appears incorrect (seeing "12.0", expected "11.0")` but it seems to work.

### Building Protobuf-c

Checkout and prepare:

```
$ cd ..
$ git clone https://github.com/protobuf-c/protobuf-c
$ cd protobuf-c
```

Building `protobuf-c` is trickier because it has to point back to the `protobuf` build, and also has to be a compatible version (hence the `3.12.4` tag in `protobuf`):

```
$ ./autogen.sh
$ EMMAKEN_CFLAGS=-I../protobuf/src EM_PKG_CONFIG_PATH=../protobuf emconfigure ./configure --host=none-none-none
$ EMMAKEN_CFLAGS='-I../protobuf/src -L../protobuf/src/.libs' emmake make
```

The `make` command above most likely will fail a couple of times, while it tries to run tests. You can't ignore it, but you can work around it. The first time it fails because `protoc-gen-c` is not executable (it's a WASM), but you can copy the system executable with the same name into the same location (and set the executable bit) to move past that by running the same make command again. The second failure is another non-executable WASM used in tests in `t/generated-code2/cxx-generate-packed-data`. You can get a binary executable to swap with that by running `./autogen.sh && ./configure && make` in a fresh clone. Copy the generated executable on top of the WASM and set the executable bit, then make again:

```
$ EMMAKEN_CFLAGS='-I../protobuf/src -L../protobuf/src/.libs' emmake make
$ find . -name \*.a
./protobuf-c/.libs/libprotobuf-c.a
```

### Compiling the Predicate WASM

```
$ mkdir tmp/src
$ cp src/main/proto/* tmp/src
$ cd tmp/src
$ emcc -I ../protobuf-c -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_predicate']" -Wl,--no-entry message.c ../protobuf-c/protobuf-c/.libs/libprotobuf-c.a ../protobuf/src/.libs/libprotobuf.a -o message.wasm)
$ cp message.wasm ../../src/main/resources
```

## Functions Returning Pointers

WASM functions that return pointers are mainly only useful if you know the length of the data they refer to. You oculd make assumptions about null termination, but they would only work with strings, and not even then if the source language didn't have null-termination. So it's better if you can to pack the length into a struct along with the data and return that. This is supported in WASM, e.g. here is a function that simply reflects the input

```wat
(module
  (memory (export "memory") 2 3)
  (func (export "reflect") (param i32) (param i32) (result i32) (result i32)
    local.get 0
    local.get 1)
)
```

Multivalued parameters in C would be structs, and `emcc` supports that with "experimental" features. So a simple echo function with memory allocation for the result might look like this with parameters and returns passed by value:

```c
#include <stdlib.h>
#include <string.h>

typedef struct _buffer {
    size_t *data;
    int len;
} buffer;

buffer echo(buffer input) {
    size_t *output = malloc(input.len);
    memcpy(output, input.data, input.len);
    buffer result = {
        output,
        input.len
    };
    return result;
}
```

It can be compiled to a WASM like this:

```
$ emcc -mmultivalue -Xclang -target-abi -Xclang experimental-mv -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_echo_']" -Wl,--no-entry echo.c -o echo.wasm
```

If you call that function in the JVM you get back an array of `Val` of length 2 - the pointer and the length.