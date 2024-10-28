package com.org.stripepaymentapp.controller;

import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Job;
import com.org.stripepaymentapp.model.SessionInfo;
import com.org.stripepaymentapp.repository.JobRepository;
import com.org.stripepaymentapp.repository.SessionInfoRepository;
import com.org.stripepaymentapp.service.PaymentService;
import com.org.stripepaymentapp.service.StripeService;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {



    @Autowired
    private StripeService stripeService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SessionInfoRepository sessionInfoRepository;

    @Autowired
    private JobRepository jobRepository;

    @PostMapping("/create-payment-link")
    public ResponseEntity<Map<String, String>> createPaymentLink(@RequestBody PaymentLinkRequest paymentLinkRequest) {
        try {
            String clientSecret = stripeService.createPaymentLink(paymentLinkRequest);

            // Create a JSON response with the paymentUrl
            Map<String, String> response = new HashMap<>();

            response.put("clientSecret", clientSecret);


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return an error message as JSON as well
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        Map<String, Object> response = new HashMap<>();

        try {
            // Retrieve the session from Stripe to verify the payment
            Session session = Session.retrieve(sessionId);

            if (session.getStatus().equalsIgnoreCase("complete")) {

                // Optionally, you can update your database or perform any necessary actions based on the session status
                paymentService.updatePaymentStatus(sessionId);

                // Prepare success response
                response.put("status", "success");
                response.put("message", "Payment verified successfully");
                response.put("session", session); // Optionally include session data if needed

                return ResponseEntity.ok(response);
            }else{
                response.put("status", "error");
                response.put("message", "Error verifying payment: " );
                return ResponseEntity.badRequest().body(response);

            }
        } catch (Exception e) {
            // Prepare error response
            response.put("status", "error");
            response.put("message", "Error verifying payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> refundPayment(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        Map<String, Object> response = new HashMap<>();

        try {
            // Retrieve the session from Stripe to verify the payment
            Optional<SessionInfo> sessionInfoList= sessionInfoRepository.findBySessionId(sessionId);

            if(sessionInfoList.isPresent()){
                SessionInfo sessionInfo= sessionInfoList.get();
                Optional<Job> jobRequest = jobRepository.findById(sessionInfo.getJobId());

                if(jobRequest.isPresent()){
                    Job job= jobRequest.get();
                    stripeService.transferAmountToServiceProvider(sessionInfo.getAccountId(), job.getAmount(), job.getCurrency());
                    job.setStatus("CANCELLED");
                    if (job.getPaymentStatus().equalsIgnoreCase("ESCROW")) {
                        job.setPaymentStatus("REFUNDED");
                    }

                    if(job.getPaymentStatus().equalsIgnoreCase("SUCCESS")){
                        job.setPaymentStatus("DISPUTED");
                    }

                    jobRepository.save(job);

                } else {
                    return ResponseEntity.ok(response);
                }

                return ResponseEntity.ok(response);
            } else {
                throw new Exception("Job not found");
            }

        } catch (Exception e) {
            // Prepare error response
            response.put("status", "error");
            response.put("message", "Error verifying payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}