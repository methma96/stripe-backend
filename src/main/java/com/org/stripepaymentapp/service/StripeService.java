package com.org.stripepaymentapp.service;

import com.org.stripepaymentapp.dto.ConnectAccountRequest;
import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Job;
import com.org.stripepaymentapp.model.Payment;
import com.org.stripepaymentapp.model.SessionInfo;
import com.org.stripepaymentapp.repository.JobRepository;
import com.org.stripepaymentapp.repository.PaymentRepository;
import com.org.stripepaymentapp.repository.SessionInfoRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;

import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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

    @Autowired
    private SessionInfoRepository sessionInfoRepository;

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

    public String createConnectedAccount(ConnectAccountRequest connectedAccountRequest){
        String accountId ="";
        try {
            // Define the parameters for the connected account
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)  // For Standard accounts
                    .setCountry(connectedAccountRequest.getCountryCode())  // The country for the connected account
                    .setEmail(connectedAccountRequest.getEmail())  // Email of the connected account
                    .build();

            // Generate a session ID (UUID or any unique identifier)
            String sessionId = UUID.randomUUID().toString();

// Save account ID and session ID to the database


// Set up the return URL with the session ID as a query parameter
            String returnUrl = "https://example.com/return?session_id=" + sessionId;

            // Create the connected account
            Account account = Account.create(params);
//            accountId = account.getId();

            SessionInfo sessionInfo=new SessionInfo();
            sessionInfo.setAccountId(account.getId());
            sessionInfo.setSessionId(sessionId);
            sessionInfo.setJobId(connectedAccountRequest.getJobId());

            sessionInfoRepository.save(sessionInfo);

            // Step 2: Create an AccountLink (the link for the user to complete their account setup)
            AccountLinkCreateParams accountLinkParams = AccountLinkCreateParams.builder()
                    .setAccount(account.getId())
                    .setRefreshUrl("https://example.com/reauth")  // URL for users to return if the flow is interrupted
                    .setReturnUrl(returnUrl)   // Redirect URL after user completes the flow
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(accountLinkParams);
            accountId= accountLink.getUrl();

            // Print the account ID of the connected account
            System.out.println("Connected account created with ID: " + account.getId());

        } catch (StripeException e) {
            e.printStackTrace();
        }

        return accountId;
    }

    private void updatePaymentInfo(PaymentLinkRequest paymentLinkRequest, Session session, String jobId){
        Payment payment=new Payment();
        payment.setSessionId(session.getId());
        payment.setAmount(paymentLinkRequest.getAmount());
        payment.setStatus("PENDING");
        payment.setCurrency(paymentLinkRequest.getCurrency());
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
        newJob.setCurrency(paymentLinkRequest.getCurrency());
        jobRepository.save(newJob);

        return newJob.getId();

    }



    public Transfer transferAmountToServiceProvider(String connectedAccountId, double amount, String currency) throws Exception {
        Map<String, Object> transferParams = new HashMap<>();
        transferParams.put("amount",  (amount * 0.8)); // Amount in cents
        transferParams.put("currency", currency);
        transferParams.put("destination", connectedAccountId);

        return Transfer.create(transferParams);
    }
}