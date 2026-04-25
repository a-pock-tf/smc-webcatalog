package com.smc.webcatalog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.UserRepository;
import com.smc.webcatalog.dao.UserTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.PasswordForm;
import com.smc.webcatalog.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class PasswordFormValidator {

	@Autowired
	UserRepository repo;

	@Autowired
	UserTemplateImpl temp;

	@Autowired
	UserServiceImpl service;

	// 新規時のチェック
	public boolean validate(BindingResult result, PasswordForm form) {
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
			// 今のパスワードが存在するか
			User login = service.login(user.getLoginId(), form.getBefore(), obj);
			if (login == null) {
				result.rejectValue("before", "my.validation.test",  "");
			}

			// 次のパスワードが合っているか。
			// 同一loginIdが存在するか
			if (StringUtils.isEmpty( form.getAfter()) || StringUtils.isEmpty(form.getAfter()) ) {
				result.rejectValue("after", "my.validation.empty", "");
			}
			// 入力がある場合、同一emailが存在するか
			else if (form.getAfter().equals(form.getAgain()) == false) {
				result.rejectValue("again", "my.validation.login", "");
			}

			ret = true;
		} else {
			result.rejectValue("before", "my.validation.login", "");
		}
		return ret;
	}

}
