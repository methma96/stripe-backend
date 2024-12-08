package com.org.stripepaymentapp.controller;

import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.dto.ServiceRequest;
import com.org.stripepaymentapp.model.Service;
import com.org.stripepaymentapp.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @PostMapping("/update-service")
    public ResponseEntity<Map<String, String>> addProviderDetails(@RequestBody ServiceRequest serviceRequest) {
        try {

            Map<String, String> response = new HashMap<>();
            Optional<Service> serviceOpt =serviceRepository.findByServiceProviderID(serviceRequest.getServiceProviderId());
            if(serviceOpt.isPresent()){
                com.org.stripepaymentapp.model.Service service = serviceOpt.get();
                service.setName(serviceRequest.getDescription());
                service.setAmount(serviceRequest.getAmount());
                serviceRepository.save(service);
            }

            response.put("accountId",serviceRequest.getServiceProviderId());


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return an error message as JSON as well
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}
