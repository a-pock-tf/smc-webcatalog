package com.smc.webcatalog.dao;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String>, UserTemplate {

	Optional<User> findByLoginId(String loginid);

	Optional<User> findByEmail(String email);

	Optional<User> findByName(String name);

}
