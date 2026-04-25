package com.smc.webcatalog.web;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.PasswordForm;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.UserForm;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.LoginFormValidator;
import com.smc.webcatalog.service.PasswordFormValidator;
import com.smc.webcatalog.service.UserFormValidator;
import com.smc.webcatalog.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/user")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class UserController extends BaseController {

	@Autowired
	UserService service;

	@Autowired
	UserFormValidator validator;

	@Autowired
	PasswordFormValidator passwordValidator;

	@Autowired
	LoginFormValidator loginValidator;

	@Autowired
	HttpSession session;

	/**
	 * 管理系 > ユーザー > 一覧
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "", "/"})
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/user/list");
		s_state.setView(ViewState.USER.toString());

		//リスト取得
		ErrorObject err = new ErrorObject();
		List<User> list = service.listAll(null, err);

		log.debug("list=" + list.toString());

		//Add Form to View
		mav.addObject("list", list);

		return mav;
	}

	/**
	 * 管理系 カテゴリ > 新規
	 * @param mav
	 * @param myform
	 * @return
	 */
	@RequestMapping(value = "/new")
	public ModelAndView create(
			ModelAndView mav,
			@ModelAttribute("userForm") UserForm myform,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/user/edit");

		//Set FormObject to view
		mav.addObject(myform);

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > 編集
	 * 編集のために、DBからデータを読んでフォームにセット
	 * @param myform
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("userForm") UserForm myform,
			@PathVariable(name = "id", required = false) String id) {

		// Set view
		mav.setViewName("/login/admin/user/edit");

		// Get by id
		ErrorObject obj = new ErrorObject();
		User u = service.get(id, obj);

		// パスワードは入力があった場合変更
		u.setPassword("");

		// Map(Copy) Category -> Form
		modelMapper.map(u, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("user", u);

		return mav;
	}

	/**
	 * POSTされたデータからDB更新
	 * @param mav
	 * @param form
	 * @param result
	 * @return
	 */
	@RequestMapping(value = "/post", method = RequestMethod.POST)
	public ModelAndView post(
			ModelAndView mav,
			@Validated @ModelAttribute("userForm") UserForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/user/edit");

		log.debug(form.toString());

		// 1) - 7)までが基本的な更新処理の流れ

		User u = new User();

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			u = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		}else {
			validator.validateNew(result, form);

		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// パスワードの入力があった場合
			if (StringUtils.isEmpty(form.getPassword()) == false)
			{
				BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
				u.setPassword(bCryptPasswordEncoder.encode( form.getPassword()));
				form.setPassword(u.getPassword());
			}
			else {
				form.setPassword(u.getPassword());
			}

			// 5) フォームからCategoryにコピー
			modelMapper.map(form, u);
			log.debug("Lang(FORM)=" + u.toString());

			// 6) 保存
			ErrorObject obj = service.save(u);

			// 7) フォームを更新(再編集用)
			u.setPassword("");
			modelMapper.map(u, form);

			mav.addObject("is_success", !obj.isError());

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("user", u);

		return mav;
	}

	/**
	 * 管理系 ユーザー > パスワード変更 画面表示
	 * @param mav
	 * @param myform
	 * @return
	 */
	@RequestMapping(value = "/password")
	public ModelAndView password(
			ModelAndView mav,
			@ModelAttribute("passwordForm") PasswordForm myform,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/user/password");
		s_state.setView(ViewState.USER.toString());

		ErrorObject obj = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");
		User u = service.get(s_user.getId(), obj);
		if (u == null) {
			mav.addObject("is_err", obj.getMessage());
		}

		myform.setId(u.getId());

		//Set FormObject to view
		mav.addObject(myform);

		return mav;
	}

	/**
	 * POSTされたデータからDB更新
	 * @param mav
	 * @param form
	 * @param result
	 * @return
	 */
	@RequestMapping(value = "/password/post", method = RequestMethod.POST)
	public ModelAndView changePassword(
			ModelAndView mav,
			@Validated @ModelAttribute("passwordForm") PasswordForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/user/password");

		log.debug(form.toString());

		// 1) - 7)までが基本的な更新処理の流れ

		ErrorObject err = new ErrorObject();

		User u = service.get(form.getId(), err);

		if (u != null) {
			passwordValidator.validate(result, form);

		}else {
			mav.addObject("is_error", err.getMessage());
			mav.addObject(form);
			return mav;
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
			u.setPassword(enc.encode(form.getAfter()));
			log.debug("User(FORM)=" + u.toString());

			// 6) 保存
			ErrorObject obj = service.save(u);

			// 7) フォームを更新(再編集用)
			form.setAfter("");
			form.setAgain("");
			form.setBefore("");

			mav.addObject("is_success", !obj.isError());
			// TODO SpringSecurity の再生成。ここでやる必要はないかも
			// s_user.setPassword(u.getPassword());

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("user", u);

		return mav;
	}

} // end class
