package com.smc.discontinued.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.discontinued.dao.DiscontinuedTemplateRepository;
import com.smc.discontinued.model.DiscontinuedTemplateForm;
import com.smc.webcatalog.model.ErrorObject;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class DiscontinuedTemplateFormValidator {

	@Autowired
	DiscontinuedTemplateRepository repo;

	@Autowired
	DiscontinuedTemplateServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, DiscontinuedTemplateForm form) {
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
	public boolean validateUpate(BindingResult result, DiscontinuedTemplateForm form) {
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
