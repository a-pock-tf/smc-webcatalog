package com.smc.psitem.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.mongodb.lang.Nullable;
import com.smc.psitem.model.PsItem;
import com.smc.webcatalog.model.MyErrors;

@Service
@Scope("session")
public interface PsItemService {

	/**
	 * 
	 * @param kw
	 * @param condition
	 * @param c1c2
	 * @param series
	 * @param lang
	 * @return active=trueのみ
	 */
	List<PsItem> searchKeyword(List<String> kw, String condition, String c1c2, String series, String lang);
	List<PsItem> searchKeyword(List<String> kw, String condition, String c1c2, String series, String lang, int start, int limit);

	/**
	 * 上記searchKeyword()のhitCount取得
	 * @param kw
	 * @param condition
	 * @param c1c2
	 * @param series
	 * @param lang
	 * @return
	 */
	long searchKeywordCount(List<String> kw, String condition, String c1c2, String series, String lang);

	List<PsItem> searchIndex(String idx, String c1c2, String series, String lang);

	/**
	 * CSVのフォーマットチェック
	 */
	MyErrors checkFormat(String fullpath, String enc, int colSize);

	/**
	 * 全体検索のAPI
	 * @param kwArr
	 * @param lang
	 * @param cd 1:前方一致 2:部分一致
	 * @param max
	 * @return
	 */
	List<PsItem> searchKeywordAndOr(String[] kwArr, String lang, String cd, int max, @Nullable Boolean active);
	long searchKeywordAndOrCount(String[] kwArr, String lang, String cd, @Nullable Boolean active);
	/**
	 *
	 * @param path
	 * @param lang
	 * @param enc
	 * @return
	 */
	int importItem(String path,String lang,String enc);
}
