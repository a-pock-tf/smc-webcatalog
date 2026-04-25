package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.NarrowDownCompare;

@Repository
@Scope("session")
public interface NarrowDownCompareRepository  extends MongoRepository<NarrowDownCompare, String> , NarrowDownCompareTemplate{

	List<NarrowDownCompare> findAllByCategoryId(String categoryId);
	
	Optional<NarrowDownCompare> findByStateRefId(String childColumnId);
	
	void deleteByCategoryId(String categoryId);
	
	void deleteByStateRefId(String childColumnId);
	
	void deleteById(String id);
}
