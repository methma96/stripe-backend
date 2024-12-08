package com.org.stripepaymentapp.repository;

import com.org.stripepaymentapp.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<Account, String> {
}
