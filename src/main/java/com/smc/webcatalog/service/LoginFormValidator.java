package com.smc.webcatalog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.LoginForm;
import com.smc.webcatalog.model.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LoginFormValidator {

	@Autowired
	UserService service;

	// ログイン時のチェック
	public User validate(BindingResult result, LoginForm form) {
		User ret = null;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		// 存在するか
		ErrorObject err = new ErrorObject();
		User u = service.login(form.getLoginid(), form.getLoginpw(), err);
		if (u == null)
		{
			result.rejectValue("loginid", "my.validation.login", new String[] { "" }, "");
		}
		else if (err.isError())
		{
			if (err.getCode().equals(ErrorCode.E10001))
			{
				result.rejectValue("loginid", "my.validation.login", new String[] { "" }, "");
			}
		}

		ret = u;
		return ret;
	}
}
