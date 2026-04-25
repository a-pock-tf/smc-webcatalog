package com.smc.webcatalog.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.CategorySeries;

@Repository
@Scope("session")
public interface CategorySeriesRepository extends MongoRepository<CategorySeries, String>,CategorySeriesTemplate {

	Optional<CategorySeries> findByCategoryId(String id);

}
