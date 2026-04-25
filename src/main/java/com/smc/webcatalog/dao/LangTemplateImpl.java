package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.Lang;

public class LangTemplateImpl implements LangTemplate{

	@Autowired
	private MongoTemplate db;


	@Override
	public List<Lang> listAll(Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<Lang> list = db.find(query, Lang.class);
		return list;
	}
	@Override
	public List<Lang> listWithoutVersion(){
		Query query = new Query();
		Criteria cr = new Criteria();
		query.addCriteria(cr.orOperator( where("version").is(null), where("version").is(false)));
//		query.addCriteria(where("version").is(null).orOperator(where("version").is(false)));
		query.addCriteria(where("active").is(true));
		List<Lang> list = db.find(query, Lang.class);
		return list;
	}

	// =================== private ===================
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}


}
