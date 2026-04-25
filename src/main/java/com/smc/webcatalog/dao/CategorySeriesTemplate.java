package com.smc.webcatalog.dao;

import java.util.List;

import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.Series;

public interface CategorySeriesTemplate {

		List<CategorySeries> findBySeriesId(String id);

		List<CategorySeries> findAllByCategoryId(String id);

		List<CategorySeries> findAllByCategoryAndSeriesId(String categoryId, String seriesId);

		/**
		 * category-seriesList と1対Nのため、repo.deleteは使えない。
		 * Listから該当IDのみを削除
		 * @return 削除数
		 */
		int deleteSeriesFromSeriesList(String seriesId);

		/**
		 * categoryのCategorySeriesがあればseriesListに追加。CategorySeriesが無ければ新規作成
		 * @param categoryId
		 * @param series
		 * @return
		 */
		CategorySeries upsert(String categoryId, Series series);

		void updateProdOrder(String prodCategoryId, List<Series> testList);
}
