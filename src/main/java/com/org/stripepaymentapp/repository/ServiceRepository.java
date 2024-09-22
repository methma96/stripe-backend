package com.org.stripepaymentapp.repository;


import com.org.stripepaymentapp.model.Service;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServiceRepository extends MongoRepository<Service, String> {
}
