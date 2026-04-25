package com.smc.omlist.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.omlist.model.Omlist;


/**
 *  Repositoryでできることはこちらで
 */
@Repository
@Scope("session")
public interface OmlistRepository extends MongoRepository<Omlist, String>, OmlistTemplate {

	public Optional<Omlist> findById(String id);

	public Optional<Omlist> findBySpec(String spec);

	public Optional<Omlist> findByKata(String kata);

	public Optional<Omlist> findByCategory(String category);

	public void deleteAllByLang(String lang);

}
