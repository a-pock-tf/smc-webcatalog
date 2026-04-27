package com.smc.webcatalog.web;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.LangForm;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.LangFormValidator;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/lang")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class LangController extends BaseController {
	@Autowired
	LangService service;

	@Autowired
	LangFormValidator validator;

	@Autowired
	TemplateService templateService;

	@Autowired
	TemplateCategoryService templateCategoryService;

	@Autowired
	CategoryService categoryService;

	@Autowired
	SeriesService seriesService;

	@Autowired
    SeriesFaqRepository faqRepo;
	
	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml html;

    @Autowired
	Environment env;

	@Autowired
	HttpSession session;

	@Autowired
    HttpServletRequest req;

	/**
	 * 全テンプレート
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "/allUpdate"})
	public ModelAndView template(
			ModelAndView mav,
			HttpServletRequest request,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/lang/list");
		s_state.setView(ViewState.LANG.toString());

		String url = request.getRequestURL().toString();
		boolean isTest = (url.indexOf("test.smcworld.com") > 0 || url.indexOf("ap1.smcworld.com") > 0 || url.indexOf("ap2.smcworld.com") > 0 || url.indexOf("localhost") > 0);

		//リスト取得
		ErrorObject err = new ErrorObject();
		List<Lang> list = service.listAll(null, err);
		for(Lang la : list) {
			ModelState st = ModelState.TEST;
			if (s_state.getState().equals("PROD")) st = ModelState.PROD;
			Template temp = templateService.getLangAndModelState(la.getLang(), st, null, err); // previewなのでTEST固定
			if (temp != null && temp.getHeartCoreId() != null && temp.getHeartCoreId().isEmpty() == false) {
				templateService.setHeartCore(temp);
				templateService.save(temp);
				//html.Init(getLocale(temp.getLang()), messagesource);
				html.outputHtml( la.getLang() + "/index.html", temp.getHeader() + temp.getContents() + temp.getFooter() ); // 静的Html吐き出し
			}
		}

		log.debug("list=" + list.toString());

		//Add Form to View
		mav.addObject("isTest", isTest);
		mav.addObject("list", list);

		return mav;
	}
	/**
	 * ローカル用出力
	 * @param myform
	 * @param mav
	 * @return
	 * @see テンプレートのbaseタグが無いこと！
	 * @note 2026/03/30 未TEST!!
	 */
	@GetMapping({ "/jpLocal", "/enLocal"})
	public ModelAndView enLocal(
			ModelAndView mav,
			HttpServletRequest request,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		ErrorObject err = new ErrorObject();
		// Set view
		mav.setViewName("/login/admin/lang/list");

		String url = request.getRequestURL().toString();
		boolean isTest = (url.indexOf("test.smcworld.com") > 0 || url.indexOf("ap1.smcworld.com") > 0 || url.indexOf("ap2.smcworld.com") > 0 || url.indexOf("localhost") > 0);

		String lang = "en-jp";
		String uri = request.getRequestURI();
		if (uri.indexOf("jpLocal") > -1) {
			lang = "ja-jp";
		}

		// 新規、またはReplaceのチェックがあれば取得
		html.InitOffLine(env, lang, messagesource, faqRepo); // resources/static/offline_jp/ または /offline_en/ に出力
		Template temp = templateService.getLangAndModelState(lang, ModelState.PROD, true, err);
		// Topページ
		String strOffline = html.offlineTemplate(temp, 2); // /webcatalog/en-jp/index.html
		html.outputHtml( "/webcatalog/"+lang+"/index.html", strOffline ); // 静的Html吐き出し

		// 検索結果表示用
		List<Series> sList = new ArrayList<Series>();
		Category searchCate = null;
		Category searchCate2 = null;
		TemplateCategory searchTempCate = null;

		// CategoryTopページ
		Category root = categoryService.getRoot(lang, ModelState.PROD, CategoryType.CATALOG, err);
		int cateCnt = 0;
		Category cate = categoryService.getWithChildren(root.getId(), true, err);
		if (cate != null) {
			List<Category> clist = cate.getChildren();
			for(Category c : clist) {
//				  if (c.getSlug().equals("electric-actuators-cylinders")) {
//				  if (c.getSlug().equals("temperature-control-equipment")) {
				if (searchCate == null) searchCate = c; // 最初の大カテゴリ
				Category withSeries = categoryService.getWithSeries(c.getId(), true, err);
				TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(c.getLang(), c.getState(), c.getId());
				if (searchTempCate == null) searchTempCate = tc;
				html.offlineTemplateCategory(temp, tc, c, null, withSeries.getSeriesList(),
						categoryService, seriesService, 3);

				Category cate2 = categoryService.getWithChildren(c.getId(), true, err);
				if (cate2 != null) {
					List<Category> c2List = cate2.getChildren();
					if (c2List != null && c2List.size() > 0) {
						for(Category c2: c2List) {
//							  if (c2.getSlug().equals("reduced-wiring-fieldbus-system")) {
							if (searchCate2 == null) searchCate2 = c2; // 最初の小カテゴリ
							Category withSeries2 = categoryService.getWithSeries(c2.getId(), true, err);
							html.offlineTemplateCategory(temp, tc, c, c2, withSeries2.getSeriesList(),
									categoryService, seriesService, 4);
//							  } // if (slug)
						}
					}
				}
				cateCnt++;
//				if (cateCnt > 0) break; // 数字のカテゴリ数まで。
			} // end for(Category c
		}
		// シリーズの書き出し
		{
			Category cateR = categoryService.getWithChildren(root.getId(), true, err);
			List<Category> clist = cateR.getChildren();
			for(Category c : clist) {
				Category withSeries = categoryService.getWithSeries(c.getId(), true, err);
				sList.addAll(withSeries.getSeriesList());
				Category cate2 = categoryService.getWithChildren(c.getId(), true, err);
				List<Category> c2List = cate2.getChildren();
				for(Category c2: c2List) {
					Category withSeries2 = categoryService.getWithSeries(c2.getId(), true, err);
					sList.addAll(withSeries2.getSeriesList());
				}
			}
			// 検索ページ作成
			html.offlineTemplateCategoryToResult(temp, searchTempCate, searchCate, searchCate2, sList);
		}


		//Add Form to View
		List<Lang> list = service.listAll(null, err);
		mav.addObject("list", list);
		mav.addObject("isTest", isTest);
		mav.addObject("is_success", "ローカル出力終了");

		return mav;
	}

	/**
	 * 管理系 > 言語 > 一覧
	 * @param myform
	 * @param mav
	 * @return
	 */
	@GetMapping({ "", "/"})
	public ModelAndView edit(
			ModelAndView mav,
			HttpServletRequest request,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/lang/list");
		s_state.setView(ViewState.LANG.toString());

		String url = request.getRequestURL().toString();
		boolean isTest = (url.indexOf("test.smcworld.com") > 0 || url.indexOf("ap1.smcworld.com") > 0 || url.indexOf("ap2.smcworld.com") > 0 || url.indexOf("localhost") > 0);

		//リスト取得
		ErrorObject err = new ErrorObject();
		List<Lang> list = service.listAll(null, err);

		log.debug("list=" + list.toString());

		//Add Form to View
		mav.addObject("list", list);
		mav.addObject("isTest", isTest);

		return mav;
	}

	/**
	 * 管理系 > 言語 > 新規
	 * @param mav
	 * @param myform
	 * @return
	 */
	@RequestMapping(value = "/new")
	public ModelAndView create(
			ModelAndView mav,
			@ModelAttribute("langForm") LangForm myform,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/lang/edit");

		//Set FormObject to view
		mav.addObject(myform);

		return mav;
	}

	/**
	 * 管理系 > 言語 > 編集
	 * 編集のために、DBからデータを読んでフォームにセット
	 * @param myform
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(
			ModelAndView mav,
			@ModelAttribute("langForm") LangForm myform,
			@PathVariable(name = "id", required = false) String id) {

		// Set view
		mav.setViewName("/login/admin/lang/edit");

		// Get by id
		ErrorObject obj = new ErrorObject();
		Lang lang = service.get(id, obj);

		// Map(Copy) Lang -> Form
		modelMapper.map(lang, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("lang", lang);

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
			@Validated @ModelAttribute("langForm") LangForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/lang/edit");

		log.debug(form.toString());

		User s_user = (User)session.getAttribute("SessionUser");

		// 1) - 7)までが基本的な更新処理の流れ

		Lang lang = null;
		boolean isNew = false;

		// 1) idがあれば(=編集) dbから取得
		if (!StringUtils.isEmpty(form.getId())) {
			ErrorObject obj = new ErrorObject();
			lang = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		}else {
			lang = new Lang("");
			validator.validateNew(result, form);
			isNew = true;
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// 5) フォームからCategoryにコピー
			modelMapper.map(form, lang);
			log.debug("Lang(FORM)=" + lang.toString());
			lang.setUser(s_user);

			// 6) 保存
			ErrorObject obj = service.save(lang);

			// 7) フォームを更新(再編集用)
			modelMapper.map(lang, form);

			mav.addObject("is_success", !obj.isError());

			// 新規ならrootカテゴリを作成
			if (isNew) categoryService.createRootCategory(form.getName());
			else if (form.isVersion() == false){
				// 無ければ作成
				Category c = categoryService.getLang(form.getName(), ModelState.TEST, CategoryType.CATALOG, null, obj);
				if (c != null && ( c.getParentId() == null || c.getParentId().isEmpty() ))
				{
					categoryService.createRootCategory(form.getName());
				}
			}

			// 成功でLangのContextを更新
			ErrorObject err = new ErrorObject();
			req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_PREFIX,  service.listAll(true, err));
			req.getServletContext().setAttribute(Lang.APPLICATION_CONTEXT_ALL_PREFIX,  service.listAll(null, err));

		} else {

			// 戻りのページ この場合はedit.htmlなので何もしない
		}

		mav.addObject(form);
		mav.addObject("lang", lang);

		return mav;
	}

	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}
}
