package com.smc.discontinued.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;


/**
 *  Repositoryでできることはこちらで
 */
@Repository
@Scope("session")
public interface DiscontinuedCategoryRepository extends MongoRepository<DiscontinuedCategory, String>, DiscontinuedCategoryTemplate {

	public Optional<DiscontinuedCategory> findById(String id);

	public Optional<DiscontinuedCategory> findByOldId(String oldid);

	public Optional<DiscontinuedCategory> findByName(String name);

	public Optional<DiscontinuedCategory> findBySlug(String slug);

	public Optional<DiscontinuedCategory> findByStateRefId(String refId);

	public List<DiscontinuedCategory> findByLangAndState(String lang, DiscontinuedModelState state);
	// TEST 独自Query ( regexのパラメーターには/をつけない)
	/*
	@Query("{name : { $regex : ?0 } }")
	public Page<Category> findByQueryWithExpression(String param0,Pageable pageable);
	*/

}
