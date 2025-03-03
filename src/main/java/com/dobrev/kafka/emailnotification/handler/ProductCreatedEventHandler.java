package com.dobrev.kafka.emailnotification.handler;
import com.dobrev.kafka.core.ProductCreatedEvent;

import com.dobrev.kafka.emailnotification.entity.ProcessedEventEntity;
import com.dobrev.kafka.emailnotification.error.NotRetryableException;
import com.dobrev.kafka.emailnotification.error.RetryableException;
import com.dobrev.kafka.emailnotification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCreatedEventHandler {
    @Value("${app.requestUrl}")
    private String requestUrl;
    private final RestTemplate restTemplate;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "${email-notification-service.kafka.created-topic}")
    public void productCreatedHandler(@Payload ProductCreatedEvent productCreatedEvent,
                        @Header(value = "messageId", required = true) String messageId,
                        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey){
        log.info("Received a new event: {}", productCreatedEvent.title());

        if(processedEventRepository.existsByMessageId(messageId)){
            log.info("Found a duplicate message id: {}", messageId);
            return;
        }
        processEvent(productCreatedEvent, messageId);
    }

    private void processEvent(ProductCreatedEvent event, String messageId) {
        notifyExternalService();
        persistProcessedEvent(messageId, event.productId());
    }

    private void notifyExternalService(){
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Received response from remote service: {}", response.getBody());
            }
        } catch (ResourceAccessException ex) {
            log.error("Remote service is unreachable: {}", ex.getMessage());
            throw new RetryableException(ex);
        } catch (Exception ex) {
            log.error("Unexpected error: {}", ex.getMessage());
            throw new NotRetryableException(ex);
        }
    }

    private void persistProcessedEvent(String messageId, String productId){
        var event = ProcessedEventEntity.builder()
                .messageId(messageId)
                .productId(productId)
                .build();
        processedEventRepository.save(event);
    }
}