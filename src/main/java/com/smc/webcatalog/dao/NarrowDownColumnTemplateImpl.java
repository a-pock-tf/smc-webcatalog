package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.NarrowDownColumn;

public class NarrowDownColumnTemplateImpl implements NarrowDownColumnTemplate{

	@Autowired
	private MongoTemplate db;

	@Override
	public List<NarrowDownColumn> findByCategoryId(String categoryId, Boolean active) {
		Query query = new Query(where("categoryId").is(categoryId));
		
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<NarrowDownColumn> ret = db.find(query, NarrowDownColumn.class);

		return ret;
	}
	@Override
	public List<NarrowDownColumn> findAllByCategoryKeys(String categoryId, String[] keys, Boolean active) {
		Query query = new Query(where("categoryId").is(categoryId));
		query.addCriteria(where("title").in((Object[])keys));
		
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<NarrowDownColumn> ret = db.find(query, NarrowDownColumn.class);
		return ret;
	}	
	// =================== private ===================
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}


	// ソートを付与
	private void addSortOrder(Query q, boolean isAsc, String param) {
		if (isAsc) {
			q.with(Sort.by(Sort.Direction.ASC, param));
		} else {
			q.with(Sort.by(Sort.Direction.DESC, param));
		}
	}


}
