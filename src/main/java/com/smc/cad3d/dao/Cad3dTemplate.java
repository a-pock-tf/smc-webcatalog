package com.smc.cad3d.dao;

import java.util.List;

import com.mongodb.lang.Nullable;
import com.smc.cad3d.model.Cad3d;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface Cad3dTemplate {

	// 各言語のルートカテゴリの取得
	List<Cad3d> listAll(String lang, @Nullable Boolean active);

	// 検索
	// +あいまい検索 DBに登録されている□をワイルドカードとする
	List<Cad3d> search(List<String> keyList, String c1c2, String series, String lang, @Nullable Boolean active);

	// 検索
	List<Cad3d> searchIndex(String index, String c1c2, String series, String lang, @Nullable Boolean active);


	// URL検索
	List<Cad3d> searchUrl(String url, String lang, @Nullable Boolean active);

	// ユーザー側検索
	List<Cad3d> search(String sid, String series, String lang, String cat);

}
