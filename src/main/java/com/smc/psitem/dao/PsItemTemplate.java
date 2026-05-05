package com.smc.psitem.dao;

import java.util.List;

import com.mongodb.lang.Nullable;
import com.smc.psitem.model.PsItem;

/***
 * MongoRepositoryで足りないものはこちらで
 * @author miyasit
 *
 */
public interface PsItemTemplate {

	// 各言語のルートカテゴリの取得
	List<PsItem> listAll(String lang, @Nullable Boolean active);

	// 検索
	// +あいまい検索 DBに登録されている□をワイルドカードとする
	// keyListはlist毎はAnd、list内はスペースでOR
	List<PsItem> search(List<String> keyList, String condition, String c1c2, String series, String lang, @Nullable Boolean active, int start, int limit);

	// 上記search()のcount
	long searchCount(List<String> keyList, String condition, String c1c2, String series, String lang, @Nullable Boolean active);

	// 検索
	List<PsItem> searchIndex(String index, String c1c2, String series, String lang, @Nullable Boolean active);

	// 検索一覧用
	// 検索DBはstate:PROD TESTは無い！
	// リスト毎に類義語が入る。類義語はor検索。
	List<PsItem> searchAndOr(String[] kwArr,  String lang, String cd, int max, @Nullable Boolean active);
	long searchAndOrCount(String[] kwArr,  String lang, String cd, @Nullable Boolean active);

}
