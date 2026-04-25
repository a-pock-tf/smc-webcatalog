package com.smc.webcatalog.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Series;

@Service
@Scope("session")
public interface CategorySeriesService {

	/**
	 *  save
	 * @param
	 * @memo 重複でもエラーとせずCategorySeriesを戻す。
	 * @return ErrorObject
	 */
	CategorySeries save(Category category, Series series, ErrorObject err);

	/**
	 * delete
	 */
	ErrorObject delete(String categoryId, String seriesId);
}
