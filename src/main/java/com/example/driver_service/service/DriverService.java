package com.example.driver_service.service;

import com.example.driver_service.model.Driver;
import com.example.driver_service.request.DriverLocationRequest;
import com.example.driver_service.response.DriverLocationResponse;

public interface DriverService {
    DriverLocationResponse getDriverLocation(String driverId) throws Exception;
    Driver updateDriverLocation(DriverLocationRequest driverLocationRequest, String id) throws Exception;
    String turnOnDriver(String driverId) throws Exception;
    String turnOffDriver(String driverId) throws Exception;
    String acceptTrip(String driverId, String tripId) throws Exception;
}
