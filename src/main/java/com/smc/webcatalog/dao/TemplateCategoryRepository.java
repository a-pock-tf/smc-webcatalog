package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.TemplateCategory;

@Repository
@Scope("session")
public interface TemplateCategoryRepository  extends MongoRepository<TemplateCategory, String> {
	
	Optional<TemplateCategory> findById(String id);
	
	Optional<TemplateCategory> findByCategoryId(String categoryId);
	
	List<TemplateCategory> findAllByCategoryId(String categoryId);
	
	Optional<TemplateCategory> findByStateRefId(String stateRefId);

	Optional<TemplateCategory> findByHeartCoreID(String heartCoreId);

	List<TemplateCategory> findAllByHeartCoreID(String heartCoreId);

}
