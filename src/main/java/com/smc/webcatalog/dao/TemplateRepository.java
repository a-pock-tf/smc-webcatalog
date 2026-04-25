package com.smc.webcatalog.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.Template;

@Repository
@Scope("session")
public interface TemplateRepository  extends MongoRepository<Template, String>, TemplateTemplate {
	Optional<Template> findByLang(String lang);
	Optional<Template> findByStateRefId(String id);
}
