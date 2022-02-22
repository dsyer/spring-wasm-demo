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