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

or use the one that is generated by the build Build (`loader/target/generated-test-resources/protobuf/descriptor-sets/proto.desc`) to build an instance dynamically (i.e. without compiling to Java or class files):

```java
jshell> import com.google.protobuf.*;
   import com.google.protobuf.Descriptors.*;
   import com.google.protobuf.DescriptorProtos.*;
jshell> var files = FileDescriptorSet.parseFrom(new FileSystemResource("loader/target/generated-test-resources/protobuf/descriptor-sets/proto.desc").getInputStream());
   var file = files.getFileList().stream().filter(item -> item.getName().equals("message.proto")).findFirst().get();
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
jshell> var dyno = DynamicMessage.newBuilder(desc.findMessageTypeByName("Object"));
jshell> var entry = MapEntry.newDefaultInstance(desc.findMessageTypeByName("Value"), WireFormat.FieldType.STRING, "", WireFormat.FieldType.STRING, "");
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

There is also a `Value` message which is `oneof` anything.

```java
jshell> Value.getDescriptor().toProto()
$217 ==> name: "Value"
field {
  name: "null_value"
  number: 1
  label: LABEL_OPTIONAL
  type: TYPE_ENUM
  type_name: ".google.protobuf.NullValue"
  oneof_index: 0
}
field {
  name: "number_value"
  number: 2
  label: LABEL_OPTIONAL
  type: TYPE_DOUBLE
  oneof_index: 0
}
field {
  name: "string_value"
  number: 3
  label: LABEL_OPTIONAL
  type: TYPE_STRING
  oneof_index: 0
}
field {
  name: "bool_value"
  number: 4
  label: LABEL_OPTIONAL
  type: TYPE_BOOL
  oneof_index: 0
}
field {
  name: "struct_value"
  number: 5
  label: LABEL_OPTIONAL
  type: TYPE_MESSAGE
  type_name: ".google.protobuf.Struct"
  oneof_index: 0
}
field {
  name: "list_value"
  number: 6
  label: LABEL_OPTIONAL
  type: TYPE_MESSAGE
  type_name: ".google.protobuf.ListValue"
  oneof_index: 0
}
oneof_decl {
  name: "kind"
}
```

Example with a string:

```java
jshell> Value.newBuilder().setStringValue("Hello World").build().toByteArray()
$195 ==> byte[13] { 26, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100 }
```

The binary representation can be broken down like this:

```
26,                                                   // index=3, type=2
11,                                                   // length=11
   72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100 // Hello World
```

and in the source code of `Value` we see the magic `26` and the value being read out of a buffer in the other direction:

```java
case 26: {
  java.lang.String s = input.readStringRequireUtf8();
  kindCase_ = 3;
  kind_ = s;
  break;
}
```

Here's a number value:

```java
jshell> Value.newBuilder().setNumberValue(123).build().toByteArray()
$196 ==> byte[9] { 17, 0, 0, 0, 0, 0, -64, 94, 64 }
```

which breaks down as:

```
17,                         // index=2, type=1 (64 bit number)
0, 0, 0, 0, 0, -64, 94, 64  // 123
```

It uses type=1 which is not very space efficient (64 bits for a low integer). In fact it stored the integer as a double which is disappointing because it might be lossy.

But you can use `Value` as a generic map entry value type:

```java
jshell> var object = DescriptorProto.newBuilder().setName("Object").setOptions(MessageOptions.newBuilder().setMapEntry(true)).addField(FieldDescriptorProto.newBuilder().setName("key").setType(FieldDescriptorProto.Type.TYPE_STRING).setNumber(1)).addField(FieldDescriptorProto.newBuilder().setLabel(FieldDescriptorProto.Label.LABEL_REPEATED).setName("value").setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName("Value").setNumber(2)).build();
jshell> var desc = Descriptors.FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("my.proto").addMessageType(object).addMessageType(Value.getDescriptor().toProto()).build(), new Descriptors.FileDescriptor[]{NullValue.getDescriptor().getFile(), Struct.getDescriptor().getFile(), ListValue.getDescriptor().getFile()});
jshell> var dyno = DynamicMessage.newBuilder(desc.findMessageTypeByName("Object"));
   var obj = DynamicMessage.newBuilder(desc.findMessageTypeByName("Object"));
   var entry = MapEntry.newDefaultInstance(desc.findMessageTypeByName("Object"), WireFormat.FieldType.STRING, "", WireFormat.FieldType.MESSAGE, null);
jshell> obj.setField(obj.getDescriptorForType().findFieldByName("key"), "obj")
   .setField(obj.getDescriptorForType().findFieldByName("value"), 
      Arrays.asList(entry.toBuilder().setKey("msg").setValue(Value.newBuilder().setStringValue("Hello World").build()).build()))
jshell> dyno.setField(dyno.getDescriptorForType().findFieldByName("key"), "obj")
   .setField(dyno.getDescriptorForType().findFieldByName("value"), 
      Arrays.asList(entry.toBuilder().setKey("msg").setValue(Value.newBuilder().setStringValue("Hello World").build()).build(),
      entry.toBuilder().setKey("gsm").setValue(obj.build()).build()))
jshell> dyno.build().toByteArray()
$194 ==> byte[63] { 10, 3, 111, 98, 106, 18, 20, 10, 3, 109, 115, 103, 18, 13, 26, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 18, 34, 10, 3, 103, 115, 109, 18, 27, 10, 3, 111, 98, 106, 18, 20, 10, 3, 109, 115, 103, 18, 13, 26, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100 }
```

In fact you can deserialize JSON directly so all that messing about with the custom "Object" type is kind of pointless:

```java
jshell> import com.google.protobuf.util.*;
jshell> var builder = Struct.newBuilder();
   ...>    JsonFormat.parser().merge("{\"obj\":{\"msg\":\"Hello World\",\"obj\":{\"gsm\":\"Bye Bye\"}}}", builder)
jshell> builder.build()
$281 ==> fields {
  key: "obj"
  value {
    struct_value {
      fields {
        key: "msg"
        value {
          string_value: "Hello World"
        }
      }
      fields {
        key: "obj"
        value {
          struct_value {
            fields {
              key: "gsm"
              value {
                string_value: "Bye Bye"
              }
            }
          }
        }
      }
    }
  }
}
jshell> JsonFormat.printer().omittingInsignificantWhitespace().print(builder.build())
$256 ==> "{\"obj\":{\"msg\":\"Hello World\",\"obj\":{\"gsm\":\"Bye Bye\"}}}"
```

There's a bit of a cheat there: the `JsonFormat.Printer` and `JsonFormat.Parser` have special case logic for `Struct` and `Value` and all of their fields types. If you created a custom type with exactly the same protobuf it wouldn't work as smoothly - e.g. you would be stuck with all the intermediate field names you can see in the `toString()` representation ("fields", "struct_value", etc.).

We can take a look at the binary representation as well:

```java
jshell> builder.build().toByteArray()
$282 ==> byte[62] { 10, 60, 10, 3, 111, 98, 106, 18, 53, 42, 51, 10, 20, 10, 3, 109, 115, 103, 18, 13, 26, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 10, 27, 10, 3, 111, 98, 106, 18, 20, 42, 18, 10, 16, 10, 3, 103, 115, 109, 18, 9, 26, 7, 66, 121, 101, 32, 66, 121, 101 }
```

The binary representation breaks down as

```
10, // type=2, index=1 of Struct
60, // length=60
10, // type=2, index=1 of map entry
3, 
    111, 98, 106, 
18, // type=2, index=2 of map entry
53, // length=53
    42, // type=2, index=5 of Value (struct_value)
    51, // length 51
      10, // type=2, index=1 of Struct
      20, // length=20
          10, // type=2, index=1 of map entry
          3, 
            109, 115, 103, 
          18, // type=2, index=2 of map entry
          13, 
            26, // type=2, index=3 of Value (string_value)
            11, 
                72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 
      10, // type=2, index=1 of Struct
      27, 
        10, // type=2, index=1 of map entry
        3, 
            111, 98, 106, 
        18, // type=2, index=2 of map entry
        20, 
            42, // type=2, index=5 of Value (struct_value)
            18, 
              10, 
              16, 
                  10, 
                  3, 
                      103, 115, 109, 
                  18, 
                  9, 
                      26, 
                      7, 
                        66, 121, 101, 32, 66, 121, 101 
```

## Truly Generic Object

The built-in `Value` type is limiting, because of the lack of support for integer fields. Here's a compatible proto definition that adds a few more field types:

```proto
syntax = "proto3";
option java_multiple_files = true;
option java_package = "com.example.driver";

import "google/protobuf/struct.proto";

message GenericMessage {
    map<string, GenericValue> fields = 1;
}

message GenericList {
    repeated GenericValue values = 1;
}

message GenericValue {
    oneof value {
        google.protobuf.NullValue null_value = 1;
        double double_val = 2;
        string string_val = 3;
        bool bool_val = 4;
        GenericMessage message_val = 5;
        GenericList list_value = 6;
        bytes bytes_val = 7;
        float float_val = 8;
        int32 int_val = 9;
        int64 long_val = 10;
    }
}
```

If you create a `GenericMessage` with exactly the same structure as the `Struct` above:

```java
MapEntry<String, GenericValue> entry = MapEntry.newDefaultInstance(GenericValue.getDescriptor(),
    FieldType.STRING, "", FieldType.MESSAGE, null);
var msg = GenericMessage.newBuilder()
    .addRepeatedField(GenericMessage.getDescriptor().findFieldByName("fields"), entry.toBuilder()
        .setKey("str").setValue(GenericValue.newBuilder().setStringVal("Hello World").build()).build())
    .addRepeatedField(GenericMessage.getDescriptor().findFieldByName("fields"), entry.toBuilder()
        .setKey("obj")
        .setValue(
            GenericValue.newBuilder()
                .setMessageVal(GenericMessage.newBuilder().addRepeatedField(
                    GenericMessage.getDescriptor().findFieldByName("fields"),
                    entry.toBuilder()
                        .setKey("str")
                        .setValue(GenericValue.newBuilder().setStringVal("Bye Bye")
                            .build())
                        .build()))
                .build())
        .build())
    .build();
```

Then the binary representation is identical to the `Struct` as long as only field indexes 1-6 are used:

```
10
  20
  10
  3
    115 116 114
  18
  13
    26
    11
      72 101 108 108 111 32 87 111 114 108 100
10
27
  10
  3
    111 98 106
  18
  20
    42
    18
      10
      16
        10
        3
          115 116 114
      18
      9
        26
        7
          66 121 101 32 66 121 101
```

If you don't have the generated types:

```java
jshell> var desc = Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[]{NullValue.getDescriptor().getFile()})
jshell> var dyno = DynamicMessage.newBuilder(desc.findMessageTypeByName("GenericMessage"))
jshell> var obj = DynamicMessage.newBuilder(desc.findMessageTypeByName("GenericValue"))
jshell> var entry = MapEntry.newDefaultInstance(desc.findMessageTypeByName("GenericValue"), WireFormat.FieldType.STRING, "", WireFormat.FieldType.MESSAGE, null);
jshell> dyno
   .addRepeatedField(dyno.getDescriptorForType().findFieldByName("fields"), entry.toBuilder().setKey("str").setValue(obj.setField(obj.getDescriptorForType().findFieldByName("string_val"), "Hello World").build()).build());
   dyno.addRepeatedField(dyno.getDescriptorForType().findFieldByName("fields"), entry.toBuilder().setKey("obj").setValue(DynamicMessage.newBuilder(desc.findMessageTypeByName("GenericValue")).setField(obj.getDescriptorForType().findFieldByName("message_val"), DynamicMessage.newBuilder(dejshell> dyno.addRepeatedField(dyno.getDescriptorForType().findFieldByName("fields"), entry.toBuilder().setKey("obj").setValue(DynamicMessage.newBuilder(desc.findMessageTypeByName("GenericValue")).setField(obj.getDescriptorForType().findFieldByName("message_val"), DynamicMessage.newBuilder(desc.findMessageTypeByName("GenericMessage")).addRepeatedField(dyno.getDescriptorForType().findFieldByName("fields"), entry.toBuilder().setKey("msg").setValue(obj.setField(obj.getDescriptorForType().findFieldByName("string_val"), "Bye Bye").build()).build()).build()).build()).build());
jshell> dyno.build().toByteArray()$322 ==> byte[51] { 10, 20, 10, 3, 115, 116, 114, 18, 13, 26, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 10, 27, 10, 3, 111, 98, 106, 18, 20, 42, 18, 10, 16, 10, 3, 109, 115, 103, 18, 9, 26, 7, 66, 121, 101, 32, 66, 121, 101 }
```