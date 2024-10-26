package com.enhanceai.platform.repository;

import com.enhanceai.platform.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    User findFirstByName(String name);

}

