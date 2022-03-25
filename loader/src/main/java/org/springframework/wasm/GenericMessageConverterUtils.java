package org.springframework.wasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.MapEntry;
import com.google.protobuf.NullValue;
import com.google.protobuf.WireFormat.FieldType;

import org.springframework.util.ObjectUtils;

public class GenericMessageConverterUtils {

	public static Map<String, Object> toMap(GenericMessage msg) {

		Map<String, Object> map = new HashMap<>();
		Map<String, GenericValue> fields = msg.getFieldsMap();

		for (String key : fields.keySet()) {
			GenericValue value = fields.get(key);
			Object val = tcartex(value);
			map.put(key, val);
		}

		return map;

	}

	public static GenericMessage fromMap(Map<String, Object> map) {

		GenericMessage.Builder msg = GenericMessage.newBuilder();
		MapEntry<String, GenericValue> entry = MapEntry.newDefaultInstance(GenericValue.getDescriptor(),
				FieldType.STRING, "", FieldType.MESSAGE, null);

		for (String key : map.keySet()) {

			Object val = map.get(key);
			MapEntry.Builder<String, GenericValue> builder = entry.toBuilder().setKey(key);
			builder.setValue(extract(val));

			msg.addRepeatedField(GenericMessage.getDescriptor().findFieldByName("fields"), builder.build());

		}

		return msg.build();
	}

	private static Object tcartex(GenericValue value) {
		Object val = null;
		if (value.hasNullVal()) {
			// do nothing
		} else if (value.hasDoubleVal()) {
			val = value.getDoubleVal();
		} else if (value.hasStringVal()) {
			val = value.getStringVal();
		} else if (value.hasBoolVal()) {
			val = value.getBoolVal();
		} else if (value.hasIntVal()) {
			val = value.getIntVal();
		} else if (value.hasLongVal()) {
			val = value.getLongVal();
		} else if (value.hasBytesVal()) {
			val = value.getBytesVal();
		} else if (value.hasFloatVal()) {
			val = value.getFloatVal();
		} else if (value.hasListVal()) {
			var list = new ArrayList<Object>();
			for (GenericValue item: value.getListVal().getValuesList()) {
				list.add(tcartex(item));
			}
			val = list;
		}  else if (value.hasMessageVal()) {
			val = toMap(value.getMessageVal());
		}
		return val;
	}

	private static GenericValue extract(Object val) {

		if (val == null) {
			return GenericValue.newBuilder().setNullVal(NullValue.NULL_VALUE).build();
		} else if (val instanceof String) {

			String str = (String) val;
			return GenericValue.newBuilder().setStringVal(str).build();

		} else if (val instanceof Integer) {

			Integer sub = (Integer) val;
			return GenericValue.newBuilder().setIntVal(sub).build();

		} else if (val instanceof Long) {

			Long sub = (Long) val;
			return GenericValue.newBuilder().setLongVal(sub).build();

		} else if (val instanceof Boolean) {

			Boolean sub = (Boolean) val;
			return GenericValue.newBuilder().setBoolVal(sub).build();

		} else if (val instanceof Float) {

			Float sub = (Float) val;
			return GenericValue.newBuilder().setFloatVal(sub).build();

		} else if (val instanceof Double) {

			Double sub = (Double) val;
			return GenericValue.newBuilder().setDoubleVal(sub).build();

		} else if (val instanceof byte[]) {

			byte[] sub = (byte[]) val;
			return GenericValue.newBuilder().setBytesVal(ByteString.copyFrom(sub)).build();

		} else if (val instanceof Map) {

			@SuppressWarnings("unchecked")
			Map<String, Object> sub = (Map<String, Object>) val;
			return GenericValue.newBuilder().setMessageVal(fromMap(sub)).build();

		} else if (val instanceof Iterable) {

			Iterable<?> sub = (Iterable<?>) val;
			var list = GenericList.newBuilder();
			for (Object item : sub) {
				list.addValues(extract(item));
			}
			return GenericValue.newBuilder().setListVal(list.build()).build();

		} else if (val.getClass().isArray()) {

			Object[] sub = ObjectUtils.toObjectArray(val);
			var list = GenericList.newBuilder();
			for (Object item : sub) {
				list.addValues(extract(item));
			}
			return GenericValue.newBuilder().setListVal(list.build()).build();

		} 
		throw new IllegalArgumentException("Unsupported type: " + val.getClass());
	}

}
