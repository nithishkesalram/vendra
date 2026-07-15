package com.procureai.infra.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProcurementEventPublisher implements ProcurementEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaProcurementEventPublisher.class);

    private final ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider;
    private final boolean kafkaEnabled;
    private final String poCreatedTopic;
    private final String poApprovedTopic;
    private final String poRejectedTopic;
    private final String documentUploadedTopic;

    public KafkaProcurementEventPublisher(
            ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplateProvider,
            @Value("${procureai.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${procureai.kafka.topics.po-created}") String poCreatedTopic,
            @Value("${procureai.kafka.topics.po-approved}") String poApprovedTopic,
            @Value("${procureai.kafka.topics.po-rejected}") String poRejectedTopic,
            @Value("${procureai.kafka.topics.document-uploaded}") String documentUploadedTopic
    ) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.kafkaEnabled = kafkaEnabled;
        this.poCreatedTopic = poCreatedTopic;
        this.poApprovedTopic = poApprovedTopic;
        this.poRejectedTopic = poRejectedTopic;
        this.documentUploadedTopic = documentUploadedTopic;
    }

    @Override
    public void poCreated(PoCreatedEvent event) {
        publish(poCreatedTopic, event.purchaseOrderId().toString(), event);
    }

    @Override
    public void poStatusChanged(PoStatusChangedEvent event) {
        String topic = switch (event.newStatus()) {
            case "APPROVED" -> poApprovedTopic;
            case "REJECTED" -> poRejectedTopic;
            default -> "po.status.changed";
        };
        publish(topic, event.purchaseOrderId().toString(), event);
    }

    @Override
    public void documentUploaded(DocumentUploadedEvent event) {
        publish(documentUploadedTopic, event.contractId().toString(), event);
    }

    private void publish(String topic, String key, Object payload) {
        if (!kafkaEnabled) {
            log.info("Kafka disabled; event {} would be published to {}", payload, topic);
            return;
        }
        KafkaTemplate<Object, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("Kafka enabled but no KafkaTemplate is available; dropping event {}", payload);
            return;
        }
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to {}", topic, ex);
                    } else {
                        log.info("Published event to {} partition {}", topic, result.getRecordMetadata().partition());
                    }
                });
    }
}
