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
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.discontinued.model.DiscontinuedSeriesForm;
import com.smc.discontinued.service.DiscontinuedCategoryServiceImpl;
import com.smc.discontinued.service.DiscontinuedSeriesFormValidator;
import com.smc.discontinued.service.DiscontinuedSeriesServiceImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.web.ScreenStatusHolder;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/discontinued/series")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class DiscontinuedSeriesController extends DiscontinuedBaseController {

	@Autowired
	DiscontinuedCategoryServiceImpl disconCategoryService;

	@Autowired
	DiscontinuedSeriesServiceImpl disconSeriesService;

	@Autowired
	DiscontinuedSeriesFormValidator validator;

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
					@RequestParam(name = "cid", required = false, defaultValue = "") String cid,
					@RequestParam(name = "archive", required = false, defaultValue = "1") String archive,
					@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		ErrorObject err = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");

		boolean isArchive = false;
		if (state != null) {
			if (state.equals("PROD")) {
				DiscontinuedSeries s = disconSeriesService.get(id, err);
				DiscontinuedCategory c = disconCategoryService.get(s.getCategoryId(), err);
				DiscontinuedCategory pC = disconCategoryService.getStateRefId(c, DiscontinuedModelState.PROD, err);
				if (pC != null) {
					err = disconSeriesService.changeStateToProd(id, pC.getId(), s_user);
					if(err.isError()) {
						mav.addObject("is_error", err.getMessage() );
					}
					else {
						if (archive != null && archive.equals("1")) {
							isArchive = true;
						}
					}
					mav.addObject("message" , "success. count=" + err.getCount());
					mav.setViewName("/login/admin/discontinued/series/list");
				} else {
					mav.addObject("is_error", "PROD category is not found." );
				}
			}
			if ( state.equals("ARCHIVE") || isArchive) {
				err = disconSeriesService.changeStateToArchive(id, s_user);
				mav.addObject("message" , "success. count=" + err.getCount());
				mav.setViewName("/login/admin/discontinued/series/list");
			}
			if (state.equals("TEST")) {
				err = disconSeriesService.changeStateToTest(id);
				DiscontinuedSeries s = disconSeriesService.get(id, err);
				mav.setViewName("redirect:/login/admin/discontinued/series/edit/" + s.getStateRefId() + "?success=1");
				return mav;
			}
		}

		// カテゴリ取得。rootならカテゴリ無しを表示
		DiscontinuedSeries s = disconSeriesService.get(id, err);
		DiscontinuedCategory c = null;
		List<DiscontinuedSeries> sList = null;
		if (cid == null || cid.isEmpty())
		{
			// 今表示中の言語、ステータス、タイプのrootを取得し、最新シリーズを表示
			sList = disconSeriesService.listAll(s.getLang(), getState(s_state), err);
			c = disconCategoryService.get(sList.get(0).getCategoryId(), err);
		}
		else
		{
			c = disconCategoryService.get(cid, err);
			sList = disconSeriesService.listCategory(cid, c.getState(), null, err);
		}

		//Add to View
		mav.addObject("categoryId", c.getId());
		mav.addObject("categoryName", c.getName());
		mav.addObject("list", sList);

		//パンくず取得
		setBreadcrumb(mav, c);

		return mav;
	}

	/**
	 * 管理系 > シリーズ > ソート
	 * @param mav
	 * @param ids
	 * @return
	 */
	@PostMapping({ "/sort" })//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView sort(
			ModelAndView mav,
			@RequestParam(name = "ids", required = false, defaultValue = "") String ids,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {


		String ret = "failed";

		String[] arr = ids.split("-");
		String categoryId = null;
		List<String> listId = new ArrayList<String>();

		ErrorObject err = new ErrorObject();
		for(String tmp : arr) {
			if (!StringUtils.isEmpty(tmp)) {
				if (tmp.indexOf("[category]") > -1) {
					categoryId = tmp.replace("[category]", "");

					DiscontinuedCategory c = disconCategoryService.get(categoryId, err);
					setBreadcrumb(mav, c);
				}
				else {
					listId.add(tmp.trim());
				}
			}
		}
		//Set view
		mav.setViewName("redirect:/login/admin/discontinued/series/?categoryId=" + categoryId + "&error=error.");

		if (listId.size() > 0)
		{
			// categorySeriesのソート
			err = disconSeriesService.sort(categoryId, listId);
			if (!err.isError()) {
				ret = "success.count=" + err.getCount();
				mav.setViewName("redirect:/login/admin/discontinued/series/?categoryId=" + categoryId + "&message=" + ret);
			}
		}

		return mav;
	}

	/**
	 * 管理系 > シリーズ > 検索
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 * @note {id}が無い場合は全部だしてしまうと多いので、カテゴリに属さないシリーズの一覧
	 */
	@GetMapping("/search")//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView search(ModelAndView mav,
			@RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		//Set view
		mav.setViewName("/login/admin/discontinued/series/search_result");
		s_state.setView(ViewState.DISCON_SERIES.toString());
		s_state.setBackUrl("/login/admin/discontinued/series/search");
		s_state.setKeyword( keyword);

		ErrorObject err = new ErrorObject();
		List<DiscontinuedSeries> list = disconSeriesService.search(keyword, s_state.getLang(), getState(s_state), null, err);

		//Add to View
		mav.addObject("list", list);
		mav.addObject("keyword", keyword);

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > 一覧
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 */
	@GetMapping({ "", "/", "/{id}" })//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView list(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@RequestParam(name = "categoryId", required = false, defaultValue = "") String categoryId,
			@RequestParam(name = "state", required = false, defaultValue = "") String state,
			@RequestParam(name = "locale", required = false, defaultValue = "") String locale,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		mav.setViewName("/login/admin/discontinued/series/list");

		//change ModelState( in Session)
		if (!StringUtils.isEmpty(state)) {
			s_state.setProd(state.equals(DiscontinuedModelState.PROD.toString()));
		}
		s_state.setView(ViewState.DISCON_SERIES.toString());

		DiscontinuedModelState _state = DiscontinuedModelState.PROD;
		if (s_state.isProd() == false) _state = DiscontinuedModelState.TEST;

		if (!StringUtils.isEmpty(lang)) {
			s_state.setLang(lang);
		}
		String _lang =s_state.getLang();

		if (categoryId != null && categoryId.isEmpty() == false) {
			s_state.setBackUrl("/login/admin/discontinued/series?categoryId="+categoryId);
		}

		setDispListParam(mav, categoryId, _lang, _state);

		return mav;
	}

	@GetMapping({"/del/{id}"})
	public ModelAndView delete(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "cid", required = false, defaultValue = "") String categoryId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/discontinued/series/list");
		s_state.setView(ViewState.DISCON_SERIES.toString());

		log.debug("ScreenStatusHolder=" + s_state);

		ErrorObject err = new ErrorObject();
		boolean isDel = false;
		DiscontinuedSeries s = disconSeriesService.get(id, err);
		if (s != null) {
			err = disconSeriesService.checkDelete(id);
			if (err.isError() == false) {
				err = disconSeriesService.delete(id);
				if (!err.isError()) {
					String res = "success.count=" + err.getCount();
					isDel = true;
					mav.setViewName("redirect:/login/admin/discontinued/series/?categoryId=" + categoryId + "&message=" + res);
				}
				else {
					mav.addObject("is_error", err.getMessage());
				}
			} else {
				mav.addObject("is_error", err.getMessage());
			}
		} else {
			mav.addObject("is_error", "series not found.id=" + id);
		}
		if (isDel == false) {
			// カテゴリ取得。rootならカテゴリ無しを表示
			DiscontinuedCategory c = null;
			List<DiscontinuedSeries> sList = null;
			if (categoryId == null || categoryId.isEmpty() || categoryId.equals("undefined"))
			{
				sList = disconSeriesService.listAll(s.getLang(), s.getState(), err);

			}
			else
			{
				c = disconCategoryService.get(categoryId, err);
				sList = disconSeriesService.listCategory(categoryId, s.getState(),null, err);
			}
			if (c == null) {
				c = disconCategoryService.get(s.getCategoryId(), err);
			}

			log.debug("c=" + c.toString());

			//Add to View
			mav.addObject("categoryId", c.getId());
			mav.addObject("categoryName", c.getName());
			mav.addObject("list", sList);

			//パンくず取得
			setBreadcrumb(mav, c);
		}

		return mav;
	}
	/**
	 * 管理系 > シリーズ > 他国新規
	 * @param mav
	 * @param myform
	 * @param id langRefidになる
	 * @param lang
	 * @param categoryId シリーズから直接他国編集->国を選ぶとnullで来る
	 * @param s_state
	 * @note edit.htmlで他国のIDを取得するのが大変なので、他国のIDを元にlangRefIdを取得しRedirect
	 * @return
	 */
	@RequestMapping(value = "/new/{id}/lang/{lang}/category/{categoryId}", method = RequestMethod.GET)
	public ModelAndView newLang(
			ModelAndView mav,
			@ModelAttribute("seriesForm") DiscontinuedSeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries se = disconSeriesService.get(id, obj);
		String langRefId = null;
		if (se.getState().equals(DiscontinuedModelState.PROD)) {
			mav.setViewName("redirect:/login/admin/discontinued/series/edit/" + se.getId() + "?lang="+ se.getLang() + "&notTest=1");
			return mav;
		}
		else {
			//Set view
			mav.setViewName("/login/admin/discontinued/series/edit");
			s_state.setView(ViewState.DISCON_SERIES.toString());

			// categoryIdからlangと一致するcategoryIdを取得。
			DiscontinuedCategory baseC = disconCategoryService.get(categoryId, obj);
			DiscontinuedCategory c = disconCategoryService.getLangRefId(baseC, lang, obj);
			if (c != null && se != null) {
				// langRefのSeriesをコピーしておく。
				modelMapper.map(se, myform);

				myform.setId(null);
				myform.setCategoryId(c.getId());
				myform.setLang(c.getLang());
				if (baseC.getLangRefId() == null || baseC.getLangRefId().isEmpty()) myform.setLangRefId(se.getId());
				myform.setState(c.getState());
				myform.setActive(true);

				setBreadcrumb(mav, c);
			} else {
				// エラー
				mav.addObject("is_error", "categoryId is empty.id=" + categoryId);
			}

			// カテゴリ一覧
			if (lang == null || lang.isEmpty()) lang = s_state.getLang();
			List<DiscontinuedCategory> listCategory = disconCategoryService.listAll(lang, getState(s_state), obj);
			mav.addObject("listCategory", listCategory);

			//Set FormObject to view
			mav.addObject(myform);

			setEditRefParam(mav, se);

		}
		return mav;
	}

	/**
	 * 管理系 シリーズ > 新規
	 * @param mav
	 * @param myform
	 * @param categoryId
	 * @param langRefId
	 * @param lang
	 * @return
	 */
	@RequestMapping(value = "/new/category/{categoryId}")
	public ModelAndView create(
			ModelAndView mav,
			@ModelAttribute("seriesForm") DiscontinuedSeriesForm myform,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@RequestParam(name = "langRefId", required = false, defaultValue = "") String langRefId,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/discontinued/series/edit");
		s_state.setView(ViewState.DISCON_SERIES.toString());

		// Get Series
		ErrorObject err = new ErrorObject();
		boolean isSet = false;
		DiscontinuedCategory c = null;
		if (StringUtils.isEmpty(categoryId) == false && categoryId.equals("null") == false) {
			c = disconCategoryService.get(categoryId, err);
			if (c != null) {
				myform.setLang(c.getLang());
				myform.setCategoryId(c.getId());
				myform.setLang(c.getLang());
				myform.setState(c.getState());
				myform.setActive(true);
				isSet = true;

				setBreadcrumb(mav, c);
				setDispListParam(mav, categoryId, c.getLang(), c.getState());
			}
		}
		if (StringUtils.isEmpty(langRefId) == false) {
			DiscontinuedSeries s = disconSeriesService.get(langRefId, err);
			setEditRefParam(mav, s);
			if (isSet == false) {
				myform.setLang(lang);
				myform.setCategoryId(c.getId());
				myform.setLang(c.getLang());
				if (c.getLangRefId() == null || c.getLangRefId().isEmpty()) myform.setLangRefId(s.getId());
				myform.setState(c.getState());
				myform.setActive(true);
			}
		}
		// カテゴリ一覧
		if (lang == null || lang.isEmpty()) lang = s_state.getLang();
		List<DiscontinuedCategory> listCategory = disconCategoryService.listAll(lang, getState(s_state), err);
		mav.addObject("listCategory", listCategory);

		//Set FormObject to view
		mav.addObject(myform);

		return mav;
	}

	/**
	 * 管理系 > シリーズ > 他国編集
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
			@ModelAttribute("seriesForm") DiscontinuedSeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries se = disconSeriesService.get(id, obj);
		String setId = null;
		if (se.getLang().equals(lang)) {
			setId = se.getId();
		} else {
			List<DiscontinuedSeries> list = disconSeriesService.listLangRef(id, obj);
			for(DiscontinuedSeries s : list) {
				if (s.getLang().equals(lang) && s.getState().equals(se.getState())) {
					setId = s.getId();
					break;
				}
			}
		}
		if (setId != null) {
			mav.setViewName("redirect:/login/admin/discontinued/series/edit/" + setId);
		}
		return mav;
	}
	/**
	 * 管理系 > シリーズ > 編集
	 * 編集のために、DBからデータを読んでフォームにセット
	 * @param myform
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("seriesForm") DiscontinuedSeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "categoryId", required = false, defaultValue = "") String categoryId,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/discontinued/series/edit");
		s_state.setView(ViewState.DISCON_SERIES.toString());
		String cate = s_state.getKeyword();

		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries s = disconSeriesService.get(id, obj);

		// Get Category
		ErrorObject err = new ErrorObject();
		DiscontinuedCategory c = null;
		if (StringUtils.isEmpty(categoryId) == false) {
			c = disconCategoryService.get(categoryId, err);
			if (c != null)
			{
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());
				setBreadcrumb(mav, c);
			}
		} else if (StringUtils.isEmpty(cate) == false) {
			c = disconCategoryService.get(cate, err);
			if (c != null)
			{
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());
				setBreadcrumb(mav, c);
			}
		} else {
			c = disconCategoryService.get(s.getCategoryId(), err);
			if (c != null)
			{
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());
				setBreadcrumb(mav, c);
			}
		}

		// Map(Copy) DiscontinuedSeries -> Form
		modelMapper.map(s, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("series", s);

		// CategorySeries
		// 紐づけのあるカテゴリ一覧
		List<DiscontinuedCategory> listCategory = disconCategoryService.listAll(s.getLang(), s.getState(), err);
		mav.addObject("listCategory", listCategory);

		setEditRefParam(mav, s);

		return mav;
	}

	/**
	 * 管理系 > シリーズ > 編集
	 * 編集のために、DBからデータを読んでフォームにセット
	 * @param myform
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "/copy/{id}", method = RequestMethod.GET)
	public ModelAndView copy(
			ModelAndView mav,
			@ModelAttribute("seriesForm") DiscontinuedSeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/discontinued/series/edit");
		s_state.setView(ViewState.DISCON_SERIES.toString());
		String categoryId = s_state.getKeyword();

		// Get Category
		ErrorObject err = new ErrorObject();
		DiscontinuedCategory c = null;
		if (StringUtils.isEmpty(categoryId) == false) {
			c = disconCategoryService.get(categoryId, err);
			if (c != null)
			{
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());
				setBreadcrumb(mav, c);
			}
		}

		// Get by id
		ErrorObject obj = new ErrorObject();
		DiscontinuedSeries s = disconSeriesService.get(id, obj);
		String seriesId = s.getId();
		s.setId(null);

		// Map(Copy) Category -> Form
		modelMapper.map(s, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("series", s);

		// CategorySeries
		// 紐づけのあるカテゴリ一覧
		List<DiscontinuedCategory> listCategory = disconCategoryService.listAll(s.getLang(), s.getState(), err);
		mav.addObject("listCategory", listCategory);

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
			@RequestParam(name = "categoryId", required = false, defaultValue = "") String categoryId,
			@Validated @ModelAttribute("seriesForm") DiscontinuedSeriesForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/discontinued/series/edit");

		log.debug(form.toString());

		User s_user = (User)session.getAttribute("SessionUser");

		// 1) - 7)までが基本的な更新処理の流れ

		DiscontinuedSeries s = new DiscontinuedSeries();
		boolean isNew = false;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			s = disconSeriesService.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		}else {
			//新規の場合はそのまま
			validator.validateNew(result, form);
			isNew = true;
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// 5) フォームからSeriesにコピー
			modelMapper.map(form, s);
			log.debug("Series(FORM)="+s.toString());

			s.setUser(s_user);

			// 6) 保存
			ErrorObject obj = disconSeriesService.save(s);

			// 7) フォームを更新(再編集用)
			modelMapper.map(s, form);

			if (obj.isError()) {
				mav.addObject("is_error", "Error!"+obj.getMessage());
			} else {
				mav.addObject("is_success", "success");
			}
			mav.addObject("series", s);

			setEditRefParam(mav, s);

		} else {

			// ページへ戻り
			if (StringUtils.isEmpty(form.getId())) {

			}
		}
		// カテゴリ準備
		DiscontinuedCategory c = null;
		if (StringUtils.isEmpty(categoryId) == false) {
			ErrorObject err = new ErrorObject();
			c = disconCategoryService.get(categoryId, err);
			if (c != null)
			{
				mav.addObject("category", c);
				setBreadcrumb(mav, c);
				mav.addObject("categoryId", categoryId);

			}
		}
		ErrorObject err = new ErrorObject();
		// 紐づけのあるカテゴリ一覧
		List<DiscontinuedCategory> listCategory = disconCategoryService.listAll(s.getLang(), s.getState(), err);
		mav.addObject("listCategory", listCategory);

		mav.addObject(form);

		return mav;
	}

	// ========== private ==========
	private ErrorObject setDispListParam(ModelAndView mav, String categoryid, String lang, DiscontinuedModelState state) {
		ErrorObject err = new ErrorObject();
		List<DiscontinuedSeries> list = null;
		DiscontinuedCategory c = null;
		if (categoryid == null || categoryid.isEmpty())
		{
			list = disconSeriesService.listAllSortByEndDate(lang, state, false, err);
			mav.addObject("list", list);
		} else {
			c = disconCategoryService.get(categoryid, err);
			list = disconSeriesService.listCategory(categoryid, c.getState(), null, err);
			mav.addObject("list", list);
			mav.addObject("categoryId", categoryid);
		}

		if (list == null || list.size() == 0) {
			// カラでもlang,stateを入れておかないと「新規」ボタンを出せない。
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
	 * 編集用の国リストとPRODorTEST、ARCHIVEを用意
	 * @param mav
	 * @param category 新規はLangRefId
	 * @return
	 */
	private ErrorObject setEditRefParam(ModelAndView mav, DiscontinuedSeries se) {
		ErrorObject err = new ErrorObject();
		// Get by langRefId
		// langRefIdがnullなら元になるID
		String langBaseId = null;
		if (StringUtils.isEmpty(se.getLangRefId())) {
			langBaseId = se.getId();
		}
		else {
			langBaseId = se.getLangRefId();
		}
		List<DiscontinuedSeries> langRefList = disconSeriesService.listLangRef(langBaseId, err);
		DiscontinuedSeries langBaseCategory = disconSeriesService.get(langBaseId, err);
		langRefList.add(0, langBaseCategory);
		List<String> langs = new ArrayList<String>();
		if (langRefList != null) {
			for(DiscontinuedSeries c : langRefList) {
				if (c != null) {
					langs.add(c.getLang());
				}
			}
		}

		// Get by TEST or PROD
		DiscontinuedSeries diffSeries = null;
		if (se.getState().equals(DiscontinuedModelState.TEST)) {
			List<DiscontinuedSeries> list = disconSeriesService.getStateRefId(se, DiscontinuedModelState.PROD, err);
			if (list != null && list.size() > 0) diffSeries = list.get(0);
		} else {
			List<DiscontinuedSeries> list = disconSeriesService.getStateRefId(se, DiscontinuedModelState.TEST, err);
			if (list != null && list.size() > 0) diffSeries = list.get(0);
		}
		// Get Archive
		List<DiscontinuedSeries> list = disconSeriesService.getStateRefId(se, DiscontinuedModelState.ARCHIVE, err);

		mav.addObject("langRefList", langs);
		mav.addObject("diffSeries", diffSeries);
		mav.addObject("langBaseId", langBaseId);
		mav.addObject("listArchive", list);

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
	private DiscontinuedModelState getState(ScreenStatusHolder s_state) {
		DiscontinuedModelState enumState = DiscontinuedModelState.PROD;
		if (s_state.getState().equals("TEST")) enumState = DiscontinuedModelState.TEST;
		else if (s_state.getState().equals("ARCHIVE")) enumState = DiscontinuedModelState.ARCHIVE;
		return enumState;
	}
	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh-tw") > -1) loc = Locale.TAIWAN;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}

}
