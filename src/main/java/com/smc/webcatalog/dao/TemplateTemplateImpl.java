package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Template;

public class TemplateTemplateImpl implements TemplateTemplate {

	@Autowired
	private MongoTemplate db;

	@Override
	public Optional<Template> findByLangAndModelState(String lang, ModelState m, Boolean active) {
		Template c = null;
		Query query = new Query();
		addStateQuery(query, m);
		addLangQuery(query, lang);
		if (active != null) addActiveQuery(query, active);

		List<Template> list = db.find(query, Template.class);
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	// =================== private ===================
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}
	private void addStateQuery(Query q, ModelState state) {
		if (state != null) {
			if (state.equals(ModelState.PROD)) {
				q.addCriteria(where("state").is("PROD"));
			} else if (state.equals(ModelState.ARCHIVE)) {
				q.addCriteria(where("state").is("ARCHIVE"));
			} else {
				q.addCriteria(where("state").is("TEST"));
			}
		}
	}
	private void addLangQuery(Query q, String lang) {
		if (lang != null) {
			q.addCriteria(where("lang").is(lang));
		}
	}

}
