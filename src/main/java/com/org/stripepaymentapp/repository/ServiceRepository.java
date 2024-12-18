package com.org.stripepaymentapp.repository;



import com.org.stripepaymentapp.model.Service;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.util.Optional;

public interface ServiceRepository extends MongoRepository<Service, String> {

    Optional<Service> findByServiceProviderID(String accountId);
}
