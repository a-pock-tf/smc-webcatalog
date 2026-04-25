package com.smc.webcatalog.dao;

import java.util.List;

import com.smc.webcatalog.model.Lang;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface LangTemplate {

	List<Lang> listAll(Boolean active);
	
	List<Lang> listWithoutVersion();
}
