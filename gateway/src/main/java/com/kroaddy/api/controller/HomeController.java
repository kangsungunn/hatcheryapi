package com.kroaddy.api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<?> home() {
        // Swagger UI로 리다이렉트
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/docs")
                .build();
    }
    
    @GetMapping("/favicon.ico")
    public ResponseEntity<?> favicon() {
        // favicon은 404 반환 (정상)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}

