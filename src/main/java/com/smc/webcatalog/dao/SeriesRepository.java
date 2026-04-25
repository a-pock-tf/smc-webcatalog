package com.smc.webcatalog.dao;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.Series;

@Repository
@Scope("session")
public interface SeriesRepository extends MongoRepository<Series, String>,SeriesTemplate {

	Optional<Series> findByStateRefId(String id);
}
