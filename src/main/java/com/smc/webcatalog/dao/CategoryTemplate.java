package com.smc.webcatalog.dao;

import java.util.List;
import java.util.Optional;

import com.mongodb.lang.Nullable;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ModelState;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface CategoryTemplate {

	// 各言語のルートカテゴリの取得
	@Deprecated
	Category findRoot(String lang, ModelState state, CategoryType type);

	/**
	 * 下位カテゴリのリスト
	 * @param parentId
	 * @param state
	 * @param type
	 * @param active
	 * @return
	 */
	List<Category> findByParentId(String parentId, ModelState state, CategoryType type, @Nullable Boolean active);

	List<Category> findByOtherParentId(String parentId, ModelState state, CategoryType type, String index, @Nullable Boolean active);

	// Nameから検索
	Optional<Category> findByName(String name, String lang,ModelState state, CategoryType type);
	// StateRefIdを検索
	Optional<Category> findByStateRefId(String id, ModelState state, CategoryType type);
	// LangRefIdを検索
	List<Category> findByLangRefId(String id);

	// Slugから検索(階層が違えば同じSlugはOKのためList）
	List<Category> findBySlug(String slug, String lang,ModelState state, CategoryType type, @Nullable Boolean active);

	// 再帰的に該当id配下のCategoryをすべて取得
	List<Category> findChild(String id, ModelState state, CategoryType type, @Nullable Boolean active);

	// 再帰的に該当id配下のCategoryをすべて取得。Other用。index(oldId)を指定
	List<Category> findChildOther(String id, ModelState state, CategoryType type, String index, @Nullable Boolean active);

	// CategoryServiceからCategoryの一覧を取得
	List<Category> findByCategorySeries(List<CategorySeries> cs,  @Nullable Boolean active);

	// 再帰的に該当index配下のCategoryをすべて取得
	List<Category> findByIndex(String index, String lang, ModelState state, CategoryType type, @Nullable Boolean active);

	// 検索
	List<Category> search(String keyword, String lang, ModelState state, CategoryType type, @Nullable Boolean active);
	List<Category> searchWithSlug(String keyword, String lang, ModelState state, CategoryType type, @Nullable Boolean active);

	/**
	 * Rootカテゴリを作成
	 * @return id
	 */
	String createRootCategory(String lang, ModelState state, String stateRefId, String langRefId);

}
