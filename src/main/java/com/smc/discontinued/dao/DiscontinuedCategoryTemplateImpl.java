package com.smc.discontinued.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import com.mongodb.lang.Nullable;
import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.webcatalog.model.Category;

public class DiscontinuedCategoryTemplateImpl implements DiscontinuedCategoryTemplate {


	@Autowired
	private MongoTemplate db;

	@Override
	public List<DiscontinuedCategory>listAll(String lang, DiscontinuedModelState state, @Nullable Boolean active) {

		Query query = new Query(
				where("lang").is(lang));
		addStateQuery(query, state);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, DiscontinuedCategory.class);

	}

	@Override
	public Optional<DiscontinuedCategory> findByName(String name, String lang, DiscontinuedModelState state) {
		Query query = new Query(where("name").is(name));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedCategory> list = db.find(query, DiscontinuedCategory.class);
		DiscontinuedCategory c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public Optional<DiscontinuedCategory> findBySlug(String slug, String lang, DiscontinuedModelState state, @Nullable Boolean active) {
		Query query = new Query(where("slug").is(slug));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedCategory> list =db.find(query, DiscontinuedCategory.class);
		DiscontinuedCategory c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}
	@Override
	public Optional<DiscontinuedCategory> findByOldId(String oldId, String lang, DiscontinuedModelState state, @Nullable Boolean active) {
		Query query = new Query(where("oldId").is(oldId));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		if (active != null) {
			addActiveQuery(query, active);
		}

		List<DiscontinuedCategory> list =db.find(query, DiscontinuedCategory.class);
		DiscontinuedCategory c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}
	@Override
	public Optional<DiscontinuedCategory> findByStateRefId(String id, DiscontinuedModelState state) {
		Query query = new Query(where("stateRefId").is(id));
		addStateQuery(query, state);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedCategory> list = db.find(query, DiscontinuedCategory.class);
		DiscontinuedCategory c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public List<DiscontinuedCategory> findByLangRefId(String id) {
		Query query = new Query(where("langRefId").is(id));
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, DiscontinuedCategory.class);
	}


	@Override
	public List<DiscontinuedCategory> search(String keyword, String lang, DiscontinuedModelState state, Boolean active) {
		Query query = new Query(where("name").regex(keyword));
		addLangQuery(query, lang);
		addStateQuery(query, state);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, DiscontinuedCategory.class);
	}

	/**
	 * Rootカテゴリを作成
	 * @return testId StatusがTESTのIDを返す
	 */
	public String createRootCategory(String lang, DiscontinuedModelState state, String stateRefId, String langRefId) {

		String ret = null;
		// if not exists
		Query query = new Query(where("parentId").is(""));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		List<Category> list = db.find(query, Category.class);

		if (list == null || list.size() > 0) {
			DiscontinuedCategory c = new DiscontinuedCategory();
			c.setState(state);
			c.setName("root");
			c.setSlug("root");
			c.setLang(lang);
			if (state.equals(DiscontinuedModelState.TEST) && StringUtils.isEmpty(stateRefId) == false) c.setStateRefId(stateRefId);
			if (langRefId != null && langRefId.equals("") == false) c.setLangRefId(langRefId);
			c = db.save(c);
			if (c != null) {
				ret = c.getId();
			}
		}

		return ret;
	}


	// =================== private ===================
	// active の検索を付与
	private void addActiveQuery(Query q, Boolean active) {
		if (active != null) {
			q.addCriteria(where("active").is(active));
		}
	}

	private void addStateQuery(Query q, DiscontinuedModelState state) {
		if (state != null) {
			q.addCriteria(where("state").is(state));
		}
	}
	private void addLangQuery(Query q, String lang) {
		if (lang != null) {
			q.addCriteria(where("lang").is(lang));
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
