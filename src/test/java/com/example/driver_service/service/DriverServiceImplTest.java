package com.example.driver_service.service;

import com.example.driver_service.DTO.UserDTO;
import com.example.driver_service.ENUM.Status;
import com.example.driver_service.client.UserClient;
import com.example.driver_service.model.Driver;
import com.example.driver_service.repository.DriverRepository;
import com.example.driver_service.request.DriverLocationRequest;
import com.example.driver_service.response.DriverLocationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.BoundGeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private BoundGeoOperations<String, String> geoOperations;

    private DriverServiceImpl driverService;

    private Driver testDriver;
    private UserDTO testUserDTO;
    private DriverLocationRequest testLocationRequest;

    @BeforeEach
    void setUp() {
        when(redisTemplate.boundGeoOps(anyString())).thenReturn(geoOperations);
        
        driverService = new DriverServiceImpl(
            driverRepository,
            userClient,
            kafkaTemplate,
            redisTemplate
        );

        // Setup test data
        testDriver = new Driver();
        testDriver.setId("1");
        testDriver.setDriverId("driver123");
        testDriver.setLatitude("10.762622");
        testDriver.setLongitude("106.660172");
        testDriver.setDetailLocation("Ho Chi Minh City");
        testDriver.setStatus(Status.OFF);

        testUserDTO = new UserDTO();
        testUserDTO.setUserId("user123");
        testUserDTO.setName("John Doe");
        testUserDTO.setEmail("john.doe@example.com");

        testLocationRequest = new DriverLocationRequest();
        testLocationRequest.setLatitude("10.762622");
        testLocationRequest.setLongitude("106.660172");
        testLocationRequest.setDetailLocation("Ho Chi Minh City");
    }

    @Test
    void getDriverLocation_WhenDriverExists_ShouldReturnDriverLocationResponse() throws Exception {
        // Arrange
        when(driverRepository.findByDriverId("driver123")).thenReturn(testDriver);
        when(userClient.getUserInfo()).thenReturn(testUserDTO);

        // Act
        DriverLocationResponse result = driverService.getDriverLocation("driver123");

        // Assert
        assertNotNull(result);
        assertEquals("driver123", result.getDriverId());
        assertEquals("10.762622", result.getLatitude());
        assertEquals("106.660172", result.getLongitude());
        assertEquals("Ho Chi Minh City", result.getDetailLocation());
        assertEquals("John Doe", result.getDriverName());

        verify(driverRepository).findByDriverId("driver123");
        verify(userClient).getUserInfo();
    }

    @Test
    void getDriverLocation_WhenDriverNotFound_ShouldThrowException() {
        // Arrange
        when(driverRepository.findByDriverId("nonexistent")).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            driverService.getDriverLocation("nonexistent");
        });

        assertEquals("Driver not found", exception.getMessage());
        verify(driverRepository).findByDriverId("nonexistent");
        verify(userClient, never()).getUserInfo();
    }

    @Test
    void updateDriverLocation_WhenDriverIdIsValid_ShouldUpdateLocationSuccessfully() throws Exception {
        // Arrange
        String driverId = "driver123";
        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);

        // Act
        String result = driverService.updateDriverLocation(testLocationRequest, driverId);

        // Assert
        assertEquals("Successfully updated location for driver driver123", result);
        
        verify(geoOperations).add(pointCaptor.capture(), eq(driverId));
        Point capturedPoint = pointCaptor.getValue();
        assertEquals(106.660172, capturedPoint.getX(), 0.000001);
        assertEquals(10.762622, capturedPoint.getY(), 0.000001);
    }

    @Test
    void updateDriverLocation_WhenDriverIdIsNull_ShouldThrowException() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            driverService.updateDriverLocation(testLocationRequest, null);
        });

        assertEquals("Driver not found", exception.getMessage());
        verify(geoOperations, never()).add(any(), any());
    }

    @Test
    void turnOnDriver_WhenDriverExists_ShouldUpdateStatusToOn() throws Exception {
        // Arrange
        when(driverRepository.findByDriverId("driver123")).thenReturn(testDriver);
        when(driverRepository.save(any(Driver.class))).thenReturn(testDriver);

        // Act
        String result = driverService.turnOnDriver("driver123");

        // Assert
        assertEquals("Driver is now available", result);
        assertEquals(Status.ON, testDriver.getStatus());
        
        verify(driverRepository).findByDriverId("driver123");
        verify(driverRepository).save(testDriver);
    }

    @Test
    void turnOnDriver_WhenDriverNotFound_ShouldThrowException() {
        // Arrange
        when(driverRepository.findByDriverId("nonexistent")).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            driverService.turnOnDriver("nonexistent");
        });

        assertEquals("Driver not found", exception.getMessage());
        verify(driverRepository).findByDriverId("nonexistent");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void turnOffDriver_WhenDriverExists_ShouldUpdateStatusToOff() throws Exception {
        // Arrange
        testDriver.setStatus(Status.ON); // Start with ON status
        when(driverRepository.findByDriverId("driver123")).thenReturn(testDriver);
        when(driverRepository.save(any(Driver.class))).thenReturn(testDriver);

        // Act
        String result = driverService.turnOffDriver("driver123");

        // Assert
        assertEquals("Driver is now available", result);
        assertEquals(Status.OFF, testDriver.getStatus());
        
        verify(driverRepository).findByDriverId("driver123");
        verify(driverRepository).save(testDriver);
    }

    @Test
    void turnOffDriver_WhenDriverNotFound_ShouldThrowException() {
        // Arrange
        when(driverRepository.findByDriverId("nonexistent")).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            driverService.turnOffDriver("nonexistent");
        });

        assertEquals("Driver not found", exception.getMessage());
        verify(driverRepository).findByDriverId("nonexistent");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void findDriversNearby_ShouldReturnGeoResults() {
        // Arrange
        double latitude = 10.762622;
        double longitude = 106.660172;
        double radius = 5.0;

        RedisGeoCommands.GeoLocation<String> geoLocation1 = 
            new RedisGeoCommands.GeoLocation<>("driver1", new Point(106.660172, 10.762622));
        RedisGeoCommands.GeoLocation<String> geoLocation2 = 
            new RedisGeoCommands.GeoLocation<>("driver2", new Point(106.661172, 10.763622));

        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult1 = 
            new GeoResult<>(geoLocation1, new Distance(1.0, Metrics.KILOMETERS));
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult2 = 
            new GeoResult<>(geoLocation2, new Distance(2.0, Metrics.KILOMETERS));

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> geoResultList = 
            Arrays.asList(geoResult1, geoResult2);

        GeoResults<RedisGeoCommands.GeoLocation<String>> mockGeoResults = 
            new GeoResults<>(geoResultList);

        when(geoOperations.radius(any(Circle.class))).thenReturn(mockGeoResults);

        // Act
        GeoResults<String> result = driverService.findDriversNearby(latitude, longitude, radius);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        
        List<GeoResult<String>> content = result.getContent();
        assertEquals("driver1", content.get(0).getContent());
        assertEquals("driver2", content.get(1).getContent());
        assertEquals(1.0, content.get(0).getDistance().getValue());
        assertEquals(2.0, content.get(1).getDistance().getValue());

        ArgumentCaptor<Circle> circleCaptor = ArgumentCaptor.forClass(Circle.class);
        verify(geoOperations).radius(circleCaptor.capture());
        
        Circle capturedCircle = circleCaptor.getValue();
        assertEquals(longitude, capturedCircle.getCenter().getX(), 0.000001);
        assertEquals(latitude, capturedCircle.getCenter().getY(), 0.000001);
        assertEquals(radius, capturedCircle.getRadius().getValue(), 0.000001);
        assertEquals(Metrics.KILOMETERS, capturedCircle.getRadius().getMetric());
    }

    @Test
    void findDriversNearby_WhenNoDriversFound_ShouldReturnEmptyResults() {
        // Arrange
        double latitude = 10.762622;
        double longitude = 106.660172;
        double radius = 5.0;

        GeoResults<RedisGeoCommands.GeoLocation<String>> emptyGeoResults = 
            new GeoResults<>(Arrays.asList());

        when(geoOperations.radius(any(Circle.class))).thenReturn(emptyGeoResults);

        // Act
        GeoResults<String> result = driverService.findDriversNearby(latitude, longitude, radius);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        
        verify(geoOperations).radius(any(Circle.class));
    }

    @Test
    void updateDriverLocation_WithInvalidCoordinates_ShouldThrowNumberFormatException() {
        // Arrange
        DriverLocationRequest invalidRequest = new DriverLocationRequest();
        invalidRequest.setLatitude("invalid_latitude");
        invalidRequest.setLongitude("106.660172");
        invalidRequest.setDetailLocation("Ho Chi Minh City");

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> {
            driverService.updateDriverLocation(invalidRequest, "driver123");
        });

        verify(geoOperations, never()).add(any(), any());
    }

    @Test
    void getDriverLocation_WhenUserClientFails_ShouldPropagateException() throws Exception {
        // Arrange
        when(driverRepository.findByDriverId("driver123")).thenReturn(testDriver);
        when(userClient.getUserInfo()).thenThrow(new RuntimeException("User service unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            driverService.getDriverLocation("driver123");
        });

        verify(driverRepository).findByDriverId("driver123");
        verify(userClient).getUserInfo();
    }

    @Test
    void constructor_ShouldInitializeGeoOperations() {
        // Arrange
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> newRedisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        BoundGeoOperations<String, String> newGeoOps = mock(BoundGeoOperations.class);
        when(newRedisTemplate.boundGeoOps(anyString())).thenReturn(newGeoOps);
        
        // Act
        DriverServiceImpl service = new DriverServiceImpl(
            driverRepository,
            userClient,
            kafkaTemplate,
            newRedisTemplate
        );

        // Assert
        verify(newRedisTemplate).boundGeoOps("active_drivers");
        assertNotNull(service);
    }

    @Test
    void acceptTrip_ShouldLogAndPublishEvent() throws Exception {
        // Arrange
        String driverId = "driver123";
        String tripId = "trip456";
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        String result = driverService.acceptTrip(driverId, tripId);

        // Assert
        assertEquals("Driver driver123 accepted trip trip456", result);

        // Verify trip_created event content
        verify(kafkaTemplate, atLeastOnce()).send(topicCaptor.capture(), messageCaptor.capture());
        List<String> topics = topicCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();

        // Find the trip_created publish call
        int tripCreatedIndex = -1;
        for (int i = 0; i < topics.size(); i++) {
            if ("trip_created".equals(topics.get(i))) {
                tripCreatedIndex = i;
                break;
            }
        }
        assertTrue(tripCreatedIndex >= 0, "trip_created topic should be sent");
        String tripCreatedMessage = messages.get(tripCreatedIndex);
        assertTrue(tripCreatedMessage.contains("\"driverId\":\"driver123\""));
        assertTrue(tripCreatedMessage.contains("\"tripId\":\"trip456\""));

        // Verify driver-logs were sent twice (start and success)
        verify(kafkaTemplate, times(2)).send(eq("driver-logs"), anyString());
    }
}