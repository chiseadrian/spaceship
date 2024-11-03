package com.example.spaceship.IT;

import com.example.spaceship.domain.entities.Ship;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
public class ShipIntegrationTest {

    private static final String USERNAME = "user";

    private static final String PASSWORD = "pass";

    private static final DockerImageName DOCKER_KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:latest");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static KafkaContainer kafkaContainer;

    @org.junit.jupiter.api.BeforeAll
    static void setup() {
        kafkaContainer = new KafkaContainer(DOCKER_KAFKA_IMAGE);
        kafkaContainer.start();

        System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());
    }

    @Test
    public void testFullShipLifecycle() throws Exception {
        // Create a new ship
        MvcResult createResult = mockMvc.perform(post("/api/spaceships")
                        .with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateShip())))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Ship createdShip = objectMapper.readValue(responseContent, Ship.class);
        Long shipId = createdShip.getId();

        assertNotNull(shipId);
        assertEquals("USS Enterprise", createdShip.getName());
        assertEquals("Constitution", createdShip.getType());

        // Get the created ship
        mockMvc.perform(get("/api/spaceships/" + shipId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("USS Enterprise"))
                .andExpect(jsonPath("$.type").value("Constitution"));

        // Update the ship
        Ship updatedShip = new Ship();
        updatedShip.setName("USS Enterprise-A");
        updatedShip.setType("Constitution Refit");
        mockMvc.perform(put("/api/spaceships/" + shipId)
                        .with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedShip)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("USS Enterprise-A"))
                .andExpect(jsonPath("$.type").value("Constitution Refit"));

        // Search for ships
        mockMvc.perform(get("/api/spaceships/search")
                        .with(httpBasic(USERNAME, PASSWORD))
                        .param("name", "Enterprise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("USS Enterprise-A"));

        // Delete the ship
        mockMvc.perform(delete("/api/spaceships/" + shipId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isNoContent());

        // Verify the ship is deleted
        mockMvc.perform(get("/api/spaceships/" + shipId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isNotFound());
    }

    private Ship generateShip() {
        Ship ship = new Ship();
        ship.setName("USS Enterprise");
        ship.setType("Constitution");
        return ship;
    }
}