package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ModelState;

/**
 *  Repositoryでできることはこちらで
 */
@Repository
@Scope("session")
public interface CategoryRepository extends MongoRepository<Category, String>, CategoryTemplate {

	public Optional<Category> findById(String id);

	public Optional<Category> findByName(String name);

	public Optional<Category> findBySlug(String slug);

	public Optional<Category> findByStateRefId(String refId);

	public Optional<Category> findByOldIdAndStateAndType(String oldId, ModelState state, CategoryType type);

	public List<Category> findByLangAndStateAndType(String lang, ModelState state, CategoryType type);
	// TEST 独自Query ( regexのパラメーターには/をつけない)
	/*
	@Query("{name : { $regex : ?0 } }")
	public Page<Category> findByQueryWithExpression(String param0,Pageable pageable);
	*/

}
