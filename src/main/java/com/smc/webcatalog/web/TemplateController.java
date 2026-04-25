package com.smc.webcatalog.web;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateForm;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.TemplateFormValidator;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/lang/template")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class TemplateController extends BaseController {

	@Autowired
	TemplateService service;

	@Autowired
	TemplateFormValidator validator;

	@Autowired
    HttpServletRequest req;

	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml html;

	/**
	 * 管理系 > 言語 > テンプレート
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/{lang}"})
	public ModelAndView upsert(
			ModelAndView mav,
			@ModelAttribute("templateForm") TemplateForm myform,
			@PathVariable(name = "lang", required = false) String lang,
			@RequestParam(name = "modelState", required = false, defaultValue = "TEST") String modelState,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state
			) {

		// Set view
		mav.setViewName("/login/admin/lang/template");
		s_state.setView(ViewState.LANG.toString());

		// 取得
		ErrorObject err = new ErrorObject();
		ModelState m = ModelState.TEST;
		if (modelState != null && modelState.equals("PROD")) m = ModelState.PROD;
		Template temp = service.getLangAndModelState(lang, m, null, err); // 管理系はactive=null
		if (temp == null) {
			temp = new Template(lang);
		}
		// Map(Copy) Template -> Form
		modelMapper.map(temp, myform);

		//Add Form to View
		mav.addObject(myform);

		return mav;
	}
	
	/**
	 * State切り替え
	 *
	 */
	@GetMapping({ "/state" })
	public ModelAndView changeState(
										ModelAndView mav,
										@RequestParam(name = "state", required = false, defaultValue = "TEST") String state,
										@RequestParam(name = "id", required = false, defaultValue = "") String id,
										@ModelAttribute("SessionUser") User s_user,
										@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		ErrorObject err = new ErrorObject();
		User u = (User)s_user;

		String success = "";

		if (state != null && state.equals("PROD")) { // カテゴリにArchiveは無し
			err = service.changeStateToProd(id, u);
			if(err.isError()) {
				mav.addObject("error", err.getMessage() );
			} else {
				mav.addObject("message" , success + err.getCount());
			}
		}
		mav.setViewName("/login/admin/lang/template");
		s_state.setView(ViewState.LANG.toString());
		
		service.refreshTemplates(); // メモリ上のtemplate更新

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
			@Validated @ModelAttribute("templateForm") TemplateForm form,
			@RequestParam(name = "toProd", required = false, defaultValue = "") String toProd,
			@RequestParam(name = "replace", required = false) String replace,
			@ModelAttribute("SessionUser") User s_user,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/lang/template");

		log.debug(form.toString());

		// 1) - 7)までが基本的な更新処理の流れ

		Template temp = null;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			temp = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		} else {
			temp = new Template(form.getLang());
			validator.validateNew(result, form);
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// 5) フォームからTemplateにコピー
			modelMapper.map(form, temp);
			log.debug("Lang(FORM)=" + temp.toString());

			// 新規、またはReplaceのチェックがあれば取得
			html.Init(getLocale(temp.getLang()), messagesource);
			if ( (StringUtils.isEmpty(form.getId()) && StringUtils.isEmpty(form.getHeartCoreId())) || (replace != null && replace.equals("1") )) {
				service.setHeartCore(temp);
			}

			// 6) 保存
			ErrorObject obj = service.save(temp);
			
			if (temp.isActive()) {
				if ( temp.getState().equals(ModelState.TEST) && toProd !=  null && toProd.equals("1") ) { // TESTで Save&Prod
					service.changeStateToProd(temp.getId(), s_user);
					html.outputHtml( form.getLang() + "/index.html", temp.getHeader() + temp.getContents() + temp.getFooter() ); // 静的Html吐き出し
				}
				if (temp.getState().equals(ModelState.PROD)) {
					html.outputHtml( form.getLang() + "/index.html", temp.getHeader() + temp.getContents() + temp.getFooter() ); // 静的Html吐き出し
				}
			}
			service.refreshTemplates(); // メモリ上のtemplate更新

			// 7) フォームを更新(再編集用)
			modelMapper.map(temp, form);

			mav.addObject("is_success", !obj.isError());

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("form", temp);

		return mav;
	}
	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}
}
