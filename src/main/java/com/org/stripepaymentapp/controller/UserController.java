package com.org.stripepaymentapp.controller;

import com.org.stripepaymentapp.dto.ConnectAccountRequest;
import com.org.stripepaymentapp.service.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private StripeService stripeService;

    @PostMapping("/create-connected-account")
    public ResponseEntity<Map<String, String>> createPaymentLink(@RequestBody ConnectAccountRequest connectAccountRequest) {
        try {
            String accountId = stripeService.createConnectedAccount(connectAccountRequest);

            // Create a JSON response with the paymentUrl
            Map<String, String> response = new HashMap<>();

            response.put("accountId", accountId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return an error message as JSON as well
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


}
