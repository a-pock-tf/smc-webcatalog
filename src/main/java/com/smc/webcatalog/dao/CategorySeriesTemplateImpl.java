package com.smc.webcatalog.dao;

import static org.springframework.data.mongodb.core.query.Criteria.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.Series;

public class CategorySeriesTemplateImpl implements CategorySeriesTemplate {

	@Autowired
	private MongoTemplate db;

	@Override
	public List<CategorySeries> findBySeriesId(String id) {
		Query query = new Query(where("seriesList.id").is(id));

		return db.find(query, CategorySeries.class);
	}
	
	@Override
	public CategorySeries findOneBySeriesId(String id) {
		Query query = new Query(where("seriesList.id").is(id));
		query.limit(1);

		return db.findOne(query, CategorySeries.class);
	}

	@Override
	public List<CategorySeries> findAllByCategoryId(String id) {
		Query query = new Query(where("categoryId").is(id));

		return db.find(query, CategorySeries.class);
	}


	@Override
	public List<CategorySeries> findAllByCategoryAndSeriesId(String categoryId, String seriesId) {
		Query query = new Query(where("seriesList.id").is(seriesId));
		query.addCriteria(where("categoryId").is(categoryId));

		return db.find(query, CategorySeries.class);
	}


	@Override
	public CategorySeries upsert(String categoryId, Series series) {
		CategorySeries ret = null;
		Query query = new Query(where("categoryId").is(categoryId));
		List<CategorySeries> list = db.find(query, CategorySeries.class);
		if (list == null || list.isEmpty()) {
			CategorySeries cs = new CategorySeries();
			cs.setCategoryId(categoryId);
			List<Series> sList = new ArrayList<Series>();
			sList.add(series);
			cs.setSeriesList(sList);
			ret = db.save(cs);
		} else {
			for(CategorySeries cs : list) {
				List<Series> sList = cs.getSeriesList();
				boolean isFind = false;
				for(Series s : sList) {
					if (s.getId().equals(series.getId())) {
						isFind = true;
					}
				}
				if (isFind == false) {
					sList.add(series);
					ret = db.save(cs);
				}
			}
		}
		return ret;
	}

	@Override
	public int deleteSeriesFromSeriesList(String seriesId) {
		int ret = 0;

		List<CategorySeries> list = findBySeriesId(seriesId);
		List<CategorySeries> delList = new ArrayList<CategorySeries>();// CategorySeriesの削除用

		for(CategorySeries cs : list) {
			List<Series> sList = cs.getSeriesList();
			List<Series> removeList = new ArrayList<Series>();
			for(Series s : sList) {
				if (s.getId().equals(seriesId)) {
					removeList.add(s);
				}
			}
			if (removeList.size() > 0) {
				if (sList.size() == 1) {
					// CategorySeriesを削除
					delList.add(cs);
				} else {
					sList.removeAll(removeList);
					cs.setSeriesList(sList);
					db.save(cs);
					ret++;
				}

			}
		}
		if (delList.size() > 0) {
			for(CategorySeries cs : delList) {
				db.remove(cs);
				ret++;
			}
		}
		return ret;
	}
	// TESTと同じPRODの順番にする。
	@Override
	public void updateProdOrder(String prodCategoryId, List<Series> testList) {
		Iterable<CategorySeries> list = findAllByCategoryId(prodCategoryId);
		List<Series> saveList = new LinkedList<Series>();
		for(Series s : testList) {
			for(CategorySeries cs : list) {
				boolean isFind = false;
				List<Series> sList = cs.getSeriesList();
				for(Series ps: sList) {
					if (ps.getModelNumber().equals(s.getModelNumber())) {
						saveList.add(ps);
						isFind=true;
						break;
					}
				}
				if (isFind) break;
			}
		}
		if (saveList.size() > 0) {
			for(CategorySeries cs : list) {
				cs.setCategoryId(prodCategoryId);
				cs.setSeriesList(saveList);
				db.save(cs);
			}
		}
	}

}
