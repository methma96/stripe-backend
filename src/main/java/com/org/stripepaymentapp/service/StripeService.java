package com.org.stripepaymentapp.service;

import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.repository.ServiceRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.ProductListParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private ServiceRepository serviceRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String createPaymentLink(PaymentLinkRequest paymentLinkRequest) throws Exception {

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice("price_1Py25KIhwUT0ZWX42Fpet9ZY") // Replace with your price ID
                                .setQuantity(1L)
                                .build()
                )
                .setSuccessUrl("https://example.com/success")
                .setCancelUrl("https://example.com/cancel")
                .build();

        Session session = Session.create(params);
        return session.getUrl();

    }

    public Transfer transferAmountToServiceProvider(String connectedAccountId, double amount) throws Exception {
        Map<String, Object> transferParams = new HashMap<>();
        transferParams.put("amount", (int) (amount * 100)); // Amount in cents
        transferParams.put("currency", "usd");
        transferParams.put("destination", connectedAccountId);

        return Transfer.create(transferParams);
    }
}