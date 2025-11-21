package com.example.driver_service.eventListener;

import com.example.driver_service.event.AcceptTripEvent;
import com.example.driver_service.event.CreateTripEvent;
import com.example.driver_service.service.DriverService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CreateTripListener {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TOPIC_DRIVER_ASSIGNED = "trip_created";
    @Autowired
    private DriverService driverService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "trip_create_wait_driver", groupId = "driver-service-group")
    public void listenTripCreated(String message) {
        try {
            CreateTripEvent event = objectMapper.readValue(message, CreateTripEvent.class);
            double longitude = Double.parseDouble(event.getLongitude());
            double latitude = Double.parseDouble(event.getLatitude());
            GeoResults<String> drivers = driverService.findDriversNearby(
                    latitude,
                    longitude,
                    10.0
            );

            List<GeoResult<String>> driverList = drivers.getContent();

            if (driverList.isEmpty()) {
                return;
            }

//            String bestDriverId = driverList.get(0).getContent();
//            AcceptTripEvent createdTripEvent = new AcceptTripEvent();
//            createdTripEvent.setTripId(event.getTripId());
//            createdTripEvent.setDriverId(bestDriverId);
//
//            String event2Json = objectMapper.writeValueAsString(createdTripEvent);
//            kafkaTemplate.send(TOPIC_DRIVER_ASSIGNED, event2Json);

            for(GeoResult<String> driver : driverList) {
                String driverId = driver.getContent();
                AcceptTripEvent createdTripEvent = new AcceptTripEvent();
                createdTripEvent.setTripId(event.getTripId());
                createdTripEvent.setDriverId(driverId);

                System.out.println("Notifying driver: " + driverId + " for trip: " + event.getTripId());
            }
            System.out.println("Received trip event: " + event);
            // add websocket notification logic here if needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
