package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smc.webcatalog.model.NarrowDownColumn;

@Repository
@Scope("session")
public interface NarrowDownColumnRepository  extends MongoRepository<NarrowDownColumn, String> , NarrowDownColumnTemplate{

	List<NarrowDownColumn> findAllByCategoryId(String categoryId);
	
	Optional<NarrowDownColumn> findByStateRefId(String childColumnId);
	
	void deleteByCategoryId(String categoryId);
	
	void deleteByStateRefId(String childColumnId);
	
	void deleteById(String id);
}
