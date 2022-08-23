#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include "message.pb-c.h"

typedef struct _wrapper
{
    void *data;
    size_t len;
} Wrapper;

void debug(size_t val);

bool predicate(Wrapper input) {
    debug((size_t)&input);
    debug((size_t)input.data);
    debug(input.len);
    SpringMessage *msg = spring_message__unpack(NULL, input.len, input.data);
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

Wrapper filter(Wrapper input)
{
    SpringMessage *msg = spring_message__unpack(NULL, input.len, input.data);
    SpringMessage *result = malloc(sizeof(SpringMessage));
    spring_message__init(result);
    result->payload = msg->payload;
    result->headers = msg->headers;
    result->n_headers = msg->n_headers;
    int len = spring_message__get_packed_size(result);
    void *buffer = malloc(len);
    spring_message__pack(result, buffer);
    spring_message__free_unpacked(msg, NULL);
    Wrapper output = {
        buffer,
        len};
    return output;
}