package eu.macphail.energy.device.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.macphail.energy.device.entity.Device;
import eu.macphail.energy.device.entity.RawDeviceEvent;
import eu.macphail.energy.device.entity.RawDeviceEventParsed;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class RawEventParsedSerde implements Serde<RawDeviceEventParsed> {
    private final Serde<RawDeviceEvent> rawDeviceEventSerde;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public RawEventParsedSerde(Serde<RawDeviceEvent> rawDeviceEventSerde) {
        this.rawDeviceEventSerde = rawDeviceEventSerde;
    }

    @Override
    public Serializer<RawDeviceEventParsed> serializer() {
        Serializer<RawDeviceEvent> rawDeviceEventSerializer = rawDeviceEventSerde.serializer();
        return new Serializer<RawDeviceEventParsed>() {
            @Override
            public byte[] serialize(String topic, RawDeviceEventParsed data) {
                RawDeviceEvent rawDeviceData = new RawDeviceEvent();
                rawDeviceData.setReceivedTimestamp(data.getReceivedTimestamp());
                rawDeviceData.setRegionId(data.getRegionId());

                String rawPayload = data.getDevices().stream()
                        .map(this::deviceToString)
                        .collect(Collectors.joining("\n"));

                rawDeviceData.setPayload(rawPayload);
                return rawDeviceEventSerializer.serialize(topic, rawDeviceData);
            }

            private String deviceToString(eu.macphail.energy.device.entity.Device device) {
                try {
                    return objectMapper.writeValueAsString(device);
                } catch (JsonProcessingException e) {
                    throw new SerializationException(e);
                }
            }
        };
    }

    @Override
    public Deserializer<RawDeviceEventParsed> deserializer() {
        Deserializer<RawDeviceEvent> rawDeviceEventDeserializer = rawDeviceEventSerde.deserializer();
        return new Deserializer<RawDeviceEventParsed>() {
            @Override
            public RawDeviceEventParsed deserialize(String topic, byte[] data) {
                RawDeviceEvent event = rawDeviceEventDeserializer.deserialize(topic, data);
                RawDeviceEventParsed rawDeviceEventParsed = new RawDeviceEventParsed(
                        event.getReceivedTimestamp(),
                        event.getRegionId().toString(),
                        parseDevices(event.getPayload().toString())
                );
                return rawDeviceEventParsed;
            }

            private List<Device> parseDevices(String payload) {
                try {
                    String p = parsePayload(payload);
                    TypeReference<List<Device>> typeReference = new TypeReference<List<Device>>() {};
                    List<Device> devices = objectMapper.readValue(p, typeReference);
                    return devices;
                } catch (IOException e) {
                    throw new SerializationException(e);
                }
            }

            private String parsePayload(String payload) {
                return "[" + String.join(",", payload.split("\n")) + "]";
            }
        };
    }
}
