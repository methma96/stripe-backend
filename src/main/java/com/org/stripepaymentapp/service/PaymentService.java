package com.org.stripepaymentapp.service;



import com.org.stripepaymentapp.dto.PaymentLinkRequest;
import com.org.stripepaymentapp.model.Job;
import com.org.stripepaymentapp.model.Payment;
import com.org.stripepaymentapp.repository.JobRepository;
import com.org.stripepaymentapp.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentLink;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JobRepository jobRepository;

    public void updatePaymentStatus(String sessionId, String chargeId) {
        Optional<Payment> paymentOpt = paymentRepository.findBySessionId(sessionId);
        Payment payment;

        if (paymentOpt.isPresent()) {
            payment = paymentOpt.get();
            payment.setStatus("ESCROW");
            payment.setChargeId(chargeId);
            paymentRepository.save(payment);

            Optional<Job> jobOptional = jobRepository.findById(payment.getJobId());

            if (jobOptional.isPresent()) {
                Job job = jobOptional.get();
                job.setPaymentStatus("ESCROW");
                jobRepository.save(job);

            }
        }
    }


}
