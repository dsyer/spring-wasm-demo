
loader := loader
function := function
gateway := gateway
build := target/build
wasm := message.wasm

ALL: $(gateway) $(function) $(loader)

$(build)/lib/libprotoc.a:
	mkdir -p $(build)
	cd $(build) && curl -L https://github.com/dsyer/protobuf-wasm/releases/download/v3.12.4-0.0.1/protobuf-wasm.tgz | tar -xzf -

$(build)/$(loader)/message.c:
	mkdir -p $(build)/$(loader)
	cp $(loader)/src/test/proto/* $(build)/$(loader)

$(build)/$(loader)/$(wasm): $(build)/$(loader)/message.c $(build)/lib/libprotoc.a 
	mkdir -p $(build)/$(loader)
	cd $(build)/$(loader) && emcc -I ../include -s ERROR_ON_UNDEFINED_SYMBOLS=0 -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_predicate','_filter','_malloc','_free']" -Wl,--no-entry message.c message.pb-c.c ../lib/libprotobuf-c.a ../lib/libprotobuf.a -o message.wasm

$(loader): $(build)/$(loader)/$(wasm)
	cp $(build)/$(loader)/$(wasm) $(loader)/src/test/resources

$(build)/$(function)/message.c:
	mkdir -p $(build)/$(function)
	cp $(function)/src/main/proto/* $(build)/$(function)

$(build)/$(function)/$(wasm): $(build)/$(function)/message.c $(build)/lib/libprotoc.a
	mkdir -p $(build)/$(function)
	cd $(build)/$(function) && emcc -I ../include -s ERROR_ON_UNDEFINED_SYMBOLS=0 -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_filter','_malloc','_free']" -Wl,--no-entry message.c cloudevents.pb-c.c any.pb-c.c timestamp.pb-c.c ../lib/libprotobuf-c.a ../lib/libprotobuf.a -o message.wasm

$(function): $(build)/$(function)/$(wasm)
	cp $(build)/$(function)/$(wasm) $(function)/src/main/resources

$(function)/protoc:
	mkdir -p $(build)/$(function)/protoc
	protoc --proto_path=$(function)/src/main/proto --c_out=$(build)/$(function)/protoc \
	$(function)/src/main/proto/cloudevents.proto $(function)/src/main/proto/any.proto $(function)/src/main/proto/timestamp.proto
	cp $(build)/$(function)/protoc/* $(function)/src/main/proto

$(build)/$(gateway)/message.c:
	mkdir -p $(build)/$(gateway)
	cp $(gateway)/src/main/proto/* $(build)/$(gateway)

$(build)/$(gateway)/$(wasm): $(build)/$(gateway)/message.c $(build)/lib/libprotoc.a 
	mkdir -p $(build)/$(gateway)
	cd $(build)/$(gateway) && emcc -I ../include -s ERROR_ON_UNDEFINED_SYMBOLS=0 -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_predicate','_request','_response','_malloc','_free']" -Wl,--no-entry message.c message.pb-c.c ../lib/libprotobuf-c.a ../lib/libprotobuf.a -o message.wasm

$(gateway): $(build)/$(gateway)/$(wasm)
	cp $(build)/$(gateway)/$(wasm) $(gateway)/src/main/resources

clean:
	rm -rf $(build)//$(loader) $(build)//$(function) $(build)//$(gateway)
