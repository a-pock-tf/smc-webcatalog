package com.smc.webcatalog.service;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;

@Service
@Scope("session")
public interface TemplateCategoryService {

	/**
	 *  save
	 * @param lang OUT 新規追加後、IDを戻す。
	 * @memo 保存前にnameの重複チェック
	 * @return ErrorObject
	 */
	ErrorObject save(TemplateCategory temp);

	ErrorObject changeStateToProd(String id, String categoryId, User u);
	/**
	 *  get
	 * @param lang
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	TemplateCategory get(String id, ErrorObject err);

	/**
	 *  get
	 * @param categoryId
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 */
	TemplateCategory getCategoryId(String categoryId, ErrorObject err);
	/**
	 *  get
	 * @param categoryId
	 * @param err OUT
	 * @return エラーの場合はnullが戻るので、ErrorObjectを確認
	 * @note 本番テンプレートを作成時、ModelState.TESTのCategoryIdを持つものが多数生成されていた。
	 *        そのチェック用。TemplateCategoryController.get()から呼ばれる時専用。2026/4/11
	 */
	TemplateCategory getCategory(Category c, ErrorObject err);

	/**
	 * HeartCoreのIDから取得。（インポート時のCategoryID更新用）
	 * @param id
	 * @param err
	 * @return
	 */
	TemplateCategory getHeartCoreID(String id, ErrorObject err);
	
	/**
	 * 言語Top
	 * @param temp
	 */
	TemplateCategory getLangAndStateFromBean(String lang, ModelState m);

	void setHeartCore(TemplateCategory temp);
	
	// 以下、templates系
	void refreshTemplateCategories();

	void addTemplateCategory(TemplateCategory temp);
	
	void removeTemplateCategory(TemplateCategory temp);
	
	TemplateCategory findByCategoryIdFromBean(String lang, ModelState s, String id);
	
	TemplateCategory findByLangFromBean(String lang, ModelState s);
}
