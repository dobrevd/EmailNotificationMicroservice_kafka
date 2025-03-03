package com.dobrev.kafka.emailnotification.repository;

import com.dobrev.kafka.emailnotification.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {
    boolean existsByMessageId(String messageId);
    int deleteByProductId(String productId);
}