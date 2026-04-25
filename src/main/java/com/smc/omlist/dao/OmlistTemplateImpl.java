package com.smc.omlist.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.omlist.model.Omlist;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public class OmlistTemplateImpl implements OmlistTemplate {

	@Autowired
	private MongoTemplate db;

	@Override
	public List<Omlist> listAll(String lang, Boolean active) {
		Query query = new Query();
		if (active != null) {
			addActiveQuery(query, active);
		}
		List<Omlist> list = db.find(query, Omlist.class);
		return list;
	}

	@Override
	public List<Omlist> search(List<String> keyList, String category, String div, String series, String lang, Boolean active) {
		Query query = new Query();
		String keyword = "";
		if (keyList != null) {
			for(String key : keyList) {
					keyword += key.trim() + "|";
			}
			if (keyword.indexOf("□") > -1) {
				keyword = keyword.replaceAll("□", ".*");
			}
			keyword = keyword.substring(0, keyword.length()-1);
			query.addCriteria(where("spec").regex(keyword, "i")
					.orOperator(where("kata").regex(keyword, "i"))
					);
		}

		addQuery(query, category, div, series, lang, active);
		List<Omlist> list = db.find(query, Omlist.class);

		return list;
	}

	// =================== private ===================
	private void addQuery(Query query, String category, String div, String series, String lang, Boolean active) {
		if (category != null && category.isEmpty() == false) {
			query.addCriteria(where("category").is(category));
		}
		if (div != null && div.isEmpty() == false) {
			query.addCriteria(where("div").is(div));
		}
		if (series != null && series.isEmpty() == false) {
			query.addCriteria(where("ids").regex("【"+series+"】", "i"));
		}
		if (lang != null && lang.isEmpty() == false) {
			query.addCriteria(where("lang").is(lang));
		}
		if (active != null) {
			query.addCriteria(where("active").is(active));
		}

	}
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}

}
