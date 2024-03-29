/* Generated by the protocol buffer compiler.  DO NOT EDIT! */
/* Generated from: cloudevents.proto */

/* Do not generate deprecated warnings for self */
#ifndef PROTOBUF_C__NO_DEPRECATED
#define PROTOBUF_C__NO_DEPRECATED
#endif

#include "cloudevents.pb-c.h"
void   io__cloudevents__v1__cloud_event__attributes_entry__init
                     (Io__Cloudevents__V1__CloudEvent__AttributesEntry         *message)
{
  static const Io__Cloudevents__V1__CloudEvent__AttributesEntry init_value = IO__CLOUDEVENTS__V1__CLOUD_EVENT__ATTRIBUTES_ENTRY__INIT;
  *message = init_value;
}
void   io__cloudevents__v1__cloud_event__cloud_event_attribute_value__init
                     (Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue         *message)
{
  static const Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue init_value = IO__CLOUDEVENTS__V1__CLOUD_EVENT__CLOUD_EVENT_ATTRIBUTE_VALUE__INIT;
  *message = init_value;
}
void   io__cloudevents__v1__cloud_event__init
                     (Io__Cloudevents__V1__CloudEvent         *message)
{
  static const Io__Cloudevents__V1__CloudEvent init_value = IO__CLOUDEVENTS__V1__CLOUD_EVENT__INIT;
  *message = init_value;
}
size_t io__cloudevents__v1__cloud_event__get_packed_size
                     (const Io__Cloudevents__V1__CloudEvent *message)
{
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event__descriptor);
  return protobuf_c_message_get_packed_size ((const ProtobufCMessage*)(message));
}
size_t io__cloudevents__v1__cloud_event__pack
                     (const Io__Cloudevents__V1__CloudEvent *message,
                      uint8_t       *out)
{
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event__descriptor);
  return protobuf_c_message_pack ((const ProtobufCMessage*)message, out);
}
size_t io__cloudevents__v1__cloud_event__pack_to_buffer
                     (const Io__Cloudevents__V1__CloudEvent *message,
                      ProtobufCBuffer *buffer)
{
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event__descriptor);
  return protobuf_c_message_pack_to_buffer ((const ProtobufCMessage*)message, buffer);
}
Io__Cloudevents__V1__CloudEvent *
       io__cloudevents__v1__cloud_event__unpack
                     (ProtobufCAllocator  *allocator,
                      size_t               len,
                      const uint8_t       *data)
{
  return (Io__Cloudevents__V1__CloudEvent *)
     protobuf_c_message_unpack (&io__cloudevents__v1__cloud_event__descriptor,
                                allocator, len, data);
}
void   io__cloudevents__v1__cloud_event__free_unpacked
                     (Io__Cloudevents__V1__CloudEvent *message,
                      ProtobufCAllocator *allocator)
{
  if(!message)
    return;
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event__descriptor);
  protobuf_c_message_free_unpacked ((ProtobufCMessage*)message, allocator);
}
void   io__cloudevents__v1__cloud_event_batch__init
                     (Io__Cloudevents__V1__CloudEventBatch         *message)
{
  static const Io__Cloudevents__V1__CloudEventBatch init_value = IO__CLOUDEVENTS__V1__CLOUD_EVENT_BATCH__INIT;
  *message = init_value;
}
size_t io__cloudevents__v1__cloud_event_batch__get_packed_size
                     (const Io__Cloudevents__V1__CloudEventBatch *message)
{
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event_batch__descriptor);
  return protobuf_c_message_get_packed_size ((const ProtobufCMessage*)(message));
}
size_t io__cloudevents__v1__cloud_event_batch__pack
                     (const Io__Cloudevents__V1__CloudEventBatch *message,
                      uint8_t       *out)
{
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event_batch__descriptor);
  return protobuf_c_message_pack ((const ProtobufCMessage*)message, out);
}
size_t io__cloudevents__v1__cloud_event_batch__pack_to_buffer
                     (const Io__Cloudevents__V1__CloudEventBatch *message,
                      ProtobufCBuffer *buffer)
{
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event_batch__descriptor);
  return protobuf_c_message_pack_to_buffer ((const ProtobufCMessage*)message, buffer);
}
Io__Cloudevents__V1__CloudEventBatch *
       io__cloudevents__v1__cloud_event_batch__unpack
                     (ProtobufCAllocator  *allocator,
                      size_t               len,
                      const uint8_t       *data)
{
  return (Io__Cloudevents__V1__CloudEventBatch *)
     protobuf_c_message_unpack (&io__cloudevents__v1__cloud_event_batch__descriptor,
                                allocator, len, data);
}
void   io__cloudevents__v1__cloud_event_batch__free_unpacked
                     (Io__Cloudevents__V1__CloudEventBatch *message,
                      ProtobufCAllocator *allocator)
{
  if(!message)
    return;
  assert(message->base.descriptor == &io__cloudevents__v1__cloud_event_batch__descriptor);
  protobuf_c_message_free_unpacked ((ProtobufCMessage*)message, allocator);
}
static const ProtobufCFieldDescriptor io__cloudevents__v1__cloud_event__attributes_entry__field_descriptors[2] =
{
  {
    "key",
    1,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    0,   /* quantifier_offset */
    offsetof(Io__Cloudevents__V1__CloudEvent__AttributesEntry, key),
    NULL,
    &protobuf_c_empty_string,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "value",
    2,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_MESSAGE,
    0,   /* quantifier_offset */
    offsetof(Io__Cloudevents__V1__CloudEvent__AttributesEntry, value),
    &io__cloudevents__v1__cloud_event__cloud_event_attribute_value__descriptor,
    NULL,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
};
static const unsigned io__cloudevents__v1__cloud_event__attributes_entry__field_indices_by_name[] = {
  0,   /* field[0] = key */
  1,   /* field[1] = value */
};
static const ProtobufCIntRange io__cloudevents__v1__cloud_event__attributes_entry__number_ranges[1 + 1] =
{
  { 1, 0 },
  { 0, 2 }
};
const ProtobufCMessageDescriptor io__cloudevents__v1__cloud_event__attributes_entry__descriptor =
{
  PROTOBUF_C__MESSAGE_DESCRIPTOR_MAGIC,
  "io.cloudevents.v1.CloudEvent.AttributesEntry",
  "AttributesEntry",
  "Io__Cloudevents__V1__CloudEvent__AttributesEntry",
  "io.cloudevents.v1",
  sizeof(Io__Cloudevents__V1__CloudEvent__AttributesEntry),
  2,
  io__cloudevents__v1__cloud_event__attributes_entry__field_descriptors,
  io__cloudevents__v1__cloud_event__attributes_entry__field_indices_by_name,
  1,  io__cloudevents__v1__cloud_event__attributes_entry__number_ranges,
  (ProtobufCMessageInit) io__cloudevents__v1__cloud_event__attributes_entry__init,
  NULL,NULL,NULL    /* reserved[123] */
};
static const ProtobufCFieldDescriptor io__cloudevents__v1__cloud_event__cloud_event_attribute_value__field_descriptors[7] =
{
  {
    "ce_boolean",
    1,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_BOOL,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_boolean),
    NULL,
    NULL,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "ce_integer",
    2,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_INT32,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_integer),
    NULL,
    NULL,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "ce_string",
    3,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_string),
    NULL,
    &protobuf_c_empty_string,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "ce_bytes",
    4,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_BYTES,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_bytes),
    NULL,
    NULL,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "ce_uri",
    5,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_uri),
    NULL,
    &protobuf_c_empty_string,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "ce_uri_ref",
    6,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_uri_ref),
    NULL,
    &protobuf_c_empty_string,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "ce_timestamp",
    7,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_MESSAGE,
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, attr_case),
    offsetof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue, ce_timestamp),
    &google__protobuf__timestamp__descriptor,
    NULL,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
};
static const unsigned io__cloudevents__v1__cloud_event__cloud_event_attribute_value__field_indices_by_name[] = {
  0,   /* field[0] = ce_boolean */
  3,   /* field[3] = ce_bytes */
  1,   /* field[1] = ce_integer */
  2,   /* field[2] = ce_string */
  6,   /* field[6] = ce_timestamp */
  4,   /* field[4] = ce_uri */
  5,   /* field[5] = ce_uri_ref */
};
static const ProtobufCIntRange io__cloudevents__v1__cloud_event__cloud_event_attribute_value__number_ranges[1 + 1] =
{
  { 1, 0 },
  { 0, 7 }
};
const ProtobufCMessageDescriptor io__cloudevents__v1__cloud_event__cloud_event_attribute_value__descriptor =
{
  PROTOBUF_C__MESSAGE_DESCRIPTOR_MAGIC,
  "io.cloudevents.v1.CloudEvent.CloudEventAttributeValue",
  "CloudEventAttributeValue",
  "Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue",
  "io.cloudevents.v1",
  sizeof(Io__Cloudevents__V1__CloudEvent__CloudEventAttributeValue),
  7,
  io__cloudevents__v1__cloud_event__cloud_event_attribute_value__field_descriptors,
  io__cloudevents__v1__cloud_event__cloud_event_attribute_value__field_indices_by_name,
  1,  io__cloudevents__v1__cloud_event__cloud_event_attribute_value__number_ranges,
  (ProtobufCMessageInit) io__cloudevents__v1__cloud_event__cloud_event_attribute_value__init,
  NULL,NULL,NULL    /* reserved[123] */
};
static const ProtobufCFieldDescriptor io__cloudevents__v1__cloud_event__field_descriptors[8] =
{
  {
    "id",
    1,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    0,   /* quantifier_offset */
    offsetof(Io__Cloudevents__V1__CloudEvent, id),
    NULL,
    &protobuf_c_empty_string,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "source",
    2,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    0,   /* quantifier_offset */
    offsetof(Io__Cloudevents__V1__CloudEvent, source),
    NULL,
    &protobuf_c_empty_string,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "spec_version",
    3,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    0,   /* quantifier_offset */
    offsetof(Io__Cloudevents__V1__CloudEvent, spec_version),
    NULL,
    &protobuf_c_empty_string,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "type",
    4,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    0,   /* quantifier_offset */
    offsetof(Io__Cloudevents__V1__CloudEvent, type),
    NULL,
    &protobuf_c_empty_string,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "attributes",
    5,
    PROTOBUF_C_LABEL_REPEATED,
    PROTOBUF_C_TYPE_MESSAGE,
    offsetof(Io__Cloudevents__V1__CloudEvent, n_attributes),
    offsetof(Io__Cloudevents__V1__CloudEvent, attributes),
    &io__cloudevents__v1__cloud_event__attributes_entry__descriptor,
    NULL,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "binary_data",
    6,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_BYTES,
    offsetof(Io__Cloudevents__V1__CloudEvent, data_case),
    offsetof(Io__Cloudevents__V1__CloudEvent, binary_data),
    NULL,
    NULL,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "text_data",
    7,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_STRING,
    offsetof(Io__Cloudevents__V1__CloudEvent, data_case),
    offsetof(Io__Cloudevents__V1__CloudEvent, text_data),
    NULL,
    &protobuf_c_empty_string,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
  {
    "proto_data",
    8,
    PROTOBUF_C_LABEL_NONE,
    PROTOBUF_C_TYPE_MESSAGE,
    offsetof(Io__Cloudevents__V1__CloudEvent, data_case),
    offsetof(Io__Cloudevents__V1__CloudEvent, proto_data),
    &google__protobuf__any__descriptor,
    NULL,
    0 | PROTOBUF_C_FIELD_FLAG_ONEOF,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
};
static const unsigned io__cloudevents__v1__cloud_event__field_indices_by_name[] = {
  4,   /* field[4] = attributes */
  5,   /* field[5] = binary_data */
  0,   /* field[0] = id */
  7,   /* field[7] = proto_data */
  1,   /* field[1] = source */
  2,   /* field[2] = spec_version */
  6,   /* field[6] = text_data */
  3,   /* field[3] = type */
};
static const ProtobufCIntRange io__cloudevents__v1__cloud_event__number_ranges[1 + 1] =
{
  { 1, 0 },
  { 0, 8 }
};
const ProtobufCMessageDescriptor io__cloudevents__v1__cloud_event__descriptor =
{
  PROTOBUF_C__MESSAGE_DESCRIPTOR_MAGIC,
  "io.cloudevents.v1.CloudEvent",
  "CloudEvent",
  "Io__Cloudevents__V1__CloudEvent",
  "io.cloudevents.v1",
  sizeof(Io__Cloudevents__V1__CloudEvent),
  8,
  io__cloudevents__v1__cloud_event__field_descriptors,
  io__cloudevents__v1__cloud_event__field_indices_by_name,
  1,  io__cloudevents__v1__cloud_event__number_ranges,
  (ProtobufCMessageInit) io__cloudevents__v1__cloud_event__init,
  NULL,NULL,NULL    /* reserved[123] */
};
static const ProtobufCFieldDescriptor io__cloudevents__v1__cloud_event_batch__field_descriptors[1] =
{
  {
    "events",
    1,
    PROTOBUF_C_LABEL_REPEATED,
    PROTOBUF_C_TYPE_MESSAGE,
    offsetof(Io__Cloudevents__V1__CloudEventBatch, n_events),
    offsetof(Io__Cloudevents__V1__CloudEventBatch, events),
    &io__cloudevents__v1__cloud_event__descriptor,
    NULL,
    0,             /* flags */
    0,NULL,NULL    /* reserved1,reserved2, etc */
  },
};
static const unsigned io__cloudevents__v1__cloud_event_batch__field_indices_by_name[] = {
  0,   /* field[0] = events */
};
static const ProtobufCIntRange io__cloudevents__v1__cloud_event_batch__number_ranges[1 + 1] =
{
  { 1, 0 },
  { 0, 1 }
};
const ProtobufCMessageDescriptor io__cloudevents__v1__cloud_event_batch__descriptor =
{
  PROTOBUF_C__MESSAGE_DESCRIPTOR_MAGIC,
  "io.cloudevents.v1.CloudEventBatch",
  "CloudEventBatch",
  "Io__Cloudevents__V1__CloudEventBatch",
  "io.cloudevents.v1",
  sizeof(Io__Cloudevents__V1__CloudEventBatch),
  1,
  io__cloudevents__v1__cloud_event_batch__field_descriptors,
  io__cloudevents__v1__cloud_event_batch__field_indices_by_name,
  1,  io__cloudevents__v1__cloud_event_batch__number_ranges,
  (ProtobufCMessageInit) io__cloudevents__v1__cloud_event_batch__init,
  NULL,NULL,NULL    /* reserved[123] */
};
