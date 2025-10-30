package com.example.driver_service.client;

import com.example.driver_service.DTO.UserDTO;
import com.example.driver_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "user-service",
        url = "http://localhost:3030",
        path = "/api/users",
        configuration = FeignConfig.class
)
public interface UserClient {
    @GetMapping("/me")
    UserDTO getUserInfo();
}
