package com.procureai.infra.kafka;

public interface ProcurementEventPublisher {

    void poCreated(PoCreatedEvent event);

    void poStatusChanged(PoStatusChangedEvent event);

    void documentUploaded(DocumentUploadedEvent event);
}
