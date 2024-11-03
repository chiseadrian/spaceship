package com.example.spaceship.infrastructure.kakfa;


import com.example.spaceship.domain.entities.Ship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class KafkaConsumer {

    private final List<Ship> ships = new ArrayList<>();

    @KafkaListener(topics = "${kafka.topic.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(Ship ship) {
        ships.add(ship);
        log.info("Received ship: {}", ship);
    }

    public List<Ship> getAllShips() {
        return new ArrayList<>(ships);
    }
}
