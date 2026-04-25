package com.smc.webcatalog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.UserRepository;
import com.smc.webcatalog.dao.UserTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.UserForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class UserFormValidator {

	@Autowired
	UserRepository repo;

	@Autowired
	UserTemplateImpl temp;

	@Autowired
	UserServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, UserForm form) {
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
		// 同一loginIdが存在するか
		if (service.isLoginIdExists(form.getLoginId(), obj)) {
			result.rejectValue("loginId", "my.validation.test", new String[] { form.getLoginId() }, "");
		}
		// 入力がある場合、同一emailが存在するか
		if (StringUtils.isEmpty(form.getEmail()) == false && service.isEmailExists(form.getEmail(), obj)) {
			result.rejectValue("email", "my.validation.test", new String[] { form.getEmail() }, "");
		}
		if (StringUtils.isEmpty(form.getPassword())) {
			result.rejectValue("password", "my.validation.empty", new String[] { form.getPassword() }, "");
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, UserForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		Optional<User> u = repo.findById(form.getId());
		if (u.isPresent()) {
			User user = u.get();
			// nameが変更された場合、同一nameが存在するか
			if (user.getName().equals(form.getName()) == false) {
				user.setName(form.getName());
				if (service.isNameExists(user.getName(), obj)){
					result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
				}
			}
			// loginIdが変更された場合、同一loginIdが存在するか
			if (user.getLoginId().equals(form.getLoginId()) == false) {
				user.setLoginId(form.getLoginId());
				if (service.isLoginIdExists(form.getLoginId(), obj)) {
					result.rejectValue("name", "my.validation.test", new String[] { form.getLoginId() }, "");
				}
			}
			// 入力がある場合、同一emailが存在するか
			if (StringUtils.isEmpty(form.getEmail()) == false && form.getEmail().equals(user.getEmail()) == false) {
				user.setEmail(form.getEmail());
				if (service.isEmailExists(form.getEmail(), obj)) {
					result.rejectValue("name", "my.validation.test", new String[] { form.getEmail() }, "");
				}
			}

			ret = true;
		}


		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
