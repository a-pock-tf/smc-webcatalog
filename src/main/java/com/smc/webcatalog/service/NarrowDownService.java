package com.smc.webcatalog.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.NarrowDownCompare;
import com.smc.webcatalog.model.NarrowDownValue;
import com.smc.webcatalog.model.Series;

@Service
@Scope("session")
public interface NarrowDownService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject saveColumn(NarrowDownColumn col);

	ErrorObject saveValue(NarrowDownValue val);

	ErrorObject deleteCategoryColumn(String categoryId);
	
	ErrorObject deleteColumn(String columnId);

	ErrorObject deleteProdColumn(String childColumnId);

	ErrorObject deleteSeriesValue(String seriesId);

	ErrorObject deleteColumnValue(String columnId);
	
	ErrorObject changeStateColumnValue(String testId, String prodId);

	/**
	 * カテゴリ配下の全Valueを削除
	 * @param categoryId
	 * @return
	 */
	ErrorObject deleteCategoryValue(String categoryId);

	List<NarrowDownColumn> getCategoryColumn(String categoryId, Boolean active, ErrorObject err);
	
	List<NarrowDownValue> getCategorySeriesValue(String categoryId, String seriesId, Boolean active, ErrorObject err);

	NarrowDownColumn getColumn(String id, ErrorObject err);

	NarrowDownValue getValue(String id, ErrorObject err);
	
	List<Series> getNarrowDown(String categoryId, HttpServletRequest request, ErrorObject err);
	
	// Compare
	List<NarrowDownCompare> getCategoryCompare(String categoryId, Boolean active, ErrorObject err);

	ErrorObject saveCompare(NarrowDownCompare comp);

	ErrorObject changeStateCompare(String testId, String prodId);

	ErrorObject deleteCategoryCompare(String categoryId);
	
	ErrorObject deleteCompare(String compareId);

	ErrorObject deleteProdCompare(String childCompareId);
}
