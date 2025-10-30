package com.example.driver_service.request;

import lombok.Data;

@Data
public class DriverLocationRequest {
    private String latitude;
    private String longitude;
    private String detailLocation;
}
