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

    result->id = "1-5150"; //msg->id; TODO: see below - decorate by adding an attribute, not modify the id
    result->source = msg->source;
    result->spec_version = msg->spec_version;
    result->type = msg->type;
	result->data_case = msg->data_case;
	result->binary_data = msg->binary_data;
	result->text_data = msg->text_data;
	result->proto_data = msg->proto_data;
	/*
	TODO: Figure out how to add another attribute 'decoratedby: "wasm"' (need to refamiliarize w/ '**' pointer-to-pointer usage.
	Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue newAttrValue = IO__CLOUDEVENTS__V1__CLOUD_EVENT__CLOUD_EVENT_ATTRIBUTE_VALUE__INIT;
	newAttrValue->attr_case = IO__CLOUDEVENTS__V1__CLOUD_EVENT__CLOUD_EVENT_ATTRIBUTE_VALUE__ATTR_CE_STRING;
	newAttrValue->ce_string = "wasm"
	Io__Cloudevents__V1__CloudEvent__AttributesEntry newAttrEntry = IO__CLOUDEVENTS__V1__CLOUD_EVENT__ATTRIBUTES_ENTRY__INIT;
	newAttrEntry->key = "decoratedby";
	newAttrEntry->value = newAttrValue;

	result->n_attributes = (msg->n_attributes)+1;
	result->attributes = msg->attributes;
	*/
	result->n_attributes = msg->n_attributes;
	result->attributes = msg->attributes;

    int len = io__cloudevents__v1__cloud_event__get_packed_size(result);
    uint8_t *buffer = malloc(len);
    io__cloudevents__v1__cloud_event__pack(result, buffer);
    io__cloudevents__v1__cloud_event__free_unpacked(msg, NULL);
    Wrapper output = {
        buffer,
        len};
    return output;
}
