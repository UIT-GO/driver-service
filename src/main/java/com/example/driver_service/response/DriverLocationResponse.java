package com.example.driver_service.response;

import lombok.Data;

@Data
public class DriverLocationResponse {
    private String driverId;
    private String latitude;
    private String longitude;
    private String detailLocation;
    private String driverName;
}
