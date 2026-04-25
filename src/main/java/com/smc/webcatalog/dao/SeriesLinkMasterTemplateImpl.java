package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.SeriesLinkMaster;

public class SeriesLinkMasterTemplateImpl implements SeriesLinkMasterTemplate{

	@Autowired
	private MongoTemplate db;


	@Override
	public List<SeriesLinkMaster> listAll(Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");

		List<SeriesLinkMaster> list = db.find(query, SeriesLinkMaster.class);
		return list;
	}

	@Override
	public List<SeriesLinkMaster> findAllByLang(String lang, Boolean active) {
		Query query = new Query();
		addLangQuery(query, lang);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");

		List<SeriesLinkMaster> list = db.find(query, SeriesLinkMaster.class);
		return list;
	}

	// =================== private ===================
	private void addLangQuery(Query q, String lang) {
		if (lang != null) {
			q.addCriteria(where("lang").is(lang));
		}
	}
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}
	// ソートを付与
	private void addSortOrder(Query q,boolean isAsc, String param) {
		if (isAsc) {
			q.with(Sort.by(Sort.Direction.ASC, param));
		}
		else {
			q.with(Sort.by(Sort.Direction.DESC, param));
		}
	}

}
