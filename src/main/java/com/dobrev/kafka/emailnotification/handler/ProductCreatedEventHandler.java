package com.dobrev.kafka.emailnotification.handler;
import com.dobrev.kafka.core.ProductCreatedEvent;

import com.dobrev.kafka.emailnotification.entity.ProcessedEventEntity;
import com.dobrev.kafka.emailnotification.error.NotRetryableException;
import com.dobrev.kafka.emailnotification.error.RetryableException;
import com.dobrev.kafka.emailnotification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    private final RestTemplate restTemplate;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "${product-service.kafka.topic}")
    public void handler(@Payload ProductCreatedEvent productCreatedEvent,
                        @Header(value = "messageId", required = true) String messageId,
                        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey){
        log.info("Received a new event: {}", productCreatedEvent.title());

        if(processedEventRepository.existsByMessageId(messageId)){
            log.info("Found a duplicate message id: {}", messageId);
            return;
        }

        String requestUrl = "http://localhost:8082/response/500";

        try {
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
            if (response.getStatusCode().value() == HttpStatus.OK.value()){
                log.info("Received response from remote service: {}", response.getBody());
            }
        }catch (ResourceAccessException ex){
            log.error(ex.getMessage());
            throw new RetryableException(ex);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new NotRetryableException(ex);
        }

        var event = ProcessedEventEntity.builder()
                .messageId(messageId)
                .productId(productCreatedEvent.productId())
                .build();
        try {
            processedEventRepository.save(event);
        }catch (DataIntegrityViolationException e){
            throw new NotRetryableException(e);
        }
    }
}