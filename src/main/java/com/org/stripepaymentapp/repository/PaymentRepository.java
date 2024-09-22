package com.org.stripepaymentapp.repository;

import com.org.stripepaymentapp.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {


}
