package com.org.stripepaymentapp.service;

import com.org.stripepaymentapp.dto.ConnectAccountRequest;
import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.*;
import com.org.stripepaymentapp.model.Account;
import com.org.stripepaymentapp.repository.*;
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
import java.util.*;


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
    private ServiceRepository serviceRepository;

    @Autowired
    private SessionInfoRepository sessionInfoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }


    @Transactional
    public String createPaymentLink(PaymentLinkRequest paymentLinkRequest) throws Exception {

        long amountInCents = (long) (paymentLinkRequest.getAmount() * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
                .setReturnUrl("http://localhost:4200/return?session_id={CHECKOUT_SESSION_ID}")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(paymentLinkRequest.getCurrency()) // Set your desired currency
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(paymentLinkRequest.getJobName()) // Set product name
                                                                .build()
                                                )
                                                .setUnitAmount(amountInCents) // Amount in cents (e.g., 10.00 USD = 1000)
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .build();

        Session session = Session.create(params);

        String jobId = updateJobInfo(paymentLinkRequest);
        updatePaymentInfo(paymentLinkRequest, session, jobId);

        return session.getClientSecret();


    }

    public String refundPayment(String chargeId, double amount) {

        String refundId = "";
        long amountInCents = (long) (amount * 100 * 0.8);  // Converts to cents
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

    public String reverseTransfer(String transferId, double amount) {
        String reversalId = "";
        long amountInCents = (long) (amount * 100 * 0.8);  // Converts to cents
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

    public String createConnectedAccount(ConnectAccountRequest connectedAccountRequest) {
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

            com.stripe.model.Account account = com.stripe.model.Account.create(params);
            accountId = account.getId();

            createService(accountId);

            // Print or log successful setup
            System.out.println("Connected account activated with ID: " + account.getId() + " and bank account ID: ");

        } catch (StripeException e) {
            e.printStackTrace();
        }

        return accountId;
    }

    private void createService(String accountId) {

        com.org.stripepaymentapp.model.Service service = new com.org.stripepaymentapp.model.Service();
        service.setServiceProviderID(accountId);
        serviceRepository.save(service);

    }

    private void updateService(String accountId, String currency){
        Optional<com.org.stripepaymentapp.model.Service> serviceOpt =serviceRepository.findByServiceProviderID(accountId);
        if(serviceOpt.isPresent()){
            com.org.stripepaymentapp.model.Service service = serviceOpt.get();
            service.setCurrency(currency);
            serviceRepository.save(service);
        }
    }

    private void createUser(String name, String dob, String address, String country, String accountId, String phone){
        User user = new User();
        user.setName(name);
        user.setDob(dob);
        user.setPhone(phone);
        user.setStripeId(accountId);
        user.setAddress(address);
        user.setCountry(country);
        userRepository.save(user);

    }

    private void createBankAccount(String accountId, ExternalAccountCollection externalAccountCollection) {
        Account accountInfo = new Account();
        accountInfo.setAccountId(accountId);
        accountInfo.setRouteNumber(((BankAccount)externalAccountCollection.getData().get(0)).getRoutingNumber());
        accountInfo.setAccountName(((BankAccount)externalAccountCollection.getData().get(0)).getAccountHolderName());
        accountRepository.save(accountInfo);

    }

    public void getAccountDetails(String accountId) {

        try {
            // Retrieve account details
            com.stripe.model.Account account = com.stripe.model.Account.retrieve(accountId);

            // Extract required information
            String name = account.getIndividual() != null ? account.getIndividual().getFirstName() + " " + account.getIndividual().getLastName() : "N/A";
            String dob = account.getIndividual() != null && account.getIndividual().getDob() != null
                    ? account.getIndividual().getDob().getYear() + "-" + account.getIndividual().getDob().getMonth() + "-" + account.getIndividual().getDob().getDay()
                    : "N/A";
            String phone = account.getIndividual() != null ? account.getIndividual().getPhone() : "N/A";
            String homeAddress = account.getIndividual() != null && account.getIndividual().getAddress() != null
                    ? account.getIndividual().getAddress().getLine1() + ", " + account.getIndividual().getAddress().getCity() + ", " + account.getIndividual().getAddress().getCountry()
                    : "N/A";
            String currency = account.getDefaultCurrency();
            String countryOfBank = account.getCountry();
            updateService(accountId, currency);
            createUser(name, dob, homeAddress, countryOfBank, accountId, phone);
            createBankAccount(accountId, account.getExternalAccounts());

        } catch (Exception e) {
            System.err.println("Error retrieving account details: " + e.getMessage());
        }
    }

    public Map<String, Object> deleteAccounts(List<String> accountIds) {
        Stripe.apiKey = stripeApiKey;
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (String accountId : accountIds) {
            try {
                com.stripe.model.Account account = com.stripe.model.Account.retrieve(accountId);
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
        AccountSessionCreateParams params =
                AccountSessionCreateParams.builder()
                        .setAccount(accountId)
                        .setComponents(
                                AccountSessionCreateParams.Components.builder()
                                        .setAccountOnboarding(
                                                AccountSessionCreateParams.Components.AccountOnboarding.builder()
                                                        .setEnabled(true)
                                                        .build()
                                        )
                                        .setPayments(
                                                AccountSessionCreateParams.Components.Payments.builder()
                                                        .setEnabled(true)
                                                        .build()
                                        )
                                        .setPayouts(
                                                AccountSessionCreateParams.Components.Payouts.builder()
                                                        .setEnabled(true)
                                                        .build()
                                        )
                                        .setBalances(
                                                AccountSessionCreateParams.Components.Balances.builder()
                                                        .setEnabled(true)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        AccountSession accountSession = AccountSession.create(params);
        return accountSession.getClientSecret();
    }


    private void updatePaymentInfo(PaymentLinkRequest paymentLinkRequest, Session session, String jobId) {
        Payment payment = new Payment();
        payment.setSessionId(session.getId());
        payment.setAmount(paymentLinkRequest.getAmount());
        payment.setStatus("PENDING");
        payment.setCurrency(paymentLinkRequest.getCurrency());
        payment.setJobId(jobId);
        paymentRepository.save(payment);

    }

    private String updateJobInfo(PaymentLinkRequest paymentLinkRequest) {

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
        long amountInCents = (long) (amount * 100 * 0.8);  // Converts to cents


        double appCharge = amount * 100*0.2; // App charge in base currency

        Map<String, String> metadata = new HashMap<>();
        metadata.put("app_charge (20%)", String.valueOf(appCharge)); // Store app charge details

        Map<String, Object> transferParams = new HashMap<>();
        transferParams.put("amount", amountInCents); // Amount in cents
        transferParams.put("currency", currency);
        transferParams.put("destination", connectedAccountId);
        transferParams.put("metadata", metadata);


        // Perform the transfer
        Transfer transfer = Transfer.create(transferParams);

        // Get and return the transfer ID
        String transferId = transfer.getId();
        System.out.println("Transfer created with ID: " + transferId);
        return transferId;
    }
}