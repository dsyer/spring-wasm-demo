#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include "message.pb-c.h"

typedef struct _wrapper
{
    uint8_t *data;
    int len;
} Wrapper;

bool predicate(Wrapper input)
{
    SpringMessage *msg = spring_message__unpack(NULL, input.len, input.data);
    SpringMessage__HeadersEntry **headers = msg->headers;
    bool result = false;
    for (int i = 0; i < msg->n_headers; i++)
    {
        if (!strcmp("one", headers[i]->key))
        {
            result = true;
            break;
        }
    }
    spring_message__free_unpacked(msg, NULL);
    return result;
}

Wrapper request(Wrapper input)
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

Wrapper response(Wrapper input)
{
    SpringMessage *msg = spring_message__unpack(NULL, input.len, input.data);
    SpringMessage *result = malloc(sizeof(SpringMessage));
    spring_message__init(result);
    result->payload = msg->payload;
    result->headers = malloc(sizeof(SpringMessage__HeadersEntry)*(msg->n_headers + 1));
    for (int i = 0; i<msg->n_headers; i++) {
        result->headers[i] = msg->headers[i];
    }
    SpringMessage__HeadersEntry *header = malloc(sizeof(SpringMessage__HeadersEntry));
    spring_message__headers_entry__init(header);
    header->key = "one";
    header->value = "two";
    result->headers[msg->n_headers] = header;
    result->n_headers = msg->n_headers + 1;
    int len = spring_message__get_packed_size(result);
    uint8_t *buffer = malloc(len);
    spring_message__pack(result, buffer);
    spring_message__free_unpacked(msg, NULL);
    Wrapper output = {
        buffer,
        len};
    return output;
}