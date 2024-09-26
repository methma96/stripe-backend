package com.org.stripepaymentapp.controller;

import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Service;
import com.org.stripepaymentapp.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    @Autowired
    private ServiceRepository serviceRepository;

    @GetMapping("/")
    public ResponseEntity<List<Service>> getAllServices() {
        try {
            List<Service> serviceList = serviceRepository.findAll();
            return ResponseEntity.ok(serviceList);
        } catch (Exception e) {
            // Log the error for debugging purposes
            System.err.println("Error fetching services: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

}
