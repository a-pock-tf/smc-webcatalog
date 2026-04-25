package com.smc.discontinued.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;

/**
 *  Repositoryでできることはこちらで
 */
@Repository
@Scope("session")
public interface DiscontinuedSeriesRepository extends MongoRepository<DiscontinuedSeries, String>, DiscontinuedSeriesTemplate {

	public Optional<DiscontinuedSeries> findById(String id);

	public Optional<DiscontinuedSeries> findByName(String name);

	public Optional<DiscontinuedSeries> findBySeries(String series, DiscontinuedModelState state);

	public Optional<DiscontinuedSeries> findByStateRefId(String refId);

	public List<DiscontinuedSeries> findAllByCategoryId(String categoryId);

	public List<DiscontinuedSeries> findAllByLang(String lang);
	// TEST 独自Query ( regexのパラメーターには/をつけない)
	/*
	@Query("{name : { $regex : ?0 } }")
	public Page<Category> findByQueryWithExpression(String param0,Pageable pageable);
	*/

}
