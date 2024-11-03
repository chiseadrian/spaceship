package com.example.spaceship.infrastructure.kakfa;

import com.example.spaceship.domain.entities.Ship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Ship> kafkaTemplate;

    @Value("${kafka.topic.name}")
    private String topic;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, Ship> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(Ship ship) {
        log.info("Publishing message to {}: {}", topic, ship);
        String key = ship.getId().toString();
        kafkaTemplate.send(topic, key, ship);
    }

}
