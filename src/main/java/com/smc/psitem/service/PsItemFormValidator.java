package com.smc.psitem.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.psitem.model.PsItemForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class PsItemFormValidator {


	// 新規時のチェック
	public boolean validateNew(BindingResult result, PsItemForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}
		if (form.getLang() ==  null || form.getLang().isEmpty()) {
			result.rejectValue("lang", "my.validation.test", new String[] { form.getLang() }, "");
		}
		if (form.getFile() == null || form.getFile().isEmpty()) {
			result.rejectValue("file", "my.validation.test", new String[] { form.getFile().toString() }, "");
		}
		if (result.hasErrors() == false) {
			String lang = form.getLang();
			String name = form.getFile().getOriginalFilename();
			if (lang.equals("ja-jp") && name.equals("psitem_ja.csv") == false) {
				result.rejectValue("file", "my.validation.test", new String[] { form.getFile().toString() }, "");
			} else if (lang.equals("en-jp") && name.equals("psitem_en.csv") == false) {
				result.rejectValue("file", "my.validation.test", new String[] { form.getFile().toString() }, "");
			} else if (lang.equals("zh-cn") && name.equals("psitem_zh.csv") == false) {
				result.rejectValue("file", "my.validation.test", new String[] { form.getFile().toString() }, "");
			} else if (lang.equals("zh-tw") && name.equals("psitem_zhtw.csv") == false) {
				result.rejectValue("file", "my.validation.test", new String[] { form.getFile().toString() }, "");
			}
		}
		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, PsItemForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
