package com.smc.discontinued.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedCategoryForm;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.discontinued.service.DiscontinuedCategoryFormValidator;
import com.smc.discontinued.service.DiscontinuedCategoryService;
import com.smc.discontinued.service.DiscontinuedSeriesService;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.web.ScreenStatusHolder;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/discontinued/category")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class DiscontinuedCategoryController extends DiscontinuedBaseController {

	@Autowired
	DiscontinuedCategoryService disconCategoryService;

	@Autowired
	DiscontinuedSeriesService disconSeriesService;

	@Autowired
	DiscontinuedCategoryFormValidator validator;

	@Autowired
    MessageSource messagesource;

	@Autowired
	HttpSession session;

	@Autowired
    HttpServletRequest req;

    @Autowired
	Environment env;

	// 管理系はシステムワイドにnull
	private Boolean active = null;

	/**
	 * Init ScreenState(Session)
	 * @return
	 */
	@ModelAttribute("SessionScreenState")
	ScreenStatusHolder getScreenState() {
		return new ScreenStatusHolder();
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
										@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		ErrorObject err = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");

		DiscontinuedCategory c = disconCategoryService.get(id, err);
		String success = "";

		if (state != null && state.equals("PROD")) { // カテゴリにArchiveは無し
			disconCategoryService.changeStateToProd(id, s_user);
			DiscontinuedCategory pC = disconCategoryService.getStateRefId(c, DiscontinuedModelState.PROD, err);
			List<DiscontinuedSeries> sList = disconSeriesService.listCategory(id, c.getState(),  true, err);
			for(DiscontinuedSeries s: sList) {
				err = disconSeriesService.changeStateToProd(s.getId(), pC.getId(), s_user);
				if(err.isError()) {
					mav.addObject("error", err.getMessage() );
				}
			}
			if(err.isError()) {
				mav.addObject("error", err.getMessage() );
			} else {
				mav.addObject("message" , success + err.getCount());
			}
		}
		mav.setViewName("/login/admin/discontinued/category/list");

		DiscontinuedModelState _state = DiscontinuedModelState.PROD;
		if (s_state.isProd() == false) _state = DiscontinuedModelState.TEST;
		// リスト表示設定
		setDispListParam(mav, null, s_state.getLang(), _state);

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > ソート
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 */
	@PostMapping({ "/sort" })
	public ModelAndView sort(
			ModelAndView mav,
			@RequestParam(name = "ids", required = false, defaultValue = "") String ids,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		String ret = "failed";

		DiscontinuedModelState _state = DiscontinuedModelState.PROD;
		if (s_state.isProd() == false) _state = DiscontinuedModelState.TEST;

		String[] arr = ids.split("-");
		String parentId = null;
		List<String> listId = new ArrayList<String>();

		ErrorObject err = new ErrorObject();
		for(String tmp : arr) {
			if (!StringUtils.isEmpty(tmp)) {
				listId.add(tmp.trim());
			}
		}
		if (listId.size() > 0)
		{
			err = disconCategoryService.sort(s_state.getLang(), _state, listId);
			if (!err.isError()) {
				ret = "success.count=" + err.getCount();
				mav.setViewName("redirect:/login/admin/discontinued/category/?message=" + ret);
			}
			else {
				mav.addObject("error", err.getMessage());
				// Set view
				mav.setViewName("redirect:/login/admin/discontinued/category/?error=error.");
			}
		}

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > 一覧
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 */
	@GetMapping({ "", "/", "/{id}"})//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView list(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@RequestParam(name = "state", required = false, defaultValue = "") String state,
			@RequestParam(name = "locale", required = false, defaultValue = "") String locale,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		mav.setViewName("/login/admin/discontinued/category/list");

		//change ModelState( in Session)
		if (!StringUtils.isEmpty(state)) {
			s_state.setProd(state.equals(DiscontinuedModelState.PROD.toString()));
		}
		s_state.setView(ViewState.DISCON_CATEGORY.toString());

		DiscontinuedModelState _state = DiscontinuedModelState.PROD;
		if (s_state.isProd() == false) _state = DiscontinuedModelState.TEST;

		if (!StringUtils.isEmpty(lang)) {
			s_state.setLang(lang);
		}
		String _lang =s_state.getLang();

		setDispListParam(mav, null, _lang, _state);

		return mav;
	}
	@GetMapping({ "/del/{id}" })
	public ModelAndView delete(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/discontinued/category/list");
		s_state.setView(ViewState.DISCON_CATEGORY.toString());

		ErrorObject err = new ErrorObject();
		String parentId = null;
		DiscontinuedCategory c = disconCategoryService.get(id, err);
		if (c != null) {

			log.debug("ScreenStatusHolder=" + s_state);

			err = disconCategoryService.checkDelete(id);
			if (err.isError()) {
				mav.addObject("error", err.getMessage());
			} else {
				err = disconCategoryService.delete(id);
				if (!err.isError()) {
					String res = "success.count=" + err.getCount();

					mav.setViewName("redirect:/login/admin/discontinued/category/?message=" + res);
				} else {
					mav.addObject("error", err.getMessage());
				}
			}

			// リスト表示設定
			DiscontinuedModelState state = DiscontinuedModelState.PROD;
			if (s_state.getState().equals("TEST")) state = DiscontinuedModelState.TEST;
			setDispListParam(mav, null, s_state.getLang(), state);

		} else {
			mav.addObject("error", "category not found.id=" + id);
		}

		return mav;
	}
	/**
	 * 管理系 > カテゴリ > 他国新規
	 * @param mav
	 * @param myform
	 * @param id langRefidになる
	 * @param lang
	 * @param s_state
	 * @note edit.htmlで他国のIDを取得するのが大変なので、ja-jpを元に他国のIDを取得しRedirect
	 * @return
	 */
	@RequestMapping(value = "/new/{id}/lang/{lang}", method = RequestMethod.GET)
	public ModelAndView newLang(
			ModelAndView mav,
			@ModelAttribute("categoryForm") DiscontinuedCategoryForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedCategory ca = disconCategoryService.get(id, obj);
		String setId = null;
		if (ca.getState().equals(DiscontinuedModelState.PROD)) {
			mav.setViewName("redirect:/login/admin/discontinued/category/edit/" + ca.getId() + "?lang="+ ca.getLang() + "&notTest=1");
			return mav;
		}
		else if (ca.getLang().equals(lang)) {
			setId = ca.getId();
		} else {
			List<DiscontinuedCategory> list = disconCategoryService.listLangRef(ca.getId(), obj);
			for(DiscontinuedCategory c : list) {
				if (c.getLang().equals(lang)) {
					setId = c.getId();
					break;
				}
			}
			if (setId == null) setId = ca.getId();
		}
		if (setId != null) {
			mav.setViewName("redirect:/login/admin/discontinued/category/new?langRefId=" + ca.getId());
		} else {
			// エラー
			mav.setViewName("redirect:/login/admin/discontinued/category/edit/" + ca.getId() + "?lang="+ ca.getLang() );
		}
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
			@ModelAttribute("categoryForm") DiscontinuedCategoryForm myform,
			@RequestParam(name = "langRefId", required = false, defaultValue = "") String langRefId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/discontinued/category/edit");

		ErrorObject obj = new ErrorObject();
		if (StringUtils.isEmpty(langRefId) == false) {
			setEditRefParam(mav, disconCategoryService.get(langRefId, obj));
		}

		{
			myform.setLang(s_state.getLang());
			String state = s_state.getState();
			DiscontinuedModelState st = DiscontinuedModelState.TEST;
			if  (state.equals("PROD")) st = DiscontinuedModelState.PROD;
			myform.setState(st);
			myform.setActive(true);
		}

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
			@ModelAttribute("categoryForm") DiscontinuedCategoryForm myform,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/discontinued/category/edit");

		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedCategory category = disconCategoryService.get(id, obj);
		s_state.setProd(category.getState().equals(DiscontinuedModelState.PROD));

		setEditRefParam(mav, category);

		// Map(Copy) Category -> Form
		modelMapper.map(category, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("category", category);

		return mav;
	}
	/**
	 * 管理系 > カテゴリ > 他国編集
	 * @param mav
	 * @param myform
	 * @param id
	 * @param lang
	 * @param s_state
	 * @note edit.htmlで他国のIDを取得するのが大変なので、ja-jpを元に他国のIDを取得しRedirect
	 * @return
	 */
	@RequestMapping(value = "/edit/{id}/lang/{lang}", method = RequestMethod.GET)
	public ModelAndView editLang(
			ModelAndView mav,
			@ModelAttribute("categoryForm") DiscontinuedCategoryForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedCategory ca = disconCategoryService.get(id, obj);
		String setId = null;
		if (ca.getLang().equals(lang)) {
			setId = ca.getId();
		} else {
			List<DiscontinuedCategory> list = disconCategoryService.listLangRef(id, obj);
			for(DiscontinuedCategory c : list) {
				if (c.getLang().equals(lang)) {
					setId = c.getId();
					break;
				}
			}
		}
		if (setId != null) {
			mav.setViewName("redirect:/login/admin/discontinued/category/edit/" + setId);
		}
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
			@RequestParam(name = "import_before", required = false, defaultValue = "") String beforeUrl,
			@Validated @ModelAttribute("categoryForm") DiscontinuedCategoryForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/discontinued/category/edit");

		log.debug(form.toString());

		User s_user = (User)session.getAttribute("SessionUser");
		// 1) - 7)までが基本的な更新処理の流れ

		DiscontinuedCategory category = new DiscontinuedCategory();

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			category = disconCategoryService.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		}else {
			//新規の場合はそのまま
			validator.validateNew(result, form);
		}


		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			boolean isUpdate = false;
			if (form.getId() != null && !StringUtils.isEmpty(form.getId())) {
				String afterSlug = form.getSlug();
				String preSlug = category.getSlug();
				if (preSlug == null && afterSlug != null) {
					isUpdate = true;
				} else if (preSlug.equals(afterSlug) == false) {
					isUpdate = true;
				} else if (form.getName().equals(category.getName()) == false) {
					isUpdate = true;
				}
			}

			// 5) フォームからCategoryにコピー
			modelMapper.map(form, category);
			log.debug("Category(FORM)="+category.toString());
			category.setUser(s_user);

			// 6) 保存
			ErrorObject obj = disconCategoryService.save(category);

			if (obj.isError() == false) {
				mav.addObject("is_success", "success");
			} else {
				mav.addObject("is_success", !obj.isError());
				mav.addObject("is_error", "Error!"+obj.getMessage());
			}
			// 7) フォームを更新(再編集用)
			modelMapper.map(category, form);


		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("categoryForm", form);
		mav.addObject("category", category);

		return mav;
	}
	// ========== private ==========
	private ErrorObject setDispListParam(ModelAndView mav, String id, String lang, DiscontinuedModelState state) {
		ErrorObject err = new ErrorObject();
		DiscontinuedCategory c = null;
		if (id == null || id.isEmpty())
		{
			List<DiscontinuedCategory> list = disconCategoryService.listAll(lang, state, err);
			mav.addObject("list", list);
		} else {
			c = disconCategoryService.get(id, err);
		}

		if (c != null) {
			log.debug("category=" + c.toString());
		} else { // カラでもlang,state,typeを入れておかないと「新規」ボタンを出せない。
			c = new DiscontinuedCategory();
			c.setLang(lang);
			c.setState(state);
		}

		//Add to View
		mav.addObject("category", c);

		//パンくず
		setBreadcrumb(mav, c);

		return err;
	}
	/**
	 * 編集用の国リストとPRODorTESTを用意
	 * @param mav
	 * @param category 新規はLangRefId
	 * @return
	 */
	private ErrorObject setEditRefParam(ModelAndView mav, DiscontinuedCategory category) {
		ErrorObject err = new ErrorObject();
		// Get by langRefId
		// langRefIdがnullなら元になるID
		String langBaseId = null;
		if (StringUtils.isEmpty(category.getLangRefId())) {
			langBaseId = category.getId();
		}
		else {
			langBaseId = category.getLangRefId();
		}
		List<DiscontinuedCategory> langRefList = disconCategoryService.listLangRef(langBaseId, err);
		DiscontinuedCategory langBaseCategory = disconCategoryService.get(langBaseId, err);
		if (langBaseCategory != null) {
			langRefList.add(0, langBaseCategory);
		}
		List<String> langs = new ArrayList<String>();
		for(DiscontinuedCategory c : langRefList) {
			langs.add(c.getLang());
		}

		// Get by TEST or PROD
		DiscontinuedCategory diffCategory = null;
		if (category.getState().equals(DiscontinuedModelState.TEST)) {
			diffCategory = disconCategoryService.getStateRefId(category, DiscontinuedModelState.PROD, err);
		} else {
			diffCategory = disconCategoryService.getStateRefId(category, DiscontinuedModelState.TEST, err);
		}

		mav.addObject("langRefList", langs);
		mav.addObject("diffCategory", diffCategory);
		mav.addObject("langBaseId", langBaseId);

		return err;
	}
	private void setBreadcrumb(ModelAndView mav, DiscontinuedCategory c) {
		List<DiscontinuedCategory> breadcrumb = null;
		ErrorObject err = new ErrorObject();

		if (c != null) {
			//Add to View
			if (breadcrumb != null) {
				mav.addObject("breadcrumb", breadcrumb);
			}

			//debug
			if (breadcrumb == null) {
				log.debug(err.getMessage());
			} else {
				for (DiscontinuedCategory _c : breadcrumb) {
					if (_c != null) {
						log.debug(_c.getName());
					}
				}
			}
		}
	}
	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh-tw") > -1) loc = Locale.TAIWAN;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}

}
