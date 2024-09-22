package com.org.stripepaymentapp.repository;

import com.org.stripepaymentapp.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobRepository extends MongoRepository<Job, String> {
}
