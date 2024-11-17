package com.org.stripepaymentapp.controller;

import com.org.stripepaymentapp.dto.AccountSessionRequest;
import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Payment;
import com.org.stripepaymentapp.repository.JobRepository;
import com.org.stripepaymentapp.repository.PaymentRepository;
import com.org.stripepaymentapp.repository.SessionInfoRepository;
import com.org.stripepaymentapp.service.PaymentService;
import com.org.stripepaymentapp.service.StripeService;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    @Autowired
    private PaymentRepository paymentRepository;

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

    @PostMapping("/account_session")
    public ResponseEntity<Map<String, String>> createPaymentAccount(@RequestBody AccountSessionRequest accountSessionRequest) {
        try {
            String clientSecret = stripeService.activateAccount(accountSessionRequest.getAccountId());

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

    @DeleteMapping("/delete-accounts")
    public Map<String, Object> deleteAccounts(@RequestBody List<String> accountIds) {
        return stripeService.deleteAccounts(accountIds);
    }



    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        Map<String, Object> response = new HashMap<>();

        try {
            // Retrieve the session from Stripe to verify the payment
            Session session = Session.retrieve(sessionId);
            // Get the PaymentIntent ID from the session


            if (session.getStatus().equalsIgnoreCase("complete")) {

                String paymentIntentId = session.getPaymentIntent();

                // Retrieve the PaymentIntent and get the charge ID
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                String chargeId = paymentIntent.getLatestCharge();

                // Optionally, you can update your database or perform any necessary actions based on the session status
                paymentService.updatePaymentStatus(sessionId,chargeId);

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
    public ResponseEntity<Map<String, String>> refundPayment(@RequestBody String jobId) {

        try {
            String refundId ="";
            Optional<Payment> paymentList = paymentRepository.findByJobId(jobId);

            if(paymentList.isPresent()){
                Payment payment = paymentList.get();
                 refundId= stripeService.refundPayment(payment.getChargeId(), payment.getAmount());
            }

            // Create a JSON response with the paymentUrl
            Map<String, String> response = new HashMap<>();

            response.put("refundId", refundId);


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return an error message as JSON as well
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }

    }



}