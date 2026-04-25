package com.smc.webcatalog.dao;

import java.util.List;

import com.smc.webcatalog.model.SeriesLink;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface SeriesLinkTemplate {

	List<SeriesLink> listAll(Boolean active);

	List<SeriesLink> findBySeriesId(String id);

	void deleteBySeriesId(String id);
}
