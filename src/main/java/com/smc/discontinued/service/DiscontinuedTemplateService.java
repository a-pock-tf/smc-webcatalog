package com.smc.discontinued.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.discontinued.model.DiscontinuedTemplate;
import com.smc.webcatalog.model.ErrorObject;

@Service
@Scope("session")
public interface DiscontinuedTemplateService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject save(DiscontinuedTemplate temp);

	/**
	 *  get
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedTemplate get(String id, ErrorObject err);

	/**
	 *  get
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	DiscontinuedTemplate getLang(String lang, ErrorObject err);

	/**
	 * HeartCoreのIDから取得。（インポート時のCategoryID更新用）
	 * @param id
	 * @param err
	 * @return
	 */
	DiscontinuedTemplate getHeartCoreID(String id, ErrorObject err);


}
