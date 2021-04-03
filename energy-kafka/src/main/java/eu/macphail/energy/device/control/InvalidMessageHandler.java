package eu.macphail.energy.device.control;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Map;

public class InvalidMessageHandler implements DeserializationExceptionHandler {
    private InvalidMessageProducer invalidMessageProducer;

    @Override
    public DeserializationHandlerResponse handle(ProcessorContext context, ConsumerRecord<byte[], byte[]> record, Exception exception) {
        System.err.println("Exception caught during Deserialization, sending to the invalid message topic; " +
                exception);

        invalidMessageProducer.sendMessage(record.key(), record.value());

        return DeserializationHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.invalidMessageProducer = (InvalidMessageProducer) configs.get("invalidMessageManager");
    }
}
