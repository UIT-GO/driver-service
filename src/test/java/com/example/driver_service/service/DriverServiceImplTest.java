package com.example.driver_service.service;

import com.example.driver_service.DTO.UserDTO;
import com.example.driver_service.client.UserClient;
import com.example.driver_service.model.Driver;
import com.example.driver_service.repository.DriverRepository;
import com.example.driver_service.request.DriverLocationRequest;
import com.example.driver_service.ENUM.Status;
import com.example.driver_service.event.AcceptTripEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {
    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private DriverServiceImpl driverService;

    @Test
    void testUpdateDriverLocation_DriverFound() throws Exception {
        String driverId = "123";
        DriverLocationRequest request = new DriverLocationRequest();
        request.setLatitude("10.0");
        request.setLongitude("20.0");
        request.setDetailLocation("Test Location");
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        when(driverRepository.findByDriverId(driverId)).thenReturn(driver);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        Driver updatedDriver = driverService.updateDriverLocation(request, driverId);
        assertEquals("10.0", updatedDriver.getLatitude());
        assertEquals("20.0", updatedDriver.getLongitude());
        assertEquals("Test Location", updatedDriver.getDetailLocation());
    }

    @Test
    void testUpdateDriverLocation_DriverNotFound() {
        String driverId = "123";
        DriverLocationRequest request = new DriverLocationRequest();
        when(driverRepository.findByDriverId(driverId)).thenReturn(null);
        Exception exception = assertThrows(Exception.class, () -> driverService.updateDriverLocation(request, driverId));
        assertEquals("Driver not found", exception.getMessage());
    }

    @Test
    void testTurnOnDriver_DriverFound() throws Exception {
        String driverId = "123";
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        when(driverRepository.findByDriverId(driverId)).thenReturn(driver);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        String result = driverService.turnOnDriver(driverId);
        assertEquals(Status.ON, driver.getStatus());
        assertEquals("Driver is now available", result);
    }

    @Test
    void testTurnOnDriver_DriverNotFound() {
        String driverId = "123";
        when(driverRepository.findByDriverId(driverId)).thenReturn(null);
        Exception exception = assertThrows(Exception.class, () -> driverService.turnOnDriver(driverId));
        assertEquals("Driver not found", exception.getMessage());
    }

    @Test
    void testTurnOffDriver_DriverFound() throws Exception {
        String driverId = "123";
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        when(driverRepository.findByDriverId(driverId)).thenReturn(driver);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        String result = driverService.turnOffDriver(driverId);
        assertEquals(Status.OFF, driver.getStatus());
        assertEquals("Driver is now available", result);
    }

    @Test
    void testTurnOffDriver_DriverNotFound() {
        String driverId = "123";
        when(driverRepository.findByDriverId(driverId)).thenReturn(null);
        Exception exception = assertThrows(Exception.class, () -> driverService.turnOffDriver(driverId));
        assertEquals("Driver not found", exception.getMessage());
    }

    @Test
    void testAcceptTrip_Success() throws Exception {
        String driverId = "driver123";
        String tripId = "trip456";
        AcceptTripEvent event = new AcceptTripEvent();
        event.setDriverId(driverId);
        event.setTripId(tripId);
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedJson = objectMapper.writeValueAsString(event);
        // Mock kafkaTemplate.send to return null (or a dummy value)
        doReturn(null).when(kafkaTemplate).send(eq("trip_created"), eq(expectedJson));

        String result = driverService.acceptTrip(driverId, tripId);
        assertEquals("Driver driver123 accepted trip trip456", result);
        verify(kafkaTemplate, times(1)).send(eq("trip_created"), eq(expectedJson));
    }

    @Test
    void testGetDriverLocation_Success() throws Exception {
        String driverId = "driver123";
        Driver driver = new Driver();
        driver.setDriverId(driverId);
        driver.setLatitude("10.0");
        driver.setLongitude("20.0");
        driver.setDetailLocation("Test Location");
        when(driverRepository.findByDriverId(driverId)).thenReturn(driver);

        UserDTO userDTO = new UserDTO();
        userDTO.setName("John Doe");
        when(userClient.getUserInfo()).thenReturn(userDTO);

        var response = driverService.getDriverLocation(driverId);
        assertEquals(driverId, response.getDriverId());
        assertEquals("10.0", response.getLatitude());
        assertEquals("20.0", response.getLongitude());
        assertEquals("Test Location", response.getDetailLocation());
        assertEquals("John Doe", response.getDriverName());
    }

    @Test
    void testGetDriverLocation_DriverNotFound() {
        String driverId = "driver123";
        when(driverRepository.findByDriverId(driverId)).thenReturn(null);
        Exception exception = assertThrows(Exception.class, () -> driverService.getDriverLocation(driverId));
        assertEquals("Driver not found", exception.getMessage());
    }
}
