package com.smc.webcatalog.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface TemplateService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject save(Template temp);
	
	/**
	 *  該当idのstateをTESTからPRODへ変更
	 * @param id (ModelState.TEST)
	 * @memo active = true のみ変更。前後のIDが同じでなければエラー。
	 * @return ErrorObject
	 */
	ErrorObject changeStateToProd(String id, User u);

	/**
	 *  get
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Template get(String id, ErrorObject err);

	/**
	 *  getLang
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 * @note 2026/3/30 lang だけではTEST PRODの２つ取れてしまう。下のgetLangAndModelState()へ変更
	 */
/*	Template getLang(String lang, ErrorObject err);*/
	
	/**
	 *  getLangAndModelState
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	Template getLangAndModelState(String lang, ModelState state, Boolean active, ErrorObject err);

	void setHeartCore(Template temp);
	
	// 以下、templates系
	void refreshTemplates();

	void addTemplates(Template temp);
	
	void removeTemplates(Template temp);
	
	Template getTemplateByTemplates(String lang, ModelState s);
}
