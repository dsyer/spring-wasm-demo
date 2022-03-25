package org.springframework.wasm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import com.example.driver.SpringMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MapEntry;
import com.google.protobuf.WireFormat;
import com.google.protobuf.WireFormat.FieldType;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;

public class ProtobufTests {

	@Test
	void testMap() throws Exception {
		var files = FileDescriptorSet
				.parseFrom(new FileSystemResource("target/generated-test-resources/protobuf/descriptor-sets/proto.desc")
						.getInputStream());
		var file = files.getFileList().stream().filter(item -> item.getName().equals("message.proto")).findFirst()
				.get();
		var desc = Descriptors.FileDescriptor.buildFrom(file, new Descriptors.FileDescriptor[0]);
		var type = desc.findMessageTypeByName("SpringMessage");
		assertThat(type).isNotNull();
		var dyno = DynamicMessage.newBuilder(type);
		dyno.setField(type.findFieldByName("payload"), ByteString.copyFrom("Hello World".getBytes()));
		var entry = MapEntry.newDefaultInstance(type.findFieldByName("headers").getMessageType(),
				WireFormat.FieldType.STRING, "", WireFormat.FieldType.STRING,
				"");
		dyno.setField(type.findFieldByName("headers"),
				Arrays.asList(entry.toBuilder().setKey("one").setValue("two").build(),
						entry.toBuilder().setKey("three").setValue("four").build()));
		var msg = dyno.build();
		assertThat(msg.getField(type.findFieldByName("payload"))).isEqualTo(ByteString.copyFromUtf8("Hello World"));
		// System.err.println(msg);
	}

	@Test
	void testMessage() throws Exception {
		byte[] bytes = new byte[] { 10, 11, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 18, 10, 10, 3, 111, 110,
				101, 18, 3, 116, 119, 111, 18, 13, 10, 5, 116, 104, 114, 101, 101, 18, 4, 102, 111, 117, 114 };
		SpringMessage msg = SpringMessage.parseFrom(bytes);
		// System.err.println(msg);
		assertThat(msg.getPayload()).isEqualTo(ByteString.copyFromUtf8("Hello World"));
	}

	@Test
	void testGenericMessage() throws Exception {
		MapEntry<String, GenericValue> entry = MapEntry.newDefaultInstance(GenericValue.getDescriptor(),
				FieldType.STRING, "", FieldType.MESSAGE, null);
		GenericMessage msg = GenericMessage.newBuilder()
				.addRepeatedField(GenericMessage.getDescriptor().findFieldByName("fields"), entry.toBuilder()
						.setKey("str").setValue(GenericValue.newBuilder().setStringVal("Hello World").build()).build())
				.addRepeatedField(GenericMessage.getDescriptor().findFieldByName("fields"), entry.toBuilder()
						.setKey("obj")
						.setValue(
								GenericValue.newBuilder()
										.setMessageVal(GenericMessage.newBuilder().addRepeatedField(
												GenericMessage.getDescriptor().findFieldByName("fields"),
												entry.toBuilder()
														.setKey("str")
														.setValue(GenericValue.newBuilder().setStringVal("Bye Bye")
																.build())
														.build()))
										.build())
						.build())
				.build();
		// System.err.println(msg);
		assertThat(msg.getFieldsMap().get("str").getStringVal()).isEqualTo("Hello World");
		for (byte b : msg.toByteArray()) {
			System.err.println(b);
		}
		System.err.println(GenericMessage.getDescriptor().toProto());
	}

}
