package com.smc.discontinued.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.discontinued.dao.DiscontinuedCategoryRepository;
import com.smc.discontinued.dao.DiscontinuedCategoryTemplateImpl;
import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedCategoryForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class DiscontinuedCategoryFormValidator {

	@Autowired
	DiscontinuedCategoryRepository discontinuedCategoryRepository;

	@Autowired
	DiscontinuedCategoryTemplateImpl disconCategoryTemplate;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, DiscontinuedCategoryForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		List<DiscontinuedCategory> list = disconCategoryTemplate.listAll(form.getLang(), form.getState(), null);
		if (list != null && list.size() > 0) {
			for(DiscontinuedCategory c : list) {
				// 同一nameが存在するか
				if (c.getName().equals(form.getName())) {
					result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
				}
				// 同一slugが存在するか
				if (c.getSlug() != null && c.getSlug().equals(form.getSlug())) {
					result.rejectValue("slug", "my.validation.test", new String[] { form.getSlug() }, "");
				}
			}
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, DiscontinuedCategoryForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		Optional<DiscontinuedCategory> oC = discontinuedCategoryRepository.findById(form.getId());
		if (oC.isPresent()) {
			DiscontinuedCategory ca = oC.get();
			// nameが変更された場合、同一nameが存在するか
			if (ca.getName().equals(form.getName()) == false) {
				List<DiscontinuedCategory> list = disconCategoryTemplate.listAll(form.getLang(), form.getState(), null);
				for(DiscontinuedCategory c : list) {
					if (c.getName().equals(form.getName())) {
						result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
					}
				}
			}
			// slugが変更された場合、同一slugが存在するか
			if (ca.getSlug() == null || ca.getSlug().equals(form.getSlug()) == false) {
				List<DiscontinuedCategory> list = disconCategoryTemplate.listAll(form.getLang(), form.getState(), null);
				for(DiscontinuedCategory c : list) {
					if (c.getSlug() != null && c.getSlug().equals(form.getSlug())) {
						result.rejectValue("slug", "my.validation.test", new String[] { form.getSlug() }, "");
					}
				}
			}

			ret = true;
		}


		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
