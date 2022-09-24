#include <stdlib.h>
#include "cloudevents.pb-c.h"

typedef struct _wrapper
{
    uint8_t *data;
    int len;
} Wrapper;

Wrapper filter(Wrapper input)
{
    Io__Cloudevents__V1__CloudEvent *msg = io__cloudevents__v1__cloud_event__unpack(NULL, input.len, input.data);
    Io__Cloudevents__V1__CloudEvent *result = malloc(sizeof(Io__Cloudevents__V1__CloudEvent));
    io__cloudevents__v1__cloud_event__init(result);

    result->id = msg->id;
    result->source = msg->source;
    result->spec_version = msg->spec_version;
    result->type = msg->type;
	result->data_case = msg->data_case;
	result->binary_data = msg->binary_data;
	result->text_data = msg->text_data;
	result->proto_data = msg->proto_data;
	result->attributes = malloc(sizeof(Io__Cloudevents__V1__CloudEvent__AttributesEntry)*(msg->n_attributes + 1));
	for (int i = 0; i < msg->n_attributes; i++) {
		result->attributes[i] = msg->attributes[i];
	}

	// Decorate msg by adding new attr entry 'decoratedby = wasm'
	Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue *newAttrValue = malloc(sizeof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue));
	io__cloudevents__v1__cloud_event__cloud_event_attribute_value__init(newAttrValue);
	newAttrValue->attr_case = IO__CLOUDEVENTS__V1__CLOUD_EVENT__CLOUD_EVENT_ATTRIBUTE_VALUE__ATTR_CE_STRING;
	newAttrValue->ce_string = "wasm";
	Io__Cloudevents__V1__CloudEvent__AttributesEntry *newAttrEntry = malloc(sizeof(Io__Cloudevents__V1__CloudEvent__AttributesEntry));
	io__cloudevents__v1__cloud_event__attributes_entry__init(newAttrEntry);
	newAttrEntry->key = "decoratedby";
	newAttrEntry->value = newAttrValue;

	result->attributes[msg->n_attributes] = newAttrEntry;
	result->n_attributes = msg->n_attributes + 1;

    int len = io__cloudevents__v1__cloud_event__get_packed_size(result);
    uint8_t *buffer = malloc(len);
    io__cloudevents__v1__cloud_event__pack(result, buffer);
    io__cloudevents__v1__cloud_event__free_unpacked(msg, NULL);
    Wrapper output = {
        buffer,
        len};
    return output;
}
