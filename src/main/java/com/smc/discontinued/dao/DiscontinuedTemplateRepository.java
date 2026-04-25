package com.smc.discontinued.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.discontinued.model.DiscontinuedTemplate;

@Repository
@Scope("session")
public interface DiscontinuedTemplateRepository  extends MongoRepository<DiscontinuedTemplate, String> {

	Optional<DiscontinuedTemplate> findByLang(String lang);

	Optional<DiscontinuedTemplate> findByHeartCoreID(String heartCoreId);

	List<DiscontinuedTemplate> findAllByHeartCoreID(String heartCoreId);

}
