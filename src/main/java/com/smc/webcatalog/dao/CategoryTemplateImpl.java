package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import com.mongodb.lang.Nullable;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ModelState;

public class CategoryTemplateImpl implements CategoryTemplate {


	@Autowired
	private MongoTemplate db;

	@Override
	public List<Category> findByParentId(String parentId, ModelState state, CategoryType type, Boolean active) {

		Query query = new Query(where("parentId").is(parentId));
		addStateQuery(query, state);
		addTypeQuery(query, type);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, true, "id");

		return db.find(query, Category.class);
	}

	@Override
	public List<Category> findByOtherParentId(String parentId, ModelState state, CategoryType type, String index, Boolean active) {

		Query query = new Query(where("parentId").is(parentId));
		addStateQuery(query, state);
		addTypeQuery(query, type);
		if (index != null) {
			query.addCriteria(where("oldId").is(index));
		}
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, true, "id");

		return db.find(query, Category.class);
	}

	@Override
	public Category findRoot(String lang, ModelState state, CategoryType type) {

		Query query = new Query(
				where("parentId").is("").andOperator(where("lang").is(lang)));
		addStateQuery(query, state);
		addTypeQuery(query, type);

		return db.findOne(query, Category.class);

	}

	@Override
	public Optional<Category> findByName(String name, String lang, ModelState state, CategoryType type) {
		Query query = new Query(where("name").is(name));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		addTypeQuery(query, type);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<Category> list = db.find(query, Category.class);
		Category c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public List<Category> findBySlug(String slug, String lang, ModelState state, CategoryType type, @Nullable Boolean active) {
		Query query = new Query(where("slug").is(slug));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		if (type != null) {
			addTypeQuery(query, type);
		}
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<Category> ret = db.find(query, Category.class);

		return ret;
	};

	@Override
	public Optional<Category> findByStateRefId(String id, ModelState state, CategoryType type) {
		Query query = new Query(where("stateRefId").is(id));
		addStateQuery(query, state);
		addTypeQuery(query, type);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<Category> list = db.find(query, Category.class);
		Category c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public List<Category> findByLangRefId(String id) {
		Query query = new Query(where("langRefId").is(id));
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, Category.class);
	}



	// 再帰的に該当id配下のCategoryをすべて取得
	@Override
	public List<Category> findChild(String id, ModelState state, CategoryType type, @Nullable Boolean active){
		List<Category> list = new ArrayList<Category>();
		findChild(id, state, type, active, list);
		return list;
	}
	private List<Category> findChild(String id, ModelState state, CategoryType type, @Nullable Boolean active, List<Category> list) {

		List<Category> ret = findByParentId(id, state, type, active);
		if (ret.isEmpty() == false) {
			for(Category c : ret) {
				if (c != null) list.add(c);
				findChild(c.getId(), state, type, active, list);
			}
		}
		return list;
	}
	@Override
	public List<Category> findChildOther(String id, ModelState state, CategoryType type, String index, @Nullable Boolean active){
		List<Category> list = new ArrayList<Category>();
		findChildOther(id, state, type, index, active, list);
		return list;
	}

	private List<Category> findChildOther(String id, ModelState state, CategoryType type, String index, @Nullable Boolean active, List<Category> list) {

		List<Category> ret = findByOtherParentId(id, state, type, index, active);
		if (ret.isEmpty() == false) {
			for(Category c : ret) {
				if (c != null) list.add(c);
				findChildOther(c.getId(), state, type, index, active, list);
			}
		}
		return list;
	}


	@Override
	public List<Category> findByCategorySeries(List<CategorySeries> cs, @Nullable Boolean active) {
		List<Category> list = new ArrayList<Category>();
		List<String> ids = new ArrayList<String>();
		for(Category c : list) {
			ids.add(c.getId());
		}
		Query query = new Query(where("id").in(ids));
		if (active != null) {
			addActiveQuery(query, active);
		}

		return db.find(query, Category.class);
	}

	@Override
	public List<Category> search(String keyword, String lang, ModelState state, CategoryType type, Boolean active) {
		Query query = new Query(where("name").regex(keyword));
		addLangQuery(query, lang);
		addStateQuery(query, state);
		addTypeQuery(query, type);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, Category.class);
	}

	@Override
	public List<Category> searchWithSlug(String keyword, String lang, ModelState state, CategoryType type, Boolean active) {
		Query query = new Query(where("name").regex(keyword));
		addLangQuery(query, lang);
		addStateQuery(query, state);
		addTypeQuery(query, type);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, Category.class);
	}

	/**
	 * Rootカテゴリを作成
	 * @return testId StatusがTESTのIDを返す
	 */
	public String createRootCategory(String lang, ModelState state, String stateRefId, String langRefId) {

		String ret = null;
		// if not exists
		Query query = new Query(where("parentId").is(""));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		List<Category> list = db.find(query, Category.class);

		if (list == null || list.size() == 0) {
			Category c = new Category();
			c.setState(state);
			c.setName("root");
			c.setSlug("root");
			c.setParentId("");
			c.setLang(lang);
			c.setType(CategoryType.CATALOG);
			if (state.equals(ModelState.TEST) && StringUtils.isEmpty(stateRefId) == false) c.setStateRefId(stateRefId);
			if (langRefId != null && langRefId.equals("") == false) c.setLangRefId(langRefId);
			c = db.save(c);
			if (c != null) {
				ret = c.getId();
			}
		}

		return ret;
	}

	@Override
	public List<Category> findByIndex(String index, String lang, ModelState state, CategoryType type, Boolean active) {
		Query query = new Query(where("oldId").is(index));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		addTypeQuery(query, type);

		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, Category.class);
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
			q.addCriteria(where("state").is(state));
		}
	}
	private void addTypeQuery(Query q, CategoryType type) {
		if (type != null) {
			q.addCriteria(where("type").is(type));
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
		} else {
			q.with(Sort.by(Sort.Direction.DESC, param));
		}
	}


}
