package com.dobrev.kafka.emailnotification.handler;
import com.dobrev.kafka.core.ProductCreatedEvent;

import com.dobrev.kafka.emailnotification.error.NotRetryableException;
import com.dobrev.kafka.emailnotification.error.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductCreatedEventHandler {
    private final RestTemplate restTemplate;

    @KafkaListener(topics = "${product-service.kafka.topic}")
    public void handler(ProductCreatedEvent productCreatedEvent){
        log.info("Received a new event: {}", productCreatedEvent.title());

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
    }
}