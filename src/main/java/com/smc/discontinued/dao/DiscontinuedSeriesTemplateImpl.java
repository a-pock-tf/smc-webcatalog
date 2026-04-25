package com.smc.discontinued.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;

public class DiscontinuedSeriesTemplateImpl implements DiscontinuedSeriesTemplate {


	@Autowired
	private MongoTemplate db;

	@Override
	public List<DiscontinuedSeries> listAll(String lang, DiscontinuedModelState state) {

		Query query = new Query(
				where("lang").is(lang));
		addStateQuery(query, state);

		return db.find(query, DiscontinuedSeries.class);

	}

	@Override
	public List<DiscontinuedSeries> listAllSortByEndDate(String lang, DiscontinuedModelState state, boolean asc) {
		Query query = new Query(
				where("lang").is(lang));
		addStateQuery(query, state);
		addSortOrder(query, false, "date");

		return db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> listCategory(String categoryId, DiscontinuedModelState state, Boolean active) {
		Query query = new Query(
				where("categoryId").is(categoryId));
		addStateQuery(query, state);
		addSortOrder(query, false, "date");
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");
		if (active != null) {
			addActiveQuery(query, active);
		}
		return db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> listLang(String lang, Boolean active) {
		Query query = new Query(
				where("lang").is(lang));
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");
		if (active != null) {
			addActiveQuery(query, active);
		}
		return db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public Optional<DiscontinuedSeries> findByName(String name, String lang, DiscontinuedModelState state) {
		Query query = new Query(where("name").is(name));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedSeries> list = db.find(query, DiscontinuedSeries.class);
		DiscontinuedSeries c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public Optional<DiscontinuedSeries> findBySeriesId(String seriesid, DiscontinuedModelState state) {
		Query query = new Query(where("seriesId").is(seriesid));
		addStateQuery(query, state);

		List<DiscontinuedSeries> list = db.find(query, DiscontinuedSeries.class);
		DiscontinuedSeries c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public List<DiscontinuedSeries> findByStateRefId(String id, DiscontinuedModelState state) {
		Query query = new Query(where("stateRefId").is(id));
		addStateQuery(query, state);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedSeries> list = db.find(query, DiscontinuedSeries.class);
		DiscontinuedSeries c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> findByLangRefId(String id) {
		Query query = new Query(where("langRefId").is(id));
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> findByLangRefId(String id, DiscontinuedModelState state) {
		Query query = new Query(where("langRefId").is(id));
		addStateQuery(query, state);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> findByImage(String file) {
		Query query = new Query(where("image").in(file));
		return  db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> findByReplacementImage(String file) {
		Query query = new Query(where("newImage").in(file));
		return  db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> findByCatalogLink(String file) {
		Query query = new Query(where("catalogLink").in(file));
		return  db.find(query, DiscontinuedSeries.class);
	}

	@Override
	public List<DiscontinuedSeries> findByComparisonDetailsPDF(String file) {
		Query query = new Query(where("comparison").in(file));
		return  db.find(query, DiscontinuedSeries.class);
	}



	@Override
	public List<DiscontinuedSeries> search(String keyword, String lang, DiscontinuedModelState state, Boolean active) {
		Query query = new Query(where("lang").is(lang)  // i は大文字、小文字区別無し。
		.orOperator(
				where("name").regex(keyword, "i"),
				where("seriesName").regex(keyword, "i"),
				where("seriesId").regex(keyword, "i"),
				where("series").regex(keyword, "i"),
				where("newSeries").regex(keyword, "i")));
		addStateQuery(query, state);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "categoryId");
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, DiscontinuedSeries.class);
	}
	
	@Override
	public boolean hitSearch(String keyword, String lang, DiscontinuedModelState state, Boolean active) {
		boolean ret = false;
		Query query = new Query(where("lang").is(lang)  // i は大文字、小文字区別無し。
		.orOperator(
				where("name").regex(keyword, "i"),
				where("seriesName").regex(keyword, "i"),
				where("seriesId").regex(keyword, "i"),
				where("series").regex(keyword, "i"),
				where("newSeries").regex(keyword, "i")));
		addStateQuery(query, state);
		if (active != null) {
			addActiveQuery(query, active);
		}
		query.limit(1); // hitなのでlimit 1
		addSortOrder(query, true, "categoryId");
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return ret;
	}


	@Override
	public List<DiscontinuedSeries> indexSearch(String h, String lang, DiscontinuedModelState state, Boolean active) {
		Query query = new Query(where("lang").is(lang)
				.orOperator(
						where("name").regex('^' + h +".*", "i"),
						where("seriesName").regex('^' + h +".*", "i"),
						where("seriesId").regex('^' + h +".*", "i"),
						where("series").regex('^' + h +".*", "i"),
						where("newSeries").regex('^' + h +".*", "i")));
		if (active != null) addActiveQuery(query, active);
		addStateQuery(query, state);
		addSortOrder(query, true, "categoryId");
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedSeries> list = db.find(query, DiscontinuedSeries.class);
		return list;
	}

	@Override
	public boolean hitIndexSearch(String h, String lang, DiscontinuedModelState state, Boolean active) {
		boolean ret = false;
		Query query = new Query(where("lang").is(lang)
				.orOperator(
						where("name").regex('^' + h +".*", "i"),
						where("seriesName").regex('^' + h +".*", "i"),
						where("seriesId").regex('^' + h +".*", "i"),
						where("series").regex('^' + h +".*", "i"),
						where("newSeries").regex('^' + h +".*", "i")));
		if (active != null) addActiveQuery(query, active);
		query.limit(1); // hitなのでlimit 1
		addStateQuery(query, state);
		addSortOrder(query, true, "categoryId");
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<DiscontinuedSeries> list = db.find(query, DiscontinuedSeries.class);
		if (list != null && list.size() > 0) ret = true;
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
