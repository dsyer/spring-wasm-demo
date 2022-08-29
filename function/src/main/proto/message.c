#include <stdlib.h>
#include "message.pb-c.h"

typedef struct _wrapper
{
    uint8_t *data;
    int len;
} Wrapper;

Wrapper filter(Wrapper input)
{
    SpringMessage *msg = spring_message__unpack(NULL, input.len, input.data);
    SpringMessage *result = malloc(sizeof(SpringMessage));
    spring_message__init(result);
    result->payload = msg->payload;
    result->headers = msg->headers;
    result->n_headers = msg->n_headers;
    int len = spring_message__get_packed_size(result);
    uint8_t *buffer = malloc(len);
    spring_message__pack(result, buffer);
    spring_message__free_unpacked(msg, NULL);
    Wrapper output = {
        buffer,
        len};
    return output;
}

// $ emcc -I ../include -s ERROR_ON_UNDEFINED_SYMBOLS=0 -Os -s STANDALONE_WASM -s EXPORTED_FUNCTIONS="['_filter','_malloc','_free']" -Wl,--no-entry message.c message.pb-c.c ../lib/libprotobuf-c.a ../lib/libprotobuf.a -o message.wasm
