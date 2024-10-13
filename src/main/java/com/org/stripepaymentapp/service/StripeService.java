package com.org.stripepaymentapp.service;

import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Job;
import com.org.stripepaymentapp.model.Payment;
import com.org.stripepaymentapp.repository.JobRepository;
import com.org.stripepaymentapp.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;

import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@Service
public class StripeService {



    /**
     * Get the price ID from Stripe based on the product name.
     *
     * @param productName The name of the product.
     * @return The price ID if found, or null if not.
     * @throws StripeException if the API call fails.
     */
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }


    @Transactional
    public String createPaymentLink(PaymentLinkRequest paymentLinkRequest) throws Exception {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
                .setReturnUrl("http://localhost:4200/return?session_id={CHECKOUT_SESSION_ID}")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(paymentLinkRequest.getPriceId()) // Replace with your price ID
                                .setQuantity(1L)
                                .build()
                )
                .build();


        Session session = Session.create(params);


        String jobId = updateJobInfo(paymentLinkRequest);
        updatePaymentInfo(paymentLinkRequest, session, jobId);

        return session.getClientSecret();

    }

    private void updatePaymentInfo(PaymentLinkRequest paymentLinkRequest, Session session, String jobId){
        Payment payment=new Payment();
        payment.setSessionId(session.getId());
        payment.setAmount(paymentLinkRequest.getAmount());
        payment.setStatus("PENDING");
        payment.setJobId(jobId);
        paymentRepository.save(payment);

    }

    private String updateJobInfo(PaymentLinkRequest paymentLinkRequest){

        Job newJob = new Job();
        newJob.setServiceProviderId(paymentLinkRequest.getServiceProviderId());
        newJob.setPriceId(paymentLinkRequest.getPriceId());
        newJob.setStatus("PENDING");
        newJob.setPaymentStatus("PENDING");
        newJob.setAmount(paymentLinkRequest.getAmount());
        newJob.setName(paymentLinkRequest.getJobName());
        jobRepository.save(newJob);

        return newJob.getId();

    }



    public Transfer transferAmountToServiceProvider(String connectedAccountId, double amount) throws Exception {
        Map<String, Object> transferParams = new HashMap<>();
        transferParams.put("amount", (int) (amount * 100)); // Amount in cents
        transferParams.put("currency", "usd");
        transferParams.put("destination", connectedAccountId);

        return Transfer.create(transferParams);
    }
}