package com.org.stripepaymentapp.repository;

import com.org.stripepaymentapp.model.Payment;
import com.org.stripepaymentapp.model.SessionInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SessionInfoRepository extends MongoRepository<SessionInfo, String> {


    Optional<SessionInfo> findBySessionId(String s);
}
