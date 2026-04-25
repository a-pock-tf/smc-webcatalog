package com.smc.omlist.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.omlist.model.Omlist;
import com.smc.webcatalog.model.MyErrors;

@Service
@Scope("session")
public interface OmlistService {

	/**
	 * lang以外はnull可
	 * @param kw
	 * @param category
	 * @param div
	 * @param series
	 * @param lang
	 * @return
	 */
	List<Omlist> searchKeyword(List<String> kw, String category, String div, String series, String lang);

	/**
	 * 本来はControllerだがAPIもあるので。
	 */
	String getTableHtml(List<Omlist> list, String lang);

	String getTableHtml2026(List<Omlist> list, String lang);
	/**
	 * CSVのフォーマットチェック
	 */
	MyErrors checkFormat(String fullpath, String enc, int colSize);

	/**
	 *
	 * @param path
	 * @param lang
	 * @param enc
	 * @return
	 */
	int importItem(String path,String lang,String enc);
}
