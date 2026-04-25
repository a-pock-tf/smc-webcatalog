package com.smc.webcatalog.dao;

import java.util.List;

import com.smc.webcatalog.model.NarrowDownColumn;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface NarrowDownColumnTemplate {

	List<NarrowDownColumn> findByCategoryId(String categoryId, Boolean active);
	
	List<NarrowDownColumn> findAllByCategoryKeys(String categoryId, String[] keys, Boolean active);
	
}
