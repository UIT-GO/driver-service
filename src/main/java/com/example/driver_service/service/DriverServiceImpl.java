package com.example.driver_service.service;

import com.example.driver_service.DTO.UserDTO;
import com.example.driver_service.ENUM.Status;
import com.example.driver_service.client.UserClient;
import com.example.driver_service.event.AcceptTripEvent;
import com.example.driver_service.model.Driver;
import com.example.driver_service.repository.DriverRepository;
import com.example.driver_service.request.DriverLocationRequest;
import com.example.driver_service.response.DriverLocationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.core.BoundGeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.GeoResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DriverServiceImpl implements DriverService{
    private final DriverRepository driverRepository;
    private final UserClient userClient;
    private static final String ACTIVE_DRIVERS_KEY = "active_drivers";
    private static final String TRIP_CREATED_TOPIC = "trip_created";
    private static final String DRIVER_LOGS_TOPIC = "driver-logs";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BoundGeoOperations<String, String> geoOperations;

    public DriverServiceImpl(
        DriverRepository driverRepository, 
        UserClient userClient, 
        KafkaTemplate<String, String> kafkaTemplate,
        RedisTemplate<String, String> redisTemplate
    ) {
        this.geoOperations = redisTemplate.boundGeoOps(ACTIVE_DRIVERS_KEY);
        this.driverRepository = driverRepository;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    private void logToKafka(String message) {
        // Wrap the log message in JSON format with service_name and timestamp
        String jsonLog = String.format("{\"message\":%s, \"service_name\":\"driver-service\", \"timestamp\":\"%s\"}",
                new ObjectMapper().valueToTree(message).toString(),
                java.time.Instant.now().toString());
        kafkaTemplate.send(DRIVER_LOGS_TOPIC, jsonLog);
    }

    @Override
    public DriverLocationResponse getDriverLocation(String driverId) throws Exception {
        logToKafka("getDriverLocation called for driverId: " + driverId);
        Driver driver = driverRepository.findByDriverId(driverId);
        if (driver != null) {
            DriverLocationResponse response = new DriverLocationResponse();
            response.setDriverId(driver.getDriverId());
            response.setLatitude(driver.getLatitude());
            response.setLongitude(driver.getLongitude());
            response.setDetailLocation(driver.getDetailLocation());
            UserDTO userDTO = userClient.getUserInfo();
            response.setDriverName(userDTO.getName());
            return response;
        } else {
            logToKafka("Driver not found for driverId: " + driverId);
            throw new Exception("Driver not found");
        }
    }

    @Override
    public String updateDriverLocation(DriverLocationRequest driverLocationRequest, String id) throws Exception {
        logToKafka("updateDriverLocation called for driverId: " + id);
        if (id != null) {
            double longitude = Double.parseDouble(driverLocationRequest.getLongitude());
            double latitude = Double.parseDouble(driverLocationRequest.getLatitude());
            geoOperations.add(new Point(longitude, latitude), id);
            logToKafka("Successfully updated location for driver after ci/cd" + id);
            return "Successfully updated location for driver " + id;
        } else {
            logToKafka("Driver not found for update location, driverId: " + id);
            throw new Exception("Driver not found");
        }
    }


    @Override
    public String turnOnDriver(String driverId) throws Exception {
        logToKafka("turnOnDriver called for driverId: " + driverId);
        Driver driver = driverRepository.findByDriverId(driverId);
        if (driver != null) {
            driver.setStatus(Status.ON);
            driverRepository.save(driver);
            logToKafka("Driver is now available: " + driverId);
            return "Driver is now available";
        } else {
            logToKafka("Driver not found for turnOn, driverId: " + driverId);
            throw new Exception("Driver not found");
        }
    }

    @Override
    public String turnOffDriver(String driverId) throws Exception {
        logToKafka("turnOffDriver called for driverId: " + driverId);
        Driver driver = driverRepository.findByDriverId(driverId);
        if (driver != null) {
            driver.setStatus(Status.OFF);
            driverRepository.save(driver);
            logToKafka("Driver is now unavailable: " + driverId);
            return "Driver is now available";
        } else {
            logToKafka("Driver not found for turnOff, driverId: " + driverId);
            throw new Exception("Driver not found");
        }
    }

    @Override
    public String acceptTrip(String driverId, String tripId) throws Exception {
        logToKafka("acceptTrip called for driverId: " + driverId + ", tripId: " + tripId);

        AcceptTripEvent acceptTripEvent = new AcceptTripEvent();
        acceptTripEvent.setDriverId(driverId);
        acceptTripEvent.setTripId(tripId);
        String json = new ObjectMapper().writeValueAsString(acceptTripEvent);
        System.out.println(json);
        kafkaTemplate.send(TRIP_CREATED_TOPIC, json);
        logToKafka("Driver " + driverId + " accepted trip " + tripId);

        return "Driver " + driverId + " accepted trip " + tripId;
    }

    @Override
    public GeoResults<String> findDriversNearby(double latitude, double longitude, double radius) {
        logToKafka("findDriversNearby called for lat: " + latitude + ", lon: " + longitude + ", radius: " + radius);

        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radius, Metrics.KILOMETERS);
        Circle circle = new Circle(center, distance);
        GeoResults<GeoLocation<String>> results = geoOperations.radius(circle);
        List<GeoResult<String>> mapped = results.getContent().stream()
            .map(r -> new GeoResult<>(r.getContent().getName(), r.getDistance()))
            .toList();

        logToKafka("findDriversNearby found " + mapped.size() + " drivers");
        return new GeoResults<>(mapped);
    }
}
