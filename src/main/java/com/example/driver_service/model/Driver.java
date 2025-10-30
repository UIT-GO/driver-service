package com.example.driver_service.model;

import com.example.driver_service.ENUM.Status;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Driver {
    @Id
    private String id;
    private String driverId;
    private String latitude;
    private String longitude;
    private Status status;
    private String detailLocation;
}
