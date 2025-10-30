package com.example.driver_service.repository;

import com.example.driver_service.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DriverRepository extends MongoRepository<Driver, String> {
    Driver findByDriverId(String driverId);
}
