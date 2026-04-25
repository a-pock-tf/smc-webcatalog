package com.smc.webcatalog.dao;

import java.util.List;

import com.smc.webcatalog.model.NarrowDownCompare;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface NarrowDownCompareTemplate {

	List<NarrowDownCompare> findByCategoryId(String categoryId, Boolean active);
	
}
