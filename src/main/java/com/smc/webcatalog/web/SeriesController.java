package com.smc.webcatalog.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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

import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesForm;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.CategorySeriesService;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.FaqCategoryService;
import com.smc.webcatalog.service.NarrowDownService;
import com.smc.webcatalog.service.SeriesFormValidator;
import com.smc.webcatalog.service.SeriesLinkMasterService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/series")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class SeriesController extends BaseController {

	@Autowired
	SeriesService service;

	@Autowired
	SeriesFormValidator validator;

	@Autowired
	CategoryService categoryService;

	@Autowired
	CategorySeriesService csService;

	@Autowired
	SeriesLinkMasterService sLinkMasterService;

	@Autowired
	TemplateService templateService;

	@Autowired
	TemplateCategoryService templateCategoryService;

	@Autowired
	FaqCategoryService faqService;

	@Autowired
	NarrowDownService narrowDownService;
	
	@Autowired
    MessageSource messagesource;

    @Autowired
	Environment env;

    @Autowired
	LibHtml html;

    @Autowired
	HttpSession session;

	private String htmlPath = "";

	private void Init() {
		try {
			htmlPath = env.getProperty("smc.webcatalog.static.page.path");
		} catch (Exception e) {
			e.printStackTrace();
	    }
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

		// カテゴリ取得。rootならカテゴリ無しを表示
		Category c = null;
		List<Series> sList = null;
		if (cid == null || cid.isEmpty())
		{
			// 今表示中の言語、ステータス、タイプのrootを取得し、最新シリーズを表示
			List<Category> list = categoryService.listAll(s_state.getLang(), s_state.getViewState(), s_state.getCategoryType(), err);
			c = list.get(0);
			sList = c.getSeriesList();
		}
		else
		{
			c = categoryService.get(cid, err);
			sList = c.getSeriesList();
		}

		boolean isArchive = false;
		if (state != null) {
			if (state.equals("PROD")) {
				err = service.changeStateToProd(id, s_user);
				if(err.isError()) {
					mav.addObject("error", err.getMessage() );
				}
				else {
					Init();
					if (archive != null && archive.equals("1")) {
						isArchive = true;
					}
					// OTHERは静的ファイルは作らない。CATALOGから取得
					if (s_state.getCategoryType().equals(CategoryType.CATALOG)) outputHtml(id);
				}
				mav.addObject("message" , "success. count=" + err.getCount());
				mav.setViewName("/login/admin/series/list");
			}
			if ( state.equals("ARCHIVE") || isArchive) {
				err = service.changeStateToArchive(id, s_user);
				mav.addObject("message" , "success. count=" + err.getCount());
				mav.setViewName("/login/admin/series/list");
			}
			if (state.equals("TEST")) {
				err = service.changeStateToTest(id);
				Series s = service.get(id, err);
				mav.setViewName("redirect:/login/admin/series/edit/" + s.getStateRefId() + "?success=1");
				return mav;
			}
		}


		if (c.getParentId() == null || c.getParentId().isEmpty()) {
			// rootの場合、最新シリーズ取得
			c = categoryService.getWithSeries(c.getId(), null, err);
			err = new ErrorObject();
			sList = service.listAll200(s_state.getLang(), s_state.getViewState(), null, err);
		}
		else {
			c = categoryService.getWithSeries(c.getId(), null, err);
			sList = c.getSeriesList();
		}

		log.debug("c=" + c.toString());

		//Add to View
		mav.addObject("categoryId", c.getId());
		mav.addObject("parentId", c.getParentId());
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
			if (tmp != null && tmp.isEmpty() == false) {
				if (tmp.indexOf("[category]") > -1) {
					categoryId = tmp.replace("[category]", "");

					Category c = categoryService.get(categoryId, err);
					setBreadcrumb(mav, c);
				}
				else {
					listId.add(tmp.trim());
				}
			}
		}
		//Set view
		mav.setViewName("redirect:/login/admin/series/?categoryId=" + categoryId + "&error=error.");

		if (listId.size() > 0)
		{
			// categorySeriesのソート
			err = service.sort(categoryId, listId);
			if (!err.isError()) {
				ret = "success.count=" + err.getCount();
				mav.setViewName("redirect:/login/admin/series/?categoryId=" + categoryId + "&message=" + ret);
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
		mav.setViewName("/login/admin/series/search_result");
		s_state.setView(ViewState.SERIES.toString());
		s_state.setBackUrl("/login/admin/series/search");
		s_state.setKeyword( keyword);

		ErrorObject err = new ErrorObject();
		List<Series> list = service.search(keyword, s_state.getLang(), s_state.getViewState(), null, err);

		//Add to View
		mav.addObject("list", list);
		mav.addObject("keyword", keyword);

		return mav;
	}


	/**
	 * 管理系 > シリーズ > 一覧
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 * @note {id}が無い場合は全部だしてしまうと多いので、カテゴリに属さないシリーズの一覧
	 */
	@GetMapping({ "", "/", "/{id}" })//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView list(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "type", required = false, defaultValue = "") String type,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@RequestParam(name = "state", required = false, defaultValue = "") String state,
			@RequestParam(name = "categoryId", required = false, defaultValue = "") String categoryId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/series/list");
		s_state.setView(ViewState.SERIES.toString());
		s_state.setBackUrl("/login/admin/series/list");
		s_state.setKeyword(categoryId);

		//change ModelState( in Session)
		if (state != null && state.isEmpty() == false) {
			s_state.setProd(state.equals(ModelState.PROD.toString()));
		}
 		ModelState _state = ModelState.PROD;
		if (s_state.isProd() == false) _state = ModelState.TEST;

		if (type != null && type.isEmpty() == false) {
			s_state.setType(type);
		}
		CategoryType _type = CategoryType.CATALOG;
		if (!s_state.getType().equals(CategoryType.CATALOG.toString())) _type = CategoryType.OTHER;

		if (lang != null && lang.isEmpty() == false) {
			s_state.setLang(lang);
		}
		String _lang = s_state.getLang();

		log.debug("ScreenStatusHolder=" + s_state);

		// カテゴリ取得。rootならカテゴリ無しを表示
		ErrorObject err = new ErrorObject();
		Category c = null;
		List<Series> sList = null;
		if (categoryId == null || categoryId.isEmpty())
		{
			// 今表示中の言語、ステータス、タイプのrootを取得し、シリーズを表示
			List<Category> list = categoryService.listAll(_lang, _state, _type, err);
			if (list != null) {
				c = list.get(0);
				sList = c.getSeriesList();
			}
		}
		else
		{
			c = categoryService.getWithSeries(categoryId, true, err);
			sList = c.getSeriesList();
		}
		if (c != null) {
			if (StringUtils.isEmpty(c.getParentId())) {
				// rootの場合、シリーズも取得
				c = categoryService.getWithSeries(c.getId(), null, err);
				err = new ErrorObject();
				sList = service.listAll200(_lang, _state, null, err); // 200件限定
			}
			else {
				c = categoryService.getWithSeries(c.getId(), null, err);
				sList = c.getSeriesList();
			}
			if (c != null) {
				log.debug("c=" + c.toString());

				//Add to View
				mav.addObject("categoryId", c.getId());
				mav.addObject("parentId", c.getParentId());
				mav.addObject("categoryName", c.getName());
			}
		}
		// プレビュー表示用。
		if (sList.size() > 0 && c != null) {
			for(Series s : sList) {
				s.setCatpansString(c.getSlug());
			}
		}
		//Add to View
		mav.addObject("list", sList);

		//パンくず取得
		setBreadcrumb(mav, c);

		return mav;
	}


	@GetMapping({"/del/{id}"})
	public ModelAndView delete(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "cid", required = false, defaultValue = "") String categoryId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/series/list");
		s_state.setView(ViewState.SERIES.toString());

		log.debug("ScreenStatusHolder=" + s_state);

		ErrorObject err = new ErrorObject();
		boolean isDel = false;
		Series s = service.get(id, err);
		if (s != null) {
			err = service.checkDelete(id);
			if (err.isError() == false) {
				
				// PRODなら公開HTMLも削除
				if (s.getState().equals(ModelState.PROD)) {
					service.deleteHtml(s);
				}
				
				narrowDownService.deleteSeriesValue(id);
				faqService.delete(id);
				
				err = service.delete(id);
				if (!err.isError()) {
					String res = "success.count=" + err.getCount();
					isDel = true;
					mav.setViewName("redirect:/login/admin/series/?categoryId=" + categoryId + "&message=" + res);
				}
				else {
					mav.addObject("error", err.getMessage());
				}
			} else {
				mav.addObject("error", err.getMessage());
			}
		} else {
			mav.addObject("error", "series not found.id=" + id);
		}
		if (isDel == false) {
			// カテゴリ取得。rootならカテゴリ無しを表示
			Category c = null;
			List<Series> sList = null;
			if (categoryId == null || categoryId.isEmpty() || categoryId.equals("undefined"))
			{
				// 今表示中の言語、ステータス、タイプのrootを取得し、紐づいていないシリーズを表示
				CategoryType type = CategoryType.CATALOG;
				if (s_state.getType().equals("OTHER")) type = CategoryType.OTHER;
				List<Category> list = categoryService.listAll(s.getLang(), s.getState(), type, err);
				c = list.get(0);
				sList = c.getSeriesList();
			}
			else
			{
				c = categoryService.get(categoryId, err);
				sList = c.getSeriesList();
			}
			if (StringUtils.isEmpty(c.getParentId())) {
				// rootの場合、紐づいていないシリーズも取得
				c = categoryService.getWithSeries(c.getId(), null, err);
				err = new ErrorObject();
				sList = service.listAll200(s.getLang(), s.getState(), null, err);
			}
			else {
				c = categoryService.getWithSeries(c.getId(), null, err);
				sList = c.getSeriesList();
			}

			log.debug("c=" + c.toString());

			//Add to View
			mav.addObject("categoryId", c.getId());
			mav.addObject("parentId", c.getParentId());
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
	 * @param lang 新規に作成する国
	 * @param categoryId シリーズから直接他国編集->国を選ぶとnullで来る。これはidと同じ国なので、langRefからlangと一致するcategoryId
	 * @param s_state
	 * @return
	 */
	@RequestMapping(value = "/new/{id}/lang/{lang}/category/{categoryId}", method = RequestMethod.GET)
	public ModelAndView newLang(
			ModelAndView mav,
			@ModelAttribute("seriesForm") SeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		mav.setViewName("/login/admin/series/edit");
		s_state.setView(ViewState.SERIES.toString());

		// Get by id
		ErrorObject obj = new ErrorObject();
		Series se = service.get(id, obj);
		String langRefId = null;
		if (se.getState().equals(ModelState.PROD)) {
			// エラー
			mav.setViewName("redirect:/login/admin/series/edit/" + se.getId() + "?lang="+ se.getLang() + "&notTest=1");
			return mav;
		} else {
			// categoryIdからlangと一致するcategoryIdを取得。
			Category baseC = categoryService.get(categoryId, obj);
			Category c = categoryService.getLangRefId(baseC, lang, obj);
			if (c != null) {
				Category parent = categoryService.get(c.getParentId(), obj);
				if (parent != null && parent.getParentId() != null && parent.getParentId().isEmpty() == false) {
					String bread  = "●%slug1%name1#%slug2%name2#";
					myform.setBreadcrumb(bread.replace("slug1", parent.getSlug()).replace("name1", parent.getName()).replace("slug2", c.getSlug()).replace("name2", c.getName()) );
				}
				if (se.getLangRefId() == null || se.getLangRefId().equals("")) {
					langRefId = se.getId();
				} else {
					langRefId = se.getLangRefId();
				}
				modelMapper.map(se, myform);
				myform.setId("");
				String[] tmps = new String[1];
				tmps[0] = c.getId();
				myform.setCategoryList(tmps);
				s_state.setKeyword(c.getId());
				myform.setLang(c.getLang());
				myform.setLangRefId(langRefId);
				myform.setOldId("");
				myform.setState(c.getState());
				myform.setActive(true);

				setBreadcrumb(mav, c);

				List<Category> tmp = new ArrayList<Category>();
				tmp.add(c);
				mav.addObject("listCategory", tmp);
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());

			} else {
				// エラー
				mav.addObject("error", "categoryId is empty.id=" + categoryId);
			}
			// langのカテゴリ一覧を取得
			CategoryType type = CategoryType.CATALOG;
			if (baseC != null) type = baseC.getType();
			List<Category> listAllCategory = categoryService.listAll(lang, baseC.getState(), baseC.getType(), obj);
			Category root = categoryService.getRoot(lang, baseC.getState(), baseC.getType(), obj);

			// OTHERも選べるように下部へ追加
			if (type == CategoryType.CATALOG) type = CategoryType.OTHER;
			else type=CategoryType.CATALOG;

			List<Category> listAllCategory2 = categoryService.listAll(baseC.getLang(), baseC.getState(), type, obj);
			if (listAllCategory2 != null && listAllCategory2.size() > 0) {
				Category root2 = categoryService.getRoot(baseC.getLang(), baseC.getState(), type, obj);
				List<Category> lc = getOptionCategory(listAllCategory, root);
				lc.addAll(getOptionCategory(listAllCategory2, root2));
				mav.addObject("listAllCategory", lc);
			} else {
				mav.addObject("listAllCategory", getOptionCategory(listAllCategory, root));
			}

			// 紐づけを追加するためのリンクマスター
			List<SeriesLinkMaster> listLinkMaster = sLinkMasterService.findByLangAll(myform.getLang(), null, obj);
			mav.addObject("listLinkMaster", listLinkMaster);

			//Set FormObject to view
			mav.addObject(myform);

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
	 * @note 他国編集はnewLang()
	 * @return
	 */
	@RequestMapping(value = "/new/category/{categoryId}")
	public ModelAndView create(
			ModelAndView mav,
			@ModelAttribute("seriesForm") SeriesForm myform,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/series/edit");
		s_state.setView(ViewState.SERIES.toString());

		// Get Series
		ErrorObject err = new ErrorObject();
		boolean isSet = false;
		Category c = null;
		if (StringUtils.isEmpty(categoryId) == false && categoryId.equals("null") == false) {
			c = categoryService.get(categoryId, err);
			if (c != null) {
				Category parent = categoryService.get(c.getParentId(), err);
				if (parent != null && parent.getParentId() != null && parent.getParentId().isEmpty() == false) {
					String bread  = "●%slug1%name1#%slug2%name2#";
					myform.setBreadcrumb(bread.replace("slug1", parent.getSlug()).replace("name1", parent.getName()).replace("slug2", c.getSlug()).replace("name2", c.getName()) );
				}
				myform.setLang(c.getLang());
				myform.setState(c.getState());
				myform.setActive(true);
				isSet = true;

				setBreadcrumb(mav, c);
			}
		} else {
			// エラー
			mav.addObject("error", "categoryId is empty.id=" + categoryId);
		}
		// 紐づけを追加するための全カテゴリ一覧
		CategoryType type = CategoryType.CATALOG;
		if (c != null) type = c.getType();
		List<Category> listAllCategory = categoryService.listAll(myform.getLang(), s_state.getViewState(), type, err);
		Category root = categoryService.getRoot(myform.getLang(), s_state.getViewState(), type, err);

		// OTHERも選べるように下部へ追加
		if (type == CategoryType.CATALOG) type = CategoryType.OTHER;
		else type=CategoryType.CATALOG;

		List<Category> listAllCategory2 = categoryService.listAll(myform.getLang(), s_state.getViewState(), type, err);
		if (listAllCategory2 != null && listAllCategory2.size() > 0) {
			Category root2 = categoryService.getRoot(myform.getLang(), s_state.getViewState(), type, err);
			List<Category> lc = getOptionCategory(listAllCategory, root);
			lc.addAll(getOptionCategory(listAllCategory2, root2));
			mav.addObject("listAllCategory", lc);
		} else {
			mav.addObject("listAllCategory", getOptionCategory(listAllCategory, root));
		}

		// 紐づけを追加するためのリンクマスター
		List<SeriesLinkMaster> listLinkMaster = sLinkMasterService.findByLangAll(myform.getLang(), null, err);
		mav.addObject("listLinkMaster", listLinkMaster);

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
			@ModelAttribute("seriesForm") SeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		// Get by id
		ErrorObject obj = new ErrorObject();
		Series se = service.get(id, obj);
		String setId = null;
		if (se.getLang().equals(lang)) {
			setId = se.getId();
		} else {
			List<Series> list = service.listLangRef(id, obj);
			for(Series s : list) {
				if (s.getLang().equals(lang) && s.getState().equals(se.getState())) {
					setId = s.getId();
					Category baseC = categoryService.get(s_state.getKeyword(), obj) ;// categoryIdが入っている
					if (baseC != null) {
						Category c = categoryService.getLangRefId(baseC, lang, obj);
						s_state.setKeyword(c.getId());
					}
					break;
				}
			}
		}
		if (setId != null) {
			mav.setViewName("redirect:/login/admin/series/edit/" + setId);
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
			@ModelAttribute("seriesForm") SeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/series/edit");
		s_state.setView(ViewState.SERIES.toString());
		String categoryId = s_state.getKeyword();

		// Get Category
		ErrorObject err = new ErrorObject();
		Category c = null;
		if (StringUtils.isEmpty(categoryId) == false) {
			c = categoryService.get(categoryId, err);
			if (c != null)
			{
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());
				setBreadcrumb(mav, c);
				s_state.setKeyword(c.getId());
			}
		}

		// Get by id
		ErrorObject obj = new ErrorObject();
		Series s = service.getWithCategory(id, null, obj);

		// Map(Copy) Category -> Form
		modelMapper.map(s, myform);
		myform.setSearchword(String.join(",", s.getKeyword()));

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("series", s);

		// CategorySeries
		// 紐づけのあるカテゴリ一覧
		List<Category> listCategory = categoryService.listCategoryFromSeries(s.getId(), null, err);
//		List<CategorySeries> listCategory = s.getCategorySeries(); // カテゴリ名の表示に必要なので、Categoryが必要。
		if (listCategory != null) {
			String[] caList = new String[listCategory.size()];
			int cnt = 0;
			for(Category ca : listCategory) {
				caList[cnt] = ca.getId();
				cnt++;
			}
			myform.setCategoryList(caList);
		}
		mav.addObject("listCategory", listCategory);

		// 紐づけを追加するための全カテゴリ一覧
		CategoryType type = CategoryType.CATALOG;
		if (c != null) type = c.getType();
		List<Category> listAllCategory = categoryService.listAll(s.getLang(), s.getState(), type, err);
		Category root = categoryService.getRoot(s.getLang(), s.getState(), type, err);

		// OTHERも選べるように下部へ追加
		if (type == CategoryType.CATALOG) type = CategoryType.OTHER;
		else type=CategoryType.CATALOG;

		List<Category> listAllCategory2 = categoryService.listAll(s.getLang(), s.getState(), type, err);
		if (listAllCategory2 != null && listAllCategory2.size() > 0) {
			Category root2 = categoryService.getRoot(s.getLang(), s.getState(), type, err);
			List<Category> lc1 = getOptionCategory(listAllCategory, root);
			List<Category> lc2 = getOptionCategory(listAllCategory2, root2);
			lc1.addAll(lc2);
			mav.addObject("listAllCategory", lc1);
		} else {
			mav.addObject("listAllCategory", getOptionCategory(listAllCategory, root));
		}

		// SeriesLink
		// 紐づけのあるリンク一覧
		List<SeriesLink> sLink = s.getLink();

		// 紐づけを追加するためのリンクマスター
		List<SeriesLinkMaster> listLinkMaster = sLinkMasterService.findByLangAll(s.getLang(), null, obj);
		mav.addObject("listLinkMaster", listLinkMaster);

		// myformにリンクを設定
		String[] linkList = new String[listLinkMaster.size()];
		int cnt = 0;
		for(SeriesLinkMaster m:listLinkMaster) {
			boolean find = false;
			for(SeriesLink sl:sLink) {
				if (sl.getLinkMaster().getId().equals(m.getId())) {
					find = true;
					linkList[cnt] = sl.getUrl();
				}
			}
			if (find == false) linkList[cnt] = "";
			cnt++;
		}
		myform.setLinkList(linkList);

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
			@ModelAttribute("seriesForm") SeriesForm myform,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/series/edit");
		s_state.setView(ViewState.SERIES.toString());
		String categoryId = s_state.getKeyword();

		// Get Category
		ErrorObject err = new ErrorObject();
		Category c = null;
		if (StringUtils.isEmpty(categoryId) == false) {
			c = categoryService.get(categoryId, err);
			if (c != null)
			{
				mav.addObject("category", c);
				mav.addObject("categoryId", c.getId());
				setBreadcrumb(mav, c);
			}
		}

		// Get by id
		ErrorObject obj = new ErrorObject();
		Series s = service.getWithCategory(id, null, obj);
		String seriesId = s.getId();
		s.setId(null);
		s.setOldId(null);

		// Map(Copy) Category -> Form
		modelMapper.map(s, myform);
		myform.setSearchword(String.join(",", s.getKeyword()));

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("series", s);

		// CategorySeries
		// 紐づけのあるカテゴリ一覧
		List<Category> listCategory = categoryService.listCategoryFromSeries(seriesId, null, err);
//		List<CategorySeries> listCategory = s.getCategorySeries(); // カテゴリ名の表示に必要なので、Categoryが必要。
		if (listCategory != null) {
			String[] caList = new String[listCategory.size()];
			int cnt = 0;
			for(Category ca : listCategory) {
				caList[cnt] = ca.getId();
				cnt++;
			}
			myform.setCategoryList(caList);
		}
		mav.addObject("listCategory", listCategory);

		// 紐づけを追加するための全カテゴリ一覧
		CategoryType type = CategoryType.CATALOG;
		if (c != null) type = c.getType();
		List<Category> listAllCategory = categoryService.listAll(s.getLang(), s.getState(), type, err);
		Category root = categoryService.getRoot(s.getLang(), s.getState(), type, err);


		// OTHERも選べるように下部へ追加
		if (type == CategoryType.CATALOG) type = CategoryType.OTHER;
		else type=CategoryType.CATALOG;

		List<Category> listAllCategory2 = categoryService.listAll(s.getLang(), s.getState(), type, err);
		if (listAllCategory2 != null && listAllCategory2.size() > 0) {
			Category root2 = categoryService.getRoot(s.getLang(), s.getState(), type, err);
			List<Category> lc = getOptionCategory(listAllCategory, root);
			lc.addAll(getOptionCategory(listAllCategory2, root2));
			mav.addObject("listAllCategory", lc);
		} else {
			mav.addObject("listAllCategory", getOptionCategory(listAllCategory, root));
		}

		// SeriesLink
		// 紐づけのあるリンク一覧
		List<SeriesLink> sLink = s.getLink();

		// 紐づけを追加するためのリンクマスター
		List<SeriesLinkMaster> listLinkMaster = sLinkMasterService.findByLangAll(s.getLang(), null, obj);
		mav.addObject("listLinkMaster", listLinkMaster);

		// myformにリンクを設定
		String[] linkList = new String[listLinkMaster.size()];
		int cnt = 0;
		for(SeriesLinkMaster m:listLinkMaster) {
			boolean find = false;
			for(SeriesLink sl:sLink) {
				if (sl.getLinkMaster().getId().equals(m.getId())) {
					find = true;
					linkList[cnt] = sl.getUrl();
				}
			}
			if (find == false) linkList[cnt] = "";
			cnt++;
		}
		myform.setLinkList(linkList);

//		setEditRefParam(mav, s); // stateRef langRef アーカイブなどはコピーしない。

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
			@Validated @ModelAttribute("seriesForm") SeriesForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/series/edit");

		log.debug(form.toString());

		User s_user = (User)session.getAttribute("SessionUser");

		// 1) - 7)までが基本的な更新処理の流れ

		Series s = new Series();
		boolean isNew = false;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			s = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		} else {
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

			// 準備中フラグ
			if (form.isPre()) s.setOldId("-1");
			else s.setOldId("");

			s.setKeyword(form.getSearchword().split(","));
			s.setUser(s_user);

			// 6) 保存
			ErrorObject obj = service.save(s);

			// CategoryService
			if (obj.isError() == false && StringUtils.isEmpty(categoryId) == false) {
				Category c = categoryService.get(categoryId, obj);
				csService.save(c, s, obj);
			}

			// CategoryService 数が違ったらdel
			boolean isDiff = false; // breadClumbを更新
			List<Category> listCategory = categoryService.listCategoryFromSeriesWithCheck(s.getId(), obj);
			if (listCategory != null && form.getCategoryList() != null && listCategory.size() != form.getCategoryList().length)
			{
				if (listCategory.size() != form.getCategoryList().length) isDiff = true;

				String[] categoryList = form.getCategoryList();
				for(Category ca : listCategory) {
					boolean isFind = false;
					for(String tmp : categoryList) {
						if (ca.getId().equals(tmp)) {
							isFind = true;
							break;
						}
					}
					if (isFind == false) {
						csService.delete(ca.getId(), s.getId());
					}
				}
			}

			// CategoryService add
			String add = form.getAddCategory();
			if (add != null && StringUtils.isEmpty(add) == false) {
				Category ca = categoryService.get(add, obj);
				if (ca != null) {
					csService.save(ca, s, obj);
					isDiff = true;
				}
			}
			// breadClumbを更新
			// Otherは対象外 2026/2/24
			String[] arr = s.getBreadcrumb().split("●");
			int cnt = arr.length -1;
			if ((isDiff || (form.getCategoryList() != null && cnt != form.getCategoryList().length)) && form.getState().equals(ModelState.TEST)) {
				listCategory = categoryService.listCategoryFromSeriesWithCheck(s.getId(), obj);
				if (listCategory != null) {
					String bread = "";
//					Category ca = listCategory.get(0);  // Otherは対象外 2026/2/24
					Category testRoot = categoryService.getRoot(s.getLang(), ModelState.TEST, /*ca.getType()*/ CategoryType.CATALOG, obj); // Otherは対象外 2026/2/24
					for(Category c : listCategory) {
						if (c.getType() != null && c.getType().equals(CategoryType.OTHER)) continue; // Otherは対象外 2026/2/24
						if (c.getParentId().equals(testRoot.getId()) == false) {
							Category pC = categoryService.get(c.getParentId(), obj);
							if (pC != null) {
								bread += "●%"+pC.getSlug()+"%"+pC.getName()+"#%"+c.getSlug()+"%"+c.getName()+"#";
							}
						}
					}
					s.setBreadcrumb(bread);
					obj = service.save(s);
				}
			}

			// SeriesLink 入力のある項目のみ保存。
			List<SeriesLinkMaster> listLinkMaster = sLinkMasterService.findByLangAll(s.getLang(), null, obj);

			String[] linkArr = form.getLinkList();
			obj = service.linkUpsert(s.getId(), listLinkMaster, linkArr, s.getState(), s_user);

			// PRODの更新ならHtmlも更新
			if (s.getState().equals(ModelState.PROD) && s.isActive()) {
				outputHtml(s.getId()); // HTML書き出し
			}

			// 7) フォームを更新(再編集用)
			s = service.getWithCategory(s.getId(), null, obj); // Linkも一緒に取り直し
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
		Category c = null;
		if (StringUtils.isEmpty(categoryId) == false) {
			ErrorObject err = new ErrorObject();
			c = categoryService.get(categoryId, err);
			if (c != null)
			{
				mav.addObject("category", c);
				setBreadcrumb(mav, c);
				mav.addObject("categoryId", categoryId);

			}
		}
		ErrorObject err = new ErrorObject();
		// 紐づけのあるカテゴリ一覧
		{
			List<Category> listCategory = categoryService.listCategoryFromSeries(s.getId(), null, err);
			if (listCategory != null) {
				String[] caList = new String[listCategory.size()];
				int cnt = 0;
				for(Category ca : listCategory) {
					caList[cnt] = ca.getId();
					cnt++;
				}
				form.setCategoryList(caList);
			}
			mav.addObject("listCategory", listCategory);

			// 紐づけを追加するための全カテゴリ一覧
			CategoryType type = CategoryType.CATALOG;
			if (c != null) type = c.getType();
			List<Category> listAllCategory = categoryService.listAll(s.getLang(), s.getState(), type, err);
			Category root = categoryService.getRoot(s.getLang(), s.getState(), type, err);


			// OTHERも選べるように下部へ追加
			if (type == CategoryType.CATALOG) type = CategoryType.OTHER;
			else type=CategoryType.CATALOG;

			List<Category> listAllCategory2 = categoryService.listAll(s.getLang(), s.getState(), type, err);
			if (listAllCategory2 != null && listAllCategory2.size() > 0) {
				Category root2 = categoryService.getRoot(s.getLang(), s.getState(), type, err);
				List<Category> lc = getOptionCategory(listAllCategory, root);
				lc.addAll(getOptionCategory(listAllCategory2, root2));
				mav.addObject("listAllCategory", lc);
			} else {
				mav.addObject("listAllCategory", getOptionCategory(listAllCategory, root));
			}

			// SeriesLink
			// 紐づけを追加するためのリンクマスター
			List<SeriesLinkMaster> listLinkMaster = sLinkMasterService.findByLangAll(s.getLang(), null, err);
			mav.addObject("listLinkMaster", listLinkMaster);

			// 紐づけのあるリンク一覧
			List<SeriesLink> sLink = s.getLink();
			if (sLink != null) { // 新規でエラーの場合は登録されていないので、null
				// myformにリンクを設定
				String[] linkList = new String[listLinkMaster.size()];
				int cnt = 0;
				for(SeriesLinkMaster m:listLinkMaster) {
					boolean find = false;
					for(SeriesLink sl:sLink) {
						if (sl.getLinkMaster().getId().equals(m.getId())) {
							find = true;
							linkList[cnt] = sl.getUrl();
						}
					}
					if (find == false) linkList[cnt] = "";
					cnt++;
				}
				form.setLinkList(linkList);
			}
		}



		mav.addObject(form);

		return mav;
	}

	// ========== private ==========

	// 対象となるHTML
	// /{lang}/{slug}/{slug}/{seid}/index.html
	// /{lang}/{slug}/{slug}/{seid}/s.html
	// /{lang}/series/{seid}/index.html
	// /{lang}/series/{seid}/guide.html
	// /{lang}/series/{seid}/s.html
	// 2026/3/30 OUTPUT時は必ずPRODのテンプレートを取得
	private ErrorObject outputHtml(String id) {
		ErrorObject err = new ErrorObject();

		Series s = service.get(id, err);
		if (s.getState().equals(ModelState.TEST)) {
			List<Series> sList = service.getStateRefId(s, ModelState.PROD, err);
			if (sList != null && sList.size() > 0) {
				s = sList.get(0);
			} else {
				return err;
			}
		}
		Locale loc = getLocale(s.getLang());
		s.setLink(service.getLink(s.getId(), err));

		List<Category> listC = categoryService.listCategoryFromSeries(s.getId(), true, err);

		// CategoryControllerと同じ処理
		for(Category c : listC) {
			Category c1 = null;
			Category c2 = null;
			
			if (c.getType().equals(CategoryType.OTHER)) continue; // その他は静的Html無し。対象外

			Template t = templateService.getTemplateFromBean(c.getLang(), ModelState.PROD); // これは言語のTemplate。PROD active取得 2026/3/30
			TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(c.getLang(), ModelState.PROD, c.getId());
			if (tc == null) {
				c1 = categoryService.get(c.getParentId(), err);
				tc = templateCategoryService.findByCategoryIdFromBean(c1.getLang(), ModelState.PROD, c1.getId());
				c2 = c;
			} else {
				c1 = c;
			}
			Category withSeries = categoryService.getWithSeries(c.getId(), true, err);
			//html.Init(getLocale(c1.getLang()), messagesource);
			if (tc.is2026()) {
				html.outputTemplateCategoryToHtml2026(t, tc, c1, c2, withSeries.getSeriesList(), categoryService, service); // 大カテゴリ静的Html吐き出し
			} else {
				html.outputTemplateCategoryToHtml(t, tc, c1, c2, withSeries.getSeriesList(), categoryService, service); // 大カテゴリ静的Html吐き出し
			}
			if (c2 == null) {
				Category withChild = categoryService.getWithChildren(c.getId(), true, err);
				for(Category ch : withChild.getChildren()) {
					withSeries = categoryService.getWithSeries(ch.getId(), true, err);
					if (tc.is2026()) {
						html.outputTemplateCategoryToHtml2026(t, tc, c, ch, withSeries.getSeriesList(), categoryService, service); // 小カテゴリ静的Html吐き出し
					} else {
						html.outputTemplateCategoryToHtml(t, tc, c, ch, withSeries.getSeriesList(), categoryService, service); // 小カテゴリ静的Html吐き出し
					}
				}
			}
		}
/*		String url = "";
		String path = "";
		Category c2 = null;
		if (listC.size() > 0) c2 = listC.get(0);
		Category c = categoryService.get(c2.getParentId(), err);
		if (c == null || c.getParentId() == null || c.getParentId().isEmpty()) {
			c = c2;
			c2 = null;
			url = AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug();
			path = c.getLang()+"/"+c.getSlug()+"/" + s.getModelNumber() + "/s.html";
			path = c.getLang()+"/series/" + s.getModelNumber() + "/s.html";
		} else {
			url = AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug();
			path = c.getLang()+"/series/" + s.getModelNumber() + "/s.html";
		}
		String html = sHtml.get(s, 1, c, c2, url, s.getLang(), false, false);
		outputHtml(path, html);*/
		return err;
	}
	public boolean outputHtml(String path, String html)
	{
		boolean ret = false;
		BufferedWriter bw = null;
		try{
			if (path.indexOf("/") == 0) path = htmlPath + path.substring(1);
			else path = htmlPath + path;

            File file = new File(path);
            if (file.exists() == false) {
            	String tmp = path.substring(0, path.lastIndexOf("/"));
            	File file2 = new File(tmp);
            	file2.mkdirs();
            }
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(html);
            bw.flush();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(bw != null) {
                    bw.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
		return ret;
	}
	/**
	 * 編集用の国リストとPRODorTEST、ARCHIVEを用意
	 * @param mav
	 * @param category 新規はLangRefId
	 * @return
	 */
	private ErrorObject setEditRefParam(ModelAndView mav, Series se) {
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
		List<Series> langRefList = service.listLangRef(langBaseId, err);
		Series langBaseCategory = service.get(langBaseId, err);
		langRefList.add(0, langBaseCategory);
		List<String> langs = new ArrayList<String>();
		for(Series s : langRefList) {
			if (s == null) continue;
			langs.add(s.getLang());
		}

		// Get by TEST or PROD
		Series diffSeries = null;
		if (se.getState().equals(ModelState.TEST)) {
			List<Series> list = service.getStateRefId(se, ModelState.PROD, err);
			if (list != null && list.size() > 0) diffSeries = list.get(0);
		} else {
			List<Series> list = service.getStateRefId(se, ModelState.TEST, err);
			if (list != null && list.size() > 0) diffSeries = list.get(0);
		}
		// Get Archive
		List<Series> list = service.getStateRefId(se, ModelState.ARCHIVE, err);

		mav.addObject("langRefList", langs);
		mav.addObject("diffSeries", diffSeries);
		mav.addObject("langBaseId", langBaseId);
		mav.addObject("listArchive", list);

		return err;
	}
	private void setBreadcrumb(ModelAndView mav, Category c) {
		List<Category> breadcrumb = null;
		ErrorObject err = new ErrorObject();

		if (c != null && !StringUtils.isEmpty(c.getParentId())) {
			breadcrumb = categoryService.getParents(c.getId(), null, err);
		}
		//Add to View
		if (breadcrumb != null) {
			mav.addObject("breadcrumb", breadcrumb);
		}

		//debug
		if (breadcrumb == null) {
			log.debug(err.getMessage());
		} else {
			for (Category _c : breadcrumb) {
				if (_c != null) {
					log.debug(_c.getName());
				}
			}
		}
	}
	public static Path getApplicationPath(Class<?> cls) throws URISyntaxException {
		ProtectionDomain pd = cls.getProtectionDomain();
		CodeSource cs = pd.getCodeSource();
		URL location = cs.getLocation();
		URI uri = location.toURI();
		Path path = Paths.get(uri);
		return path;
	}
	private List<Category> getOptionCategory(List<Category> base, Category root) {
		List<Category> list = new LinkedList<Category>();
		for (Category c : base) {
			if (c == null) continue;
			Category cate = new Category();
			cate.setId(c.getId());
			if (root.getId().contentEquals(c.getParentId())) {
				cate.setName(c.getName());
			} else {
				cate.setName("　" + c.getName());
			}
			list.add(cate);
		}
		return list;
	}
	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh-tw") > -1) loc = Locale.TAIWAN;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}

}
