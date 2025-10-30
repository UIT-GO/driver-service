package com.example.driver_service.controller;

import com.example.driver_service.model.Driver;
import com.example.driver_service.request.AcceptTripRequest;
import com.example.driver_service.request.DriverLocationRequest;
import com.example.driver_service.response.DriverLocationResponse;
import com.example.driver_service.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {
    @Autowired
    private DriverService driverService;

    @PutMapping("/{id}/location")
    ResponseEntity<Driver> updateDriverLocation(@RequestBody DriverLocationRequest driverLocationRequest, @PathVariable String id) throws Exception {
        return ResponseEntity.ok(driverService.updateDriverLocation(driverLocationRequest, id));
    }

    @GetMapping("/{id}/location")
    ResponseEntity<DriverLocationResponse> getDriverLocation(@PathVariable("id") String driverId) throws Exception {
        return ResponseEntity.ok(driverService.getDriverLocation(driverId));
    }

    @PutMapping("/{id}/on")
    ResponseEntity<String> turnOnDriver(@PathVariable("id") String driverId) throws Exception {
        return ResponseEntity.ok(driverService.turnOnDriver(driverId));
    }

    @PutMapping("/{id}/off")
    ResponseEntity<String> turnOffDriver(@PathVariable("id") String driverId) throws Exception {
        return ResponseEntity.ok(driverService.turnOffDriver(driverId));
    }

    @PostMapping()
    public ResponseEntity<String> acceptTrip(@RequestBody AcceptTripRequest acceptTripRequest) throws Exception {
        // Placeholder implementation
        return ResponseEntity.ok(driverService.acceptTrip(acceptTripRequest.getDriverId(), acceptTripRequest.getTripId()));
    }
}
