package com.procureai.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "procureai.kafka.enabled", havingValue = "true")
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ProcessedEventRepository processedEventRepository;

    public NotificationConsumer(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = {
            "${procureai.kafka.topics.po-created}",
            "${procureai.kafka.topics.po-approved}",
            "${procureai.kafka.topics.po-rejected}",
            "${procureai.kafka.topics.vendor-risk-flagged}"
    })
    @Transactional
    public void handle(ConsumerRecord<String, String> record) {
        String eventId = fingerprint(record.topic(), record.key(), record.value());
        if (processedEventRepository.existsById(eventId)) {
            log.info("Skipping duplicate event {} from {}", eventId, record.topic());
            return;
        }
        log.info("Simulated notification for topic {}: {}", record.topic(), record.value());
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setTopic(record.topic());
        processedEvent.setProcessedAt(Instant.now());
        processedEventRepository.save(processedEvent);
    }

    private String fingerprint(String topic, String key, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = topic + ":" + key + ":" + value;
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
