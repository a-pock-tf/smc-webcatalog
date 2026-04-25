package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.NarrowDownValue;

public class NarrowDownValueTemplateImpl implements NarrowDownValueTemplate{

	@Autowired
	private MongoTemplate db;

	@Override
	public List<NarrowDownValue> search(List<NarrowDownColumn> columns, HashMap<String, List<String>> map) {
		Query query = new Query();
		List<Criteria> orList = new ArrayList<>();
		for (NarrowDownColumn c : columns) {
			List<String> vals = map.get(c.getId());
			
			List<Criteria> cList = new ArrayList<>();
			cList.add(where("columnId").is(c.getId()));
			
			if (c.getSelect().equals("range")) {
				cList.add(where("start").gte(vals.get(0)));
				cList.add(where("end").lte(vals.get(0)));
			} else if (c.getSelect().equals("checkbox")) {
				// or  preview のnarrowdown 表示
				List<Criteria> chkList = new ArrayList<>();
				for (String val : vals) {
					chkList.add(where("param").in(val));
				}
				Criteria cr = new Criteria();
				cList.add(cr.orOperator(chkList));
			} else { // radio or select
				cList.add(where("param").in(vals.get(0)));
			}
			if (cList.size() > 0) {
				Criteria cr = new Criteria();
				orList.add(cr.andOperator(cList));
			}
		}
		if (orList.size() > 0) {
			Criteria cr = new Criteria();
			query.addCriteria(cr.orOperator(orList));
		}
		List<NarrowDownValue> ret = db.find(query, NarrowDownValue.class);
		return ret;
	}

	@Override
	public List<NarrowDownValue> findAllByRange(String columnId, String val) {
		Query query = new Query(where("columnId").is(columnId));
		query.addCriteria(where("start").gte(val));
		query.addCriteria(where("end").lte(val));
		
		List<NarrowDownValue> ret = db.find(query, NarrowDownValue.class);
		return ret;
	}


	@Override
	public List<NarrowDownValue> findAllByValue(String columnId, String val) {
		Query query = new Query(where("columnId").is(columnId));
		query.addCriteria(where("param").in(val));
		
		List<NarrowDownValue> ret = db.find(query, NarrowDownValue.class);
		return ret;
	}


	@Override
	public List<NarrowDownValue> findAllByValues(String columnId, String[] vals) {
		Query query = new Query(where("columnId").is(columnId));
		List<Criteria> orList = new ArrayList<>();
		for (String v : vals) {
			orList.add(where("param").in(v));
		}
		if (orList.size() > 0) {
			Criteria cri = new Criteria();
			query.addCriteria(cri.orOperator(orList));
		}
		List<NarrowDownValue> ret = db.find(query, NarrowDownValue.class);
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
 