#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include "message.pb-c.h"

bool predicate(uint8_t *data, int len)
{
    SpringMessage *msg = spring_message__unpack(NULL, len, data);
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

uint8_t *filter(uint8_t *data, int len)
{
    SpringMessage *msg = spring_message__unpack(NULL, len, data);
    SpringMessage__HeadersEntry **headers = msg->headers;
    SpringMessage *result = malloc(sizeof(SpringMessage));
    spring_message__init(result);
    result->payload = msg->payload;
    result->headers = msg->headers;
    uint8_t *buffer = malloc(spring_message__get_packed_size(result));
    spring_message__pack(result, buffer);
    spring_message__free_unpacked(msg, NULL);
    return buffer;
}