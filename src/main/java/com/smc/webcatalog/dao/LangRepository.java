package com.smc.webcatalog.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.Lang;

@Repository
@Scope("session")
public interface LangRepository  extends MongoRepository<Lang, String>, LangTemplate {
	Optional<Lang> findByName(String name);
}
