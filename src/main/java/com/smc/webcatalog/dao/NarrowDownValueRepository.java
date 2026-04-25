package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.NarrowDownValue;

@Repository
@Scope("session")
public interface NarrowDownValueRepository  extends MongoRepository<NarrowDownValue, String>, NarrowDownValueTemplate {
	List<NarrowDownValue> findAllBySeriesId(String seriesId);
	
	List<NarrowDownValue> findAllByColumnId(String columnId);
	
	List<NarrowDownValue> findAllByCategoryIdAndSeriesId(String categoryId, String seriesId);
	
	Optional<NarrowDownValue> findByCategoryId(String categoryId);

	Optional<NarrowDownValue> findByStateRefId(String columnId);

	void deleteBySeriesId(String seriesId);
	
	void deleteByCategoryId(String categoryId);

	void deleteAllByColumnId(String columnId);

	void deleteByStateRefId(String childValueId);
}
