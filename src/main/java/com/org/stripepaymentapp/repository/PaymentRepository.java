package com.org.stripepaymentapp.repository;


import com.org.stripepaymentapp.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findBySessionId(String sessionId);
    Optional<Payment> findByJobId(String jobId);




}
