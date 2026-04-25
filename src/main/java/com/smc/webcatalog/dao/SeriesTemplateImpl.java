package com.smc.webcatalog.dao;

// XXX whre句はstatic import
import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.lang.Nullable;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;

public class SeriesTemplateImpl implements SeriesTemplate {


	@Autowired
	private MongoTemplate db;

	@Override
	public Optional<Series> findByName(String name, String lang, ModelState state) {
		Query query = new Query(where("name").is(name));
		addStateQuery(query, state);
		addLangQuery(query, lang);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<Series> list = db.find(query, Series.class);
		Series c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public Optional<Series> findByModelNumber(String mn, ModelState state, @Nullable Boolean active) {
		Query query = new Query(where("modelNumber").is(mn));
		addStateQuery(query, state);
		if (active != null) {
			addActiveQuery(query, active);
		}
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<Series> list = db.find(query, Series.class);
		Series c = null;
		if (list != null && list.isEmpty() == false)
		{
			c = list.get(0);
		}
		return Optional.ofNullable(c);
	}

	@Override
	public List<Series> findByStateRefId(String testCategoryId, ModelState state) {

		Query query = new Query(where("stateRefId").is(testCategoryId));
		addStateQuery(query, state);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");


		return db.find(query, Series.class);
	}

	@Override
	public List<Series> findByLangRefId(String id) {
		Query query = new Query(where("langRefId").is(id));
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, Series.class);
	}

	@Override
	public List<Series> findByLangRefId(String id, ModelState state) {
		Query query = new Query(where("langRefId").is(id));
		addStateQuery(query, state);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		return db.find(query, Series.class);
	}

	@Override
	public List<Series> findByCategorySeriesEmptyList(String lang, ModelState state) {
		List<Series> list = listAll(lang, state, null, null);

		Query query = new Query(where("seriesList").nin(list));
		addSortOrder(query, false, "id");

		List<CategorySeries> cList = db.find(query, CategorySeries.class);

		query = new Query(where("seriesList").in(cList));
		addSortOrder(query, false, "id");

		return db.find(query, Series.class);
	}


	@Override
	public List<Series> listAll(String lang, ModelState state, Boolean active, Integer limit) {
		Query query = new Query(where("lang").is(lang));
		addStateQuery(query, state);
		if (active != null) addActiveQuery(query, active);
		addSortOrder(query, false, "id");
		if (limit != null) query.limit(limit);

		List<Series> list = db.find(query, Series.class);
		return list;
	}

	@Override
	public List<Series> getPage(String[] keys, String lang, ModelState state, int page, int max) {
		List<Series> ret = null;
		List<Criteria> cliList = new ArrayList<>(); // queryに入れる時は１回で入れないとエラーになる
		for(String key : keys) {
			String[] arr = key.split(" ");
			List<Criteria> orList = new ArrayList<>();
			for(String k : arr) {
				if (k != null && k.trim().isEmpty() == false) {
					String kwQuery = ".*"+k+".*";
					orList.add(where("keyword").regex(kwQuery, "i"));
					orList.add(where("detail").regex(kwQuery, "i"));
					orList.add(where("other").regex(kwQuery, "i"));
					orList.add(where("spec").regex(kwQuery, "i"));
				}
			}
			if (orList.size() > 0) {
				Criteria cri = new Criteria();
				cliList.add(cri.orOperator(orList));
			}
		}
		if (cliList.size() > 0) {
			if (lang != null && lang.isEmpty() == false) {
				cliList.add(where("lang").is(lang));
			}
			cliList.add(where("state").is(state));
			cliList.add(where("active").is(true));

			cliList.add(where("query").ne("")); // ID無し（query="")は除外

			Criteria cri = new Criteria(); // criは毎回newしないとaddされる。
			Query query = new Query(cri.andOperator(cliList));
			if (max > 0) {
				if ( page > 1) {
					query.skip((page-1) * max);
				}
				query.limit(max);
			}
			addSortOrder(query, true, "order");
			addSortOrder(query, false, "id");
			
			ret = db.find(query, Series.class);

		}

		return ret;
	}


	@Override
	public List<Series> search(String[] keys,String lang, ModelState state, Boolean active) {
		String keyword = "";
		for(String k: keys) {
			keyword += k.trim() + "|";
		}
		keyword = keyword.substring(0, keyword.length()-1);
		Query query = new Query(where("lang").is(lang)
				.orOperator(where("keyword").regex(keyword, "i"), // i は大文字、小文字区別無し。
						where("detail").regex(keyword, "i"),
						where("other").regex(keyword, "i"),
						where("spec").regex(keyword, "i")));
		addStateQuery(query, state);
		if (active != null) addActiveQuery(query, active);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		query.limit(10000);

		List<Series> list = db.find(query, Series.class);
		return list;
	}

	@Override
	public List<Series> indexSearch(String h, String lang, ModelState state, Boolean active) {
		Query query = new Query(where("lang").is(lang));
		query.addCriteria(where("modelNumber").regex('^' + h +".*"));
		addStateQuery(query, state);
		if (active != null) addActiveQuery(query, active);
		addSortOrder(query, true, "order");
		addSortOrder(query, false, "id");

		List<Series> list = db.find(query, Series.class);
		return list;
	}

	@Override
	public List<Series> findBySlugFromBreadcrumb(String slug) {
		Query query = new Query(where("breadcrumb").regex("%"+slug+"%"));
		List<Series> list = db.find(query, Series.class);
		return list;
	}

	@Override
	public boolean updateCad3D(List<String> list, String lang) {
		boolean ret = false;
		if (list != null) {
			Query query = new Query(where("cad3d").is(true));
			if (lang != null) query.addCriteria(where("lang").is(lang));
			Update up = new Update();
			up.set("cad3d", false);
			db.updateMulti(query, up, Series.class);

			query = new Query(where("modelNumber").in(list));
			if (lang != null) query.addCriteria(where("lang").is(lang));
			up = new Update();
			up.set("cad3d", true);
			db.updateMulti(query, up, Series.class);
		}
		return ret;
	}
	@Override
	public boolean updateCustom(List<String> list, String lang) {
		boolean ret = false;
		if (list != null) {
			Query query = new Query(where("custom").is(true));
			if (lang != null) query.addCriteria(where("lang").is(lang));
			Update up = new Update();
			up.set("custom", false);
			db.updateMulti(query, up, Series.class);

			query = new Query(where("modelNumber").in(list));
			if (lang != null) query.addCriteria(where("lang").is(lang));
			up = new Update();
			up.set("custom", true);
			db.updateMulti(query, up, Series.class);
		}
		return ret;
	}

	@Override
	public boolean updateOrderMade(List<String> list, String lang) {
		boolean ret = false;
		if (list != null) {
			Query query = new Query(where("orderMade").is(true));
			if (lang != null) query.addCriteria(where("lang").is(lang));
			Update up = new Update();
			up.set("orderMade", false);
			db.updateMulti(query, up, Series.class);

			query = new Query(where("modelNumber").in(list));
			if (lang != null) query.addCriteria(where("lang").is(lang));
			up = new Update();
			up.set("orderMade", true);
			db.updateMulti(query, up, Series.class);
		}
		return ret;
	}
	
	@Override
	public List<Series> searchAndOr(String[] kwArr, String lang, int max, Boolean isProd, Boolean active) {
		List<Series> ret = null;
		List<Criteria> cliList = getKwArrCriteriaList(kwArr);
		
		if (cliList.size() > 0) {
			if (lang != null && lang.isEmpty() == false) {
				cliList.add(where("lang").is(lang));
			}
			if (isProd) {
				cliList.add(where("state").is("PROD"));
			} else {
				cliList.add(where("state").is("TEST"));
			}
			if (active != null) {
				cliList.add(where("active").is(active));
			}
			cliList.add(where("query").ne("")); // ID無し（query="")は除外

			Criteria cri = new Criteria(); // criは毎回newしないとaddされる。
			Query query = new Query(cri.andOperator(cliList));
			if (max > 0) {
				query.limit(max);
			}
			addSortOrder(query, true, "order");
			addSortOrder(query, false, "id");
			
			ret = db.find(query, Series.class);

		}
		return ret;
	}
	private List<Criteria> getKwArrCriteriaList(String[] kwArr) {
		List<Criteria> cliList = new ArrayList<>();
		
		for(String key : kwArr) {
			String[] arr = key.split(" ");
			List<Criteria> orList = new ArrayList<>();
			for(String k : arr) {
				if (k != null && k.trim().isEmpty() == false) {
					if (k.indexOf("□") > -1) {
						k = k.replaceAll("□", ".*");
					}
					String kwQuery = ".*"+k+".*";
					orList.add(where("keyword").regex(kwQuery, "i"));
					orList.add(where("detail").regex(kwQuery, "i"));
					orList.add(where("other").regex(kwQuery, "i"));
					orList.add(where("spec").regex(kwQuery, "i"));
				}
			}
			if (orList.size() > 0) {
				Criteria cri = new Criteria();
				cliList.add(cri.orOperator(orList));
			}
		}
		
		return cliList;
	}
	@Override
	public long searchAndOrCount(String[] kwArr, String lang, Boolean isProd, Boolean active) {
		long ret = -1;
		List<Criteria> cliList = getKwArrCriteriaList(kwArr);
		
		if (cliList.size() > 0) {
			if (lang != null && lang.isEmpty() == false) {
				cliList.add(where("lang").is(lang));
			}
			if (isProd) {
				cliList.add(where("state").is("PROD"));
			} else {
				cliList.add(where("state").is("TEST"));
			}
			if (active != null) {
				cliList.add(where("active").is(active));
			}
			cliList.add(where("query").ne("")); // ID無し（query="")は除外

			Criteria cri = new Criteria(); // criは毎回newしないとaddされる。
			Query query = new Query(cri.andOperator(cliList));
			ret = db.count(query, Series.class);

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
