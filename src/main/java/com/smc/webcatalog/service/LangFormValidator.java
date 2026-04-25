package com.smc.webcatalog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.LangRepository;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.LangForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class LangFormValidator {

	@Autowired
	LangRepository repo;

	@Autowired
	LangServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, LangForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		// 同一nameが存在するか
		if (service.isNameExists(form.getName(), obj)) {
			result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
		}
		// versionがtrueならbaseLangは必須
		if (form.isVersion()) {
			if ((form.getBaseLang() == null || form.getBaseLang().isEmpty())) {
				result.rejectValue("baseLang", "my.validation.empty", null, "");
			} else if (service.isNameExists(form.getBaseLang(), obj) == false) {
				result.rejectValue("baseLang", "my.validation.empty", null, "");
			}
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, LangForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		Optional<Lang> u = repo.findById(form.getId());
		if (u.isPresent()) {
			Lang lang = u.get();
			// nameが変更された場合、同一nameが存在するか
			if (lang.getName().equals(form.getName()) == false) {
				lang.setName(form.getName());
				if (service.isNameExists(lang.getName(), obj)){
					result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
				}
			}

			ret = true;
		}
		// versionがtrueならbaseLangは必須
		if (form.isVersion()) {
			if ((form.getBaseLang() == null || form.getBaseLang().isEmpty())) {
				result.rejectValue("baseLang", "my.validation.empty", null, "");
			} else if (service.isNameExists(form.getBaseLang(), obj) == false) {
				result.rejectValue("baseLang", "my.validation.empty", null, "");
			}
		}

		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
