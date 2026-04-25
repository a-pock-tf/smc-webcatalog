package com.smc.omlist.dao;

import java.util.List;

import com.mongodb.lang.Nullable;
import com.smc.omlist.model.Omlist;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface OmlistTemplate {

	// 各言語のルートカテゴリの取得
	List<Omlist> listAll(String lang, @Nullable Boolean active);

	// 検索
	List<Omlist> search(List<String> keyList, String category, String div, String series, String lang, @Nullable Boolean active);


}
