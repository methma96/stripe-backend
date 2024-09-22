package com.org.stripepaymentapp.controller;

import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.service.PaymentService;
import com.org.stripepaymentapp.service.StripeService;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {



    @Autowired
    private StripeService stripeService;

    @PostMapping("/create-payment-link")
    public ResponseEntity<String> createPaymentLink(@RequestBody PaymentLinkRequest paymentLinkRequest) {
        try {
            String paymentUrl = stripeService.createPaymentLink(paymentLinkRequest);
            return ResponseEntity.ok(paymentUrl);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}