package com.smc.webcatalog.dao;

import java.util.List;

import com.smc.webcatalog.model.SeriesLinkMaster;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface SeriesLinkMasterTemplate {

	List<SeriesLinkMaster> listAll(Boolean active);

	List<SeriesLinkMaster> findAllByLang(String lang, Boolean active);
}
