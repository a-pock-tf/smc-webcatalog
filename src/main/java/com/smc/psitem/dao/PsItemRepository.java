package com.smc.psitem.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.psitem.model.PsItem;


/**
 *  Repositoryでできることはこちらで
 */
@Repository
@Scope("session")
public interface PsItemRepository extends MongoRepository<PsItem, String>, PsItemTemplate {

	public Optional<PsItem> findById(String id);

	public Optional<PsItem> findByName(String name);

	public Optional<PsItem> findBySid(String sid);

	public List<DiscontinuedCategory> findAllByLang(String lang);

	public List<PsItem> findAllBySid(String sid);

	public void deleteAllByLang(String lang);

}
