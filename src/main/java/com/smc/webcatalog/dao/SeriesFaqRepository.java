package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.SeriesFaq;

@Repository
@Scope("session")
public interface SeriesFaqRepository  extends MongoRepository<SeriesFaq, String> {
	
	Optional<SeriesFaq> findById(String categoryId);

	Optional<SeriesFaq> findByModelNumber(String modelNumber);

	Optional<SeriesFaq> findBySeriesId(String seriesId);
	
	List<SeriesFaq> findAllBySeriesId(String seriesId);
	
	void deleteBySeriesId(String seriesId);

}
