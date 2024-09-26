package com.org.stripepaymentapp.repository;

import com.org.stripepaymentapp.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByServiceId(long customerId);
}
