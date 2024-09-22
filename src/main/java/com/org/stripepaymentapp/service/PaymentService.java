package com.org.stripepaymentapp.service;



import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Job;
import com.org.stripepaymentapp.repository.JobRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentLink;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private JobRepository jobRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String createPaymentLink(PaymentLinkRequest paymentLinkRequest) throws Exception {

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice("price_1Py25KIhwUT0ZWX42Fpet9ZY")
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setPaymentIntentData(
                                SessionCreateParams.PaymentIntentData.builder()
                                        .setApplicationFeeAmount(123L)
                                        .setTransferData(
                                                SessionCreateParams.PaymentIntentData.TransferData.builder()
                                                        .setDestination("acct_1Pxof7IMEeLOr8zc")
                                                        .build()
                                        )
                                        .build()
                        )
                        .setSuccessUrl("https://example.com/success")
                        .setCancelUrl("https://example.com/cancel")
                        .build();

        Session session = Session.create(params);

        return session.getUrl();

    }

}
