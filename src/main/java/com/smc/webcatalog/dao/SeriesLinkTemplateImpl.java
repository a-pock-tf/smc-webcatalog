package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.SeriesLink;

public class SeriesLinkTemplateImpl implements SeriesLinkTemplate{

	@Autowired
	private MongoTemplate db;


	@Override
	public List<SeriesLink> listAll(Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<SeriesLink> list = db.find(query, SeriesLink.class);
		return list;
	}

	@Override
	public List<SeriesLink> findBySeriesId(String id) {
		Query query = new Query();
		if (id != null) {
			addSeriesIdQuery(query, id);
		}
		List<SeriesLink> list = db.find(query, SeriesLink.class);
		return list;
	}

	@Override
	public void deleteBySeriesId(String id) {
		Query query = new Query();
		if (id != null) {
			addSeriesIdQuery(query, id);
		}
		db.findAllAndRemove(query, SeriesLink.class);
	}


	// =================== private ===================
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}

	// SeriesID の検索を付与
	private void addSeriesIdQuery(Query q, String id) {
		if (id != null) {
			q.addCriteria(where("seriesId").is(id));
		}
	}

}
