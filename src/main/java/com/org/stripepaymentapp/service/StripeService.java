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

import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
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

    public String refundPayment(String chargeId, double amount){

        String refundId = "";
        long amountInCents = (long) (amount * 100*0.8);  // Converts to cents
        try {
            // Define refund parameters
            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setCharge(chargeId)     // ID of the charge to refund
                    .setAmount(amountInCents)       // Amount to refund (in cents)
                    .build();

            // Create the refund, which goes back to the customer’s original payment method
            Refund refund = Refund.create(refundParams);
            refundId = refund.getId();
            System.out.println("Refund created with ID: " + refundId);

        } catch (StripeException e) {
            e.printStackTrace();
        }
        return refundId;
    }

    public String reverseTransfer(String transferId , double amount) {
        String reversalId = "";
        long amountInCents = (long) (amount * 100*0.8);  // Converts to cents
        try {
            // Retrieve the original transfer
            Transfer transfer = Transfer.retrieve(transferId);

            TransferReversalCollectionCreateParams params =
                    TransferReversalCollectionCreateParams.builder().setAmount(amountInCents).build();
            TransferReversal transferReversal = transfer.getReversals().create(params);
            reversalId = transferReversal.getId();

        } catch (StripeException e) {
            e.printStackTrace();
        }
        return reversalId;
    }

    public String createConnectedAccount(ConnectAccountRequest connectedAccountRequest){
        String accountId = "";
        try {
            // Step 1: Create the connected account (already done in your method)
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry(connectedAccountRequest.getCountryCode())
                    .setDefaultCurrency(connectedAccountRequest.getCurrency())
                    .setEmail(connectedAccountRequest.getEmail())
                    .setCapabilities(
                            AccountCreateParams.Capabilities.builder()
                                    .setCardPayments(
                                            AccountCreateParams.Capabilities.CardPayments.builder()
                                                    .setRequested(true)
                                                    .build()
                                    )
                                    .setTransfers(
                                            AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build()
                                    )
                                    .build()
                    )
                    .build();

            Account account = Account.create(params);
            accountId = account.getId();

            // Print or log successful setup
            System.out.println("Connected account activated with ID: " + account.getId() + " and bank account ID: ");

        } catch (StripeException e) {
            e.printStackTrace();
        }

        return accountId;
    }

    public Map<String, Object> deleteAccounts(List<String> accountIds) {
        Stripe.apiKey = stripeApiKey;
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (String accountId : accountIds) {
            try {
                Account account = Account.retrieve(accountId);
                account.delete();
                successCount++;
            } catch (StripeException e) {
                failureCount++;
                result.put(accountId, "Failed to delete: " + e.getMessage());
            }
        }

        result.put("summary", Map.of("deleted", successCount, "failed", failureCount));
        return result;
    }



    public String activateAccount(String accountId) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("account", accountId);

        Map<String, Object> payments = new HashMap<>();
        payments.put("enabled", true);

        Map<String, Object> features = new HashMap<>();
        features.put("refund_management", true);
        features.put("dispute_management", true);
        features.put("capture_payments", true);
        payments.put("features", features);

        Map<String, Object> components = new HashMap<>();
        components.put("payments", payments);
        params.put("components", components);

        AccountSession accountSession = AccountSession.create(params);

        return accountSession.getClientSecret();
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



    public String transferAmountToServiceProvider(String connectedAccountId, double amount, String currency) throws Exception {
        // Convert the amount to cents (Stripe expects the amount in the smallest currency unit)
        long amountInCents = (long) (amount * 100*0.8);  // Converts to cents

        Map<String, Object> transferParams = new HashMap<>();
        transferParams.put("amount", amountInCents); // Amount in cents
        transferParams.put("currency", currency);
        transferParams.put("destination", connectedAccountId);

        // Perform the transfer
        Transfer transfer = Transfer.create(transferParams);

        // Get and return the transfer ID
        String transferId = transfer.getId();
        System.out.println("Transfer created with ID: " + transferId);
        return transferId;
    }
}