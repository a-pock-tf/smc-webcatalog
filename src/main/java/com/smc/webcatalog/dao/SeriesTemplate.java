package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import com.mongodb.lang.Nullable;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface SeriesTemplate {

	// Nameから検索
	Optional<Series> findByName(String name, String lang, ModelState state);

	// ModelNumberから検索
	Optional<Series> findByModelNumber(String mn, ModelState state, @Nullable Boolean active);

	/**
	 * StateRefIdはTEST固定。stateで指定された PROD or ARCHIVEのSeriesを検索
	 * @param id
	 * @param state
	 * @return
	 */
	List<Series> findByStateRefId(String testSeriesId, ModelState state);

	// LangRefIdを検索
	List<Series> findByLangRefId(String id);

	List<Series> findByLangRefId(String id, ModelState state);

	List<Series> listAll(String lang, ModelState state, Boolean active, Integer limit);

	List<Series> getPage(String[] arr, String lang, ModelState state, int page, int max);

	List<Series> search(String[] arr, String lang, ModelState state, Boolean active);

	List<Series> indexSearch(String h, String lang, ModelState state, Boolean active);

	List<Series> findByCategorySeriesEmptyList(String lang, ModelState state);

	/***
	 * Slugがbreadcrumbに入っているシリーズを取得。Slugが変更された場合に利用。
	 * @param slug
	 * @return
	 */
	List<Series> findBySlugFromBreadcrumb(String slug);

	/**
	 * SIDのリストにあるcad3dフラグをONに、無いものはOFF
	 * @param list
	 * @param lang
	 * @return
	 */
	boolean updateCad3D(List<String> list, String lang);

	boolean updateCustom(List<String> list, String lang);

	boolean updateOrderMade(List<String> list, String lang);

	// ↓CategorySereisTemplateを使って取得
	// シリーズのIDを含むリストを検索(シリーズから見た場合、シリーズ削除の際のDBRef削除用)
	// List<Category> listBySeriesId(String seriesId);

	// 検索一覧用
	// リスト毎に類義語が入る。類義語はor検索。
	List<Series> searchAndOr(String[] kwArr,  String lang, int max, Boolean isProd, @Nullable Boolean active);
	long searchAndOrCount(String[] kwArr,  String lang, Boolean isProd, @Nullable Boolean active);

}
