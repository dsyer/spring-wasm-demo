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