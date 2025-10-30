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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DriverServiceImpl implements DriverService{
    private final DriverRepository driverRepository;
    private final UserClient userClient;
    private static final String TRIP_CREATED_TOPIC = "trip_created";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DriverServiceImpl(DriverRepository driverRepository, UserClient userClient, KafkaTemplate<String, String> kafkaTemplate) {
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
    public Driver updateDriverLocation(DriverLocationRequest driverLocationRequest, String id) throws Exception {
        Driver driver = driverRepository.findByDriverId(id);
        if (driver != null) {
            driver.setLatitude(driverLocationRequest.getLatitude());
            driver.setLongitude(driverLocationRequest.getLongitude());
            driver.setDetailLocation(driverLocationRequest.getDetailLocation());
            return driverRepository.save(driver);
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
}
