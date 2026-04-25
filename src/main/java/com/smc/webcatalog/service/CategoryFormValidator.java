package com.smc.webcatalog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.CategoryRepository;
import com.smc.webcatalog.dao.CategoryTemplateImpl;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryForm;
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
public class CategoryFormValidator {

	@Autowired
	CategoryRepository repo;

	@Autowired
	CategoryTemplateImpl temp;

	@Autowired
	CategoryServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, CategoryForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		// 2022/10/20 子のみ確認。親が違えばOK
		// 同一nameが存在するか
		ErrorObject obj = new ErrorObject();
		Category p = service.getWithChildren(form.getParentId(), null, obj);
		if (p != null && p.getChildren().size() > 0) { // 同じ親の子供で比較
			Category c = new Category();
			c.setParentId(form.getParentId());
			c.setName(form.getName());
			if (service.isNameExists(c, obj)) {
				result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
			}
		}
		// 同一slugが存在するか
		Category c = new Category();
		c.setParentId(form.getParentId());
		c.setSlug(form.getSlug());
		if (service.isSlugExists(c, obj)) {
			result.rejectValue("slug", "my.validation.test", new String[] { form.getSlug() }, "");
		}
		// 2024/11/07 NarrowDown追加。大カテゴリはCheckNG。エラーを返す。
		// 2024/10/24 Compare追加。大カテゴリはCheckNG。エラーを返す。
		if (p.isRoot() && form.isNarrowdown()) {
			result.rejectValue("narrowdown", "my.validation.test", new String[] { form.getName() }, "It cannot be used in the first level of category.");
		}
		if (p.isRoot() && form.isCompare()) {
			result.rejectValue("compare", "my.validation.test", new String[] { form.getName() }, "It cannot be used in the first level of category.");
		}
		if (form.getId() == null && form.isActive()) {
			result.rejectValue("compare", "my.validation.test", new String[] { form.getName() }, "It cannot be checked when adding a new category.");
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, CategoryForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		Optional<Category> c = repo.findById(form.getId());
		if (c.isPresent()) {
			Category ca = c.get();

			// nameが変更された場合、同一nameが存在するか
			if (ca.getName().equals(form.getName()) == false) {
				ca.setName(form.getName());
				if (service.isNameExists(ca, obj)){
					result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
				}
			}
			// slugが変更された場合、同一slugが存在するか
			Optional<Category> oP = repo.findById(form.getId());
			if (oP.isPresent()) {
				Category p = oP.get();
				p.setSlug(form.getSlug());
				if ((ca.getSlug() == null || ca.getSlug().equals(form.getSlug()) == false)
						&& service.isSlugExists(p, obj)) {
					result.rejectValue("slug", "my.validation.test", new String[] { form.getSlug() }, "");
				}
			}

			ret = true;
		}
		// 2024/10/24 Compare追加。大カテゴリはCheckNG。エラーを返す。
		Category p = service.get(form.getParentId(), obj);
		if (p.isRoot() && form.isNarrowdown()) {
			result.rejectValue("narrowdown", "my.validation.test", new String[] { form.getName() }, "It cannot be used in the first level of category.");
		}
		if (p.isRoot() && form.isCompare()) {
			result.rejectValue("compare", "my.validation.test", new String[] { form.getName() }, "It cannot be used in the first level of categories.");
		}

		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
