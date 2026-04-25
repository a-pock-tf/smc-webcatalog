package com.smc.webcatalog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.SeriesRepository;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class SeriesFormValidator {

	@Autowired
	SeriesRepository repo;

	@Autowired
	SeriesTemplateImpl temp;

	@Autowired
	SeriesServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, SeriesForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		Series s = new Series();
		s.setName(form.getName());
		s.setLang(form.getLang());
		s.setState(form.getState());
		s.setModelNumber(form.getModelNumber());

		// 同一nameはみない 2021/12/9
		// 同一nameが存在するか
//		if (service.isNameExists(s, obj)) {
//			result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
//		}
		// 同一modelNumberが存在するか
		if (service.isModelNumberExists(s.getModelNumber(), s.getState(), null, obj)) {
			result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "There is already a same SeriesID.");
		}

		// modelNumberには英数字とハイフン、アンダーバーのみ
		if (checkModelNumber(s.getModelNumber()) == false) {
			result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Only use a-z A-Z 0-9 _-");
		}
		// 言語の接尾字があるか。en-jpは-E、zh-cnは-ZH、zh-twは-ZHTWで終わっていること
		if (form.getLang() != null) {
			if (form.getLang().equals("en-jp") && form.getModelNumber().lastIndexOf("-E") != form.getModelNumber().length()-2) {
				result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Ending with -E ");
			} else if (form.getLang().equals("zh-cn") && form.getModelNumber().lastIndexOf("-ZH") != form.getModelNumber().length()-3) {
				result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Ending with -ZH ");
			} else if (form.getLang().equals("zh-tw")  && form.getModelNumber().lastIndexOf("-ZHTW") != form.getModelNumber().length()-5) {
				result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Ending with -ZHTW ");
			}
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, SeriesForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		Optional<Series> c = repo.findById(form.getId());
		if (c.isPresent()) {
			Series ca = c.get();
			// 同一nameはみない 2021/12/9
			// nameが変更された場合、同一nameが存在するか
//			if (ca.getName().equals(form.getName()) == false) {
//				ca.setName(form.getName());
//				if (service.isNameExists(ca, obj)){
//					result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
//				}
//			}
			// modelNumberが変更された場合、同一nameが存在するか
			if (ca.getModelNumber().equals(form.getModelNumber()) == false) {
				ca.setModelNumber(form.getModelNumber());
				if (service.isModelNumberExists(ca.getModelNumber(), ca.getState(), null, obj)){
					result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "There is already a same SeriesID.");
				}
				// modelNumberには英数字とハイフン、アンダーバーのみ
//				if (checkModelNumber(ca.getModelNumber()) == false) {
//					result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Only use a-z A-Z 0-9 _-");
//				}
			}
			// 言語の接尾字があるか。en-jpは-E、zh-cnは-ZH、zh-twは-ZHTWで終わっていること
			if (form.getLang() != null) {
				if (form.getLang().equals("en-jp") 
					&& (form.getModelNumber().lastIndexOf("-E") != form.getModelNumber().length()-2 && form.getModelNumber().equals("EX260-EN") == false)
				) {
					result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Ending with -E ");
				} else if (form.getLang().equals("zh-cn") && form.getModelNumber().lastIndexOf("-ZH") != form.getModelNumber().length()-3) {
					result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Ending with -ZH ");
				} else if (form.getLang().equals("zh-tw")  && form.getModelNumber().lastIndexOf("-ZHTW") != form.getModelNumber().length()-5) {
					result.rejectValue("modelNumber", "my.validation.test", new String[] { form.getModelNumber() }, "Ending with -ZHTW ");
				}
			}
			ret = true;
		}


		//2) その他のチェック
		// private chekcABC();

		return ret;

	}
	String reg = "^[a-zA-Z0-9_\\-]+$";
	private boolean checkModelNumber(String sid) {
		boolean ret = false;

		if (sid != null && sid.matches(reg)) {
			ret = true;
		}
		return ret;
	}

}
