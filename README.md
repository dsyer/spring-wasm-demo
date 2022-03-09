Spring Cloud Gateway with a custom predicate implemented as a WASM.

Run the app and then send a request (e.g. with curl) to `localhost:8080/github/`. If there is no header with key "one" the predicate does not match and you get a 404. If there is a header called "one" then the request is routed to github. More effort required to make a useful feature:

* [x] There is only one "plugin" in this project but its location could be parameterized (it's now a resource location in the predicate config).
* [ ] Extend to filters as well as predicates.
* [x] Resource management (prevent leaks and re-use instances of the WASM). Maybe could be improved still, but the things that can be shared and now shared, and everything is disposed.
* [x] Break out WasmLoader into a library JAR
* [x] Add Spring Cloud Function sample
* [ ] Automate build of WASMs
* [ ] Pass some configuration down from the JVM into the WASM
* [ ] See if there is a way to support a subset of [proxy-wasm](https://github.com/proxy-wasm/spec).

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

Here's a JShell REPL session that shows how you can play with the WASM module. Start with `./loader.jsh`:

```java
jshell> import org.springframework.util.*;
  import org.springframework.core.io.*;
  import io.github.kawamuray.wasmtime.*;
  import io.github.kawamuray.wasmtime.wasi.*;
jshell> WasiCtx wasi = new WasiCtxBuilder().inheritStdio().inheritStderr().inheritStdin().build();
  var store = Store.withoutData(wasi);
  Engine engine = store.engine();
  Linker linker = new Linker(store.engine());
  WasiCtx.addToLinker(linker);
  byte[] wasm = StreamUtils.copyToByteArray(new FileSystemResource("loader/src/test/resources/message.wasm").getInputStream());
  var module = io.github.kawamuray.wasmtime.Module.fromBinary(engine, wasm);
  linker.module(store, "", module);
```

## Compiling a WASM

You need WASM-compiled libraries for `protobuf` and `protobuf-c`. Those require some work but we can download pre-packaged binaries:

```
$ mkdir tmp
$ cd tmp
$ curl -L https://github.com/dsyer/protobuf-wasm/releases/download/v3.12.4-0.0.1/protobuf-wasm.tgz | tar -xzvf -
$ cd ..
```

Then you can compile the example WASMs. Start from the root of the sample. For `gateway`.

```
$ mkdir -p tmp/src
$ cp gateway/src/main/proto/* tmp/src
$ cd tmp/src
$ emcc -I ../include -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_predicate']" -Wl,--no-entry message.c message.pb-c.c ../lib/libprotobuf-c.a ../lib/libprotobuf.a -o message.wasm
$ cp message.wasm ../../gateway/src/main/resources
```

and for `function`:

```
$ mkdir -p tmp/src
$ cp function/src/main/proto/* tmp/src
$ cd tmp/src
$ emcc -I ../include -Os -mmultivalue -Xclang -target-abi -Xclang experimental-mv -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_filter']" -Wl,--no-entry message.c message.pb-c.c ../lib/libprotobuf-c.a ../lib/libprotobuf.a -o message.wasm
$ cp message.wasm ../../function/src/main/resources
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
$ emcc -mmultivalue -Xclang -target-abi -Xclang experimental-mv -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_echo']" -Wl,--no-entry echo.c -o echo.wasm
```

If you call that function in the JVM you get back an array of `Val` of length 2 - the pointer and the length.

## Protobufs for Generic Objects

Create a descriptor:

```
$ protoc --proto_path=loader/src/test/proto --descriptor_set_out=loader/target/proto.desc loader/src/test/proto/message.proto 
```

Build an instance dynamically (i.e. without compiling to Java or class files):

```java
jshell> import com.google.protobuf.*;
   import com.google.protobuf.Descriptors.*;
   import com.google.protobuf.DescriptorProtos.*;
jshell> var files = FileDescriptorSet.parseFrom(new FileSystemResource("loader/target/proto.desc").getInputStream());
   var file = files.getFileList().get(0);
   var desc = Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[0]);
   var type = desc.findMessageTypeByName("SpringMessage");
jshell> var dyno = DynamicMessage.newBuilder(type);
jshell> dyno.setField(type.findFieldByName("payload"), ByteString.copyFrom("Hello World".getBytes()));
jshell> var entry = MapEntry.newDefaultInstance(type.findFieldByName("headers").getMessageType(), WireFormat.FieldType.STRING, "", WireFormat.FieldType.STRING, "");
jshell> dyno.setField(type.findFieldByName("headers"),
    Arrays.asList(entry.toBuilder().setKey("one").setValue("two").build(),
    entry.toBuilder().setKey("three").setValue("four").build()));
jshell> var msg = dyno.build()
msg ==> payload: "Hello World"
headers {
  key: "one"
  value: "two"
}
headers {
  key: "three"
  value: "four"
}
```

You can even sythesize the whole description with a bit of work (it gets hard with nested types):

```java
jshell> var proto = DescriptorProto.newBuilder().setName("MyType").addField(FieldDescriptorProto.newBuilder().setName("value").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(1)).build();
jshell> var desc = Descriptors.FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("my.proto").addMessageType(proto).build(), new Descriptors.FileDescriptor[0]);
jshell> var type = desc.findMessageTypeByName("MyType");
jshell> var dyno = DynamicMessage.newBuilder(type);
jshell> dyno.setField(type.findFieldByName("value"), "Hello World");
jshell> var msg = dyno.build()
msg ==> value: "Hello World"
```

A map type (string to string):

```java
jshell> var value = DescriptorProto.newBuilder().setName("Value").setOptions(MessageOptions.newBuilder().setMapEntry(true)).addField(FieldDescriptorProto.newBuilder().setName("key").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(1)).addField(FieldDescriptorProto.newBuilder().setName("value").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(2)).build()
jshell> var desc = Descriptors.FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("my.proto").addMessageType(value).build(), new Descriptors.FileDescriptor[0])
jshell> var type = desc.findMessageTypeByName("Value")
jshell> var dyno = DynamicMessage.newBuilder(type);
jshell> dyno.setField(type.findFieldByName("key"), "msg");
   dyno.setField(type.findFieldByName("value"), "Hello World");
$50 ==> key: "msg"
value: "Hello World"
```

A custom type with a repeated (muliple entries) map field:

```java
jshell> var proto = DescriptorProto.newBuilder().setName("MyType").addField(FieldDescriptorProto.newBuilder().setLabel(FieldDescriptorProto.Label.LABEL_REPEATED).setName("values").setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setNumber(1).setTypeName("Value").build()).build();
jshell> var value = DescriptorProto.newBuilder().setName("Value").setOptions(MessageOptions.newBuilder().setMapEntry(true)).addField(FieldDescriptorProto.newBuilder().setName("key").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(1)).addField(FieldDescriptorProto.newBuilder().setName("value").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(2)).build();
jshell> var desc = Descriptors.FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("my.proto").addMessageType(proto).addMessageType(value).build(), new Descriptors.FileDescriptor[0]);
```

A generic object like a JSON could be an array of polymorphic objects of one of these types, maybe?

```java
jshell> var value = DescriptorProto.newBuilder().setName("Value").setOptions(MessageOptions.newBuilder().setMapEntry(true)).addField(FieldDescriptorProto.newBuilder().setName("key").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(1)).addField(FieldDescriptorProto.newBuilder().setName("value").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(2)).build();
jshell> var object = DescriptorProto.newBuilder().setName("Object").setOptions(MessageOptions.newBuilder().setMapEntry(true)).addField(FieldDescriptorProto.newBuilder().setName("key").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(1)).addField(FieldDescriptorProto.newBuilder().setLabel(FieldDescriptorProto.Label.LABEL_REPEATED).setName("value").setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName("Value").setNumber(2)).build();
jshell> var desc = Descriptors.FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("my.proto").addMessageType(object).addMessageType(value).build(), new Descriptors.FileDescriptor[0]);
jshell> var type = desc.findMessageTypeByName("Value");
jshell> var dyno = DynamicMessage.newBuilder(desc.findMessageTypeByName("Object"));
jshell> var entry = MapEntry.newDefaultInstance(type, WireFormat.FieldType.STRING, "", WireFormat.FieldType.STRING, "");
jshell> dyno.setField(dyno.getDescriptorForType().findFieldByName("key"), "obj")
   .setField(dyno.getDescriptorForType().findFieldByName("value"), 
      Arrays.asList(entry.toBuilder().setKey("msg").setValue("Hello World").build(),
      entry.toBuilder().setKey("gsm").setValue("Bye Bye").build()));
$60 ==> key: "obj"
value {
  key: "gsm"
  value: "Bye Bye"
}
value {
  key: "msg"
  value: "Hello World"
}
jshell> dyno.build().toByteArray()
$61 ==> byte[41] { 10, 3, 111, 98, 106, 18, 18, 10, 3, 109, 115, 103, 18, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 18, 13, 10, 3, 103, 115, 109, 18, 7, 66, 121, 101, 32, 66, 121, 101 }
```

The byte encoding breaks down as

* 10: index=1, type=2 -- (index<<3) | type, 2 is "length delimited"
* 3: length
* `[111, 98, 106]`: "obj"
* 18: index=2, type=2
* 18: length (to the end of "Hello World")
* 10: index=1, type=2
* 3: length
* `[109, 115, 103]`: "msg"
* 18: index=2, type=2
* 11: length
* `[72, 101, ..., 100]`: "Hello World"
* 18: index=2, type=2
* 13: length (to the end of "Bye Bye")
* 10: index=1, type=2
* 3: length
* `[103, 115, 109]`: "gsm"
* 18: index=2, type=2
* 7: length
* `[66, 121, 101, 32, 66, 121, 101]`: "Bye Bye"