package org.springframework.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.NullValue;

import org.junit.jupiter.api.Test;
import org.springframework.util.ObjectUtils;

public class GenericMessageConverterUtilsTests {

	@Test
	void testBuildFromNull() {
		Map<String, Object> map = new HashMap<>();
		map.put("val", null);
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("val").getNullVal()).isEqualTo(NullValue.NULL_VALUE);
		assertThat(GenericMessageConverterUtils.toMap(msg)).isEqualTo(map);
	}

	@Test
	void testBuildFromString() {
		Map<String, Object> map = Map.of("msg", "Hello World");
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("msg").getStringVal()).isEqualTo("Hello World");
		assertThat(GenericMessageConverterUtils.toMap(msg)).isEqualTo(map);
	}

	@Test
	void testBuildFromInt() {
		Map<String, Object> map = Map.of("val", 123);
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("val").getIntVal()).isEqualTo(123);
		assertThat(GenericMessageConverterUtils.toMap(msg)).isEqualTo(map);
	}

	@Test
	void testBuildFromLong() {
		Map<String, Object> map = Map.of("val", 123L);
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		// You can't call toString() on a GenericMessage (seems like a bug)
		// System.err.println(msg);
		assertThat(msg.getFieldsMap().get("val").getIntVal()).isEqualTo(0); // default value
		assertThat(msg.getFieldsMap().get("val").getLongVal()).isEqualTo(123);
		printBytes(msg.toByteArray());
		assertThat(GenericMessageConverterUtils.toMap(msg)).isEqualTo(map);
	}

	@Test
	void testBuildFromStringAndNestedMap() {
		Map<String, Object> map = Map.of("obj", Map.of("msg", "Hello World", "obj", Map.of("gsm", "Bye Bye")));
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("obj").getMessageVal().getFieldsMap().get("msg").getStringVal()).isEqualTo("Hello World");
		assertThat(GenericMessageConverterUtils.toMap(msg)).isEqualTo(map);
	}

	@Test
	void testBuildFromList() {
		Map<String, Object> map = Map.of("val", Arrays.asList("Hello", "World"));
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("val").getListVal().getValuesCount()).isEqualTo(2);
		assertThat(GenericMessageConverterUtils.toMap(msg)).isEqualTo(map);
	}

	@Test
	void testBuildFromArrayOfString() {
		Map<String, Object> map = Map.of("val", new String[]{"Hello", "World"});
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("val").getListVal().getValuesCount()).isEqualTo(2);
		assertThat(msg.getFieldsMap().get("val").getListVal().getValues(0).getStringVal()).isEqualTo("Hello");
		assertThat(GenericMessageConverterUtils.toMap(msg).get("val")).isEqualTo(Arrays.asList((Object[])map.get("val")));
	}

	@Test
	void testBuildFromArrayOfPrimitive() {
		Map<String, Object> map = Map.of("val", new int[]{123, 456});
		GenericMessage msg = GenericMessageConverterUtils.fromMap(map);
		assertThat(msg.getFieldsMap().get("val").getListVal().getValuesCount()).isEqualTo(2);
		assertThat(GenericMessageConverterUtils.toMap(msg).get("val")).isEqualTo(Arrays.asList(ObjectUtils.toObjectArray(map.get("val"))));
	}

	private void printBytes(byte[] bytes) {
		System.err.print("byte[" + bytes.length + "]: {");
		int count = 0;
		for (byte b : bytes) {
			System.err.print(b);
			count++;
			if (count<bytes.length) {
				System.err.print(", ");
			}
		}
		System.err.println("}");
	}

}
