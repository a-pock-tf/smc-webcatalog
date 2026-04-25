package com.smc.discontinued.dao;

import java.util.List;
import java.util.Optional;

import com.mongodb.lang.Nullable;
import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface DiscontinuedCategoryTemplate {

	// 各言語のルートカテゴリの取得
	List<DiscontinuedCategory> listAll(String lang, DiscontinuedModelState state, @Nullable Boolean active);

	// Nameから検索
	Optional<DiscontinuedCategory> findByName(String name, String lang,DiscontinuedModelState state );

	// Slugから検索(1階層のためOptional）
	Optional<DiscontinuedCategory> findBySlug(String slug, String lang,DiscontinuedModelState state, @Nullable Boolean active);

	// oldId
	Optional<DiscontinuedCategory> findByOldId(String oldId, String lang, DiscontinuedModelState state, @Nullable Boolean active);

	// StateRefIdを検索
	Optional<DiscontinuedCategory> findByStateRefId(String id, DiscontinuedModelState state);

	// LangRefIdを検索
	List<DiscontinuedCategory> findByLangRefId(String id);

	// 検索
	List<DiscontinuedCategory> search(String keyword, String lang, DiscontinuedModelState state, @Nullable Boolean active);

}
