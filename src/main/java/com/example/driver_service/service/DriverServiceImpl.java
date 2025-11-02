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

    @Override
    public DriverLocationResponse getDriverLocation(String driverId) throws Exception {
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
            throw new Exception("Driver not found");
        }
    }

    @Override
    public String updateDriverLocation(DriverLocationRequest driverLocationRequest, String id) throws Exception {
        if (id != null) {
            double longitude = Double.parseDouble(driverLocationRequest.getLongitude());
            double latitude = Double.parseDouble(driverLocationRequest.getLatitude());
            geoOperations.add(new Point(longitude, latitude), id);
            return "Successfully updated location for driver " + id;
        } else {
            throw new Exception("Driver not found");
        }
    }


    @Override
    public String turnOnDriver(String driverId) throws Exception {
        Driver driver = driverRepository.findByDriverId(driverId);
        if (driver != null) {
            driver.setStatus(Status.ON);
            driverRepository.save(driver);
            return "Driver is now available";
        } else {
            throw new Exception("Driver not found");
        }
    }

    @Override
    public String turnOffDriver(String driverId) throws Exception {
        Driver driver = driverRepository.findByDriverId(driverId);
        if (driver != null) {
            driver.setStatus(Status.OFF);
            driverRepository.save(driver);
            return "Driver is now available";
        } else {
            throw new Exception("Driver not found");
        }
    }

    @Override
    public String acceptTrip(String driverId, String tripId) throws Exception {
        AcceptTripEvent acceptTripEvent = new AcceptTripEvent();
        acceptTripEvent.setDriverId(driverId);
        acceptTripEvent.setTripId(tripId);
        // Here you would typically publish the event to a message broker like Kafka
        String json = new ObjectMapper().writeValueAsString(acceptTripEvent);
        System.out.println(json);
        kafkaTemplate.send(TRIP_CREATED_TOPIC, json);

        return "Driver " + driverId + " accepted trip " + tripId;
    }

    @Override
    public GeoResults<String> findDriversNearby(double latitude, double longitude, double radius) {
        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radius, Metrics.KILOMETERS);
        Circle circle = new Circle(center, distance);

        GeoResults<GeoLocation<String>> results = geoOperations.radius(circle);

        List<GeoResult<String>> mapped = results.getContent().stream()
            .map(r -> new GeoResult<>(r.getContent().getName(), r.getDistance()))
            .toList();

        return new GeoResults<>(mapped);
    }
}
