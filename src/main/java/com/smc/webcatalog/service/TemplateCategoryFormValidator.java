package com.smc.webcatalog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.TemplateCategoryRepository;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.TemplateCategoryForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class TemplateCategoryFormValidator {

	@Autowired
	TemplateCategoryRepository repo;

	@Autowired
	TemplateCategoryServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, TemplateCategoryForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		if (form.getHeartCoreId() == null)
		{
			result.rejectValue("heartCoreId", "my.validation.empty", new String[] { "" }, "");
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, TemplateCategoryForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		if (form.getHeartCoreId() == null)
		{
			result.rejectValue("heartCoreId", "my.validation.empty", new String[] { "" }, "");
		}
		ret = true;


		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
