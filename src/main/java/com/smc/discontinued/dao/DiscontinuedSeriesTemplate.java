package com.smc.discontinued.dao;

import java.util.List;
import java.util.Optional;

import com.mongodb.lang.Nullable;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface DiscontinuedSeriesTemplate {

	// 各言語のルートカテゴリの取得
	List<DiscontinuedSeries> listAll(String lang, DiscontinuedModelState state);

	// 終了日でソート
	List<DiscontinuedSeries> listAllSortByEndDate(String lang, DiscontinuedModelState state, boolean asc);

	List<DiscontinuedSeries> listCategory(String categoryId, DiscontinuedModelState state, @Nullable Boolean active);

	List<DiscontinuedSeries> listLang(String lang, @Nullable Boolean active);

	// Nameから検索
	Optional<DiscontinuedSeries> findByName(String name, String lang,DiscontinuedModelState state );

	Optional<DiscontinuedSeries> findBySeriesId(String seriesid, DiscontinuedModelState state);

	// StateRefIdを検索
	List<DiscontinuedSeries> findByStateRefId(String id, DiscontinuedModelState state);

	// LangRefIdを検索
	List<DiscontinuedSeries> findByLangRefId(String id);

	List<DiscontinuedSeries> findByLangRefId(String id, DiscontinuedModelState state );

	/**
	 * Discontinued image
  　　 例） VHK-old.jpg
Replacement image
  　　 例） VHK-A.jpg
Discontinued catalogLink
  　　 例） VHK-old.pdf
Product comparison details PDF
  　　 例） VHK-comp.pdf
	 */
	List<DiscontinuedSeries> findByImage(String file);

	List<DiscontinuedSeries> findByReplacementImage(String file);

	List<DiscontinuedSeries> findByCatalogLink(String file);

	List<DiscontinuedSeries> findByComparisonDetailsPDF(String file);

	// 検索
	List<DiscontinuedSeries> search(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active);

	List<DiscontinuedSeries> indexSearch(String h, String lang, DiscontinuedModelState state, Boolean active);

	/**
	 * WEBカタログからのヒット確認。確認だけなので、limit1
	 * @param h
	 * @param lang
	 * @param state
	 * @param active
	 * @return
	 */
	boolean hitSearch(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active);
	/**
	 * WEBカタログからのヒット確認。確認だけなので、limit1
	 * @param h
	 * @param lang
	 * @param state
	 * @param active
	 * @return
	 */
	boolean hitIndexSearch(String h, String lang, DiscontinuedModelState state, Boolean active);
}
