package com.smc.webcatalog.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.SeriesLinkMasterForm;
import com.smc.webcatalog.model.SeriesLinkType;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.SeriesLinkMasterFormValidator;
import com.smc.webcatalog.service.SeriesLinkMasterService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/serieslinkmaster")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class SeriesLinkMasterController extends BaseController {
	@Autowired
	SeriesLinkMasterService service;

	@Autowired
	SeriesLinkMasterFormValidator validator;

	@Autowired
    HttpServletRequest req;

	/**
	 * 管理系 > リンクマスタ > 一覧
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "", "/"})
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/serieslinkmaster/list");
		s_state.setView(ViewState.SERIES_LINK_MASTER.toString());

		//リスト取得
		String lang = s_state.getLang();
		if (lang == null) lang = "ja-jp"
				;
		ErrorObject err = new ErrorObject();
		List<SeriesLinkMaster> list = service.findByLangAll(lang, null, err);

		log.debug("list=" + list.toString());

		//Add Form to View
		mav.addObject("list", list);

		return mav;
	}

	/**
	 * 管理系 > リンクマスタ > 新規
	 * @param mav
	 * @param myform
	 * @return
	 */
	@RequestMapping(value = "/new")
	public ModelAndView create(
			ModelAndView mav,
			@ModelAttribute("seriesLinkMasterForm") SeriesLinkMasterForm myform,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/serieslinkmaster/edit");

		String lang = s_state.getLang();
		if (lang == null) lang = "ja-jp";

		//Set FormObject to view
		myform.setLang(lang);
		mav.addObject(myform);
		mav.addObject("type", SeriesLinkType.values());

		return mav;
	}

	/**
	 * 管理系 > リンクマスタ > 編集
	 * 編集のために、DBからデータを読んでフォームにセット
	 * @param myform
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("seriesLinkMasterForm") SeriesLinkMasterForm myform,
			@PathVariable(name = "id", required = false) String id) {

		// Set view
		mav.setViewName("/login/admin/serieslinkmaster/edit");

		// Get by id
		ErrorObject obj = new ErrorObject();
		SeriesLinkMaster master = service.get(id, obj);

		// Map(Copy) SeriesLinkMaster -> Form
		modelMapper.map(master, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("type", SeriesLinkType.values());
		mav.addObject("master", master);

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
			@Validated @ModelAttribute("seriesLinkMasterForm") SeriesLinkMasterForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/serieslinkmaster/edit");

		log.debug(form.toString());

		// 1) - 7)までが基本的な更新処理の流れ

		SeriesLinkMaster master = null;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			master = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		}else {
			master = new SeriesLinkMaster("");
			validator.validateNew(result, form);
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// 5) フォームからCategoryにコピー
			modelMapper.map(form, master);
			log.debug("SeriesLinkMaster(FORM)=" + master.toString());

			// 6) 保存
			ErrorObject obj = service.save(master);

			// 7) フォームを更新(再編集用)
			modelMapper.map(master, form);

			mav.addObject("is_success", !obj.isError());

			// 成功でLangのContextを更新
			ErrorObject err = new ErrorObject();
			req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_PREFIX,  service.listAll(true, err));
			req.getServletContext().setAttribute(SeriesLinkMaster.APPLICATION_CONTEXT_ALL_PREFIX,  service.listAll(null, err));

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("type", SeriesLinkType.values());
		mav.addObject("master", master);

		return mav;
	}
}
