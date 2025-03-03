package com.dobrev.kafka.emailnotification.handler;

import com.dobrev.kafka.core.ProductCreatedEvent;
import com.dobrev.kafka.emailnotification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
@KafkaListener(topics = "${email-notification-service.kafka.delete-topic}")
public class ProductDeleteEventHandler {
    @Value("${email-notification-service.kafka.info-topic}")
    private String notificationTopic;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, ProductDeleteEvent> kafkaTemplate;

    @Transactional("jpaTransactionManager")
    @KafkaHandler
    public void productDeleteHandler(ProductDeleteEvent productDeleteEvent){
        log.info("Received a new product-delete event: {}", productDeleteEvent.title());
        sendProductEventToKafka(productDeleteEvent.productId(), productDeleteEvent);

        if (processedEventRepository.deleteByProductId(productDeleteEvent.productId()) == 0){
            throw new RuntimeException();
        }
    }

    public void sendProductEventToKafka(String productId, ProductDeleteEvent productEvent) {
        kafkaTemplate.send(notificationTopic, productId, productEvent)
                .whenComplete((result, exception) -> {
                    if (exception != null){
                        log.error("Failed to send message: {}", exception.getMessage());
                    } else{
                        log.info("Message sent successfully: {}", result.getRecordMetadata());
                    }
                });
    }
}