package com.example.driver_service.eventListener;

import com.example.driver_service.event.CreateTripEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CreateTripListener {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "trip_create_wait_driver", groupId = "driver-service-group")
    public void listenTripCreated(String message) {
        try {
            CreateTripEvent event = objectMapper.readValue(message, CreateTripEvent.class);
            // Process the event (e.g., log, update DB, etc.)
            System.out.println("Received trip event: " + event);
            // add websocket notification logic here if needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
