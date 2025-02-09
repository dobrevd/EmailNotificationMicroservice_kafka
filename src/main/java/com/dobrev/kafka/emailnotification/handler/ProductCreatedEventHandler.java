package com.dobrev.kafka.emailnotification.handler;
import com.dobrev.kafka.core.ProductCreatedEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductCreatedEventHandler {
    @KafkaListener(topics = "${product-service.kafka.topic}")
    public void handler(ProductCreatedEvent productCreatedEvent){
        log.info("Received a new event: {}", productCreatedEvent.title());
    }
}