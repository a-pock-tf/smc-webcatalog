package com.smc.webcatalog.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.SeriesFaq;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface FaqCategoryService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject saveAll(List<SeriesFaq> list, User u);
	
	ErrorObject delete(String seriesId);

	/**
	 *  get
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	SeriesFaq get(String id, ErrorObject err);

	/**
	 *  getSeriesId
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	SeriesFaq getSeriesId(String seriesId, ErrorObject err);
	
	/**
	 *  get
	 * @param categoryId
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	SeriesFaq getModelNumber(String modelNumber, ErrorObject err);

}
