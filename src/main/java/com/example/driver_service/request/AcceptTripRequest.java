package com.example.driver_service.request;

import lombok.Data;

@Data
public class AcceptTripRequest {
    private String tripId;
    private String driverId;
}
