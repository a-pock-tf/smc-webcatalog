package com.smc.webcatalog.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
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

import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryForm;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.model.ViewState;
import com.smc.webcatalog.service.CategoryFormValidator;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.NarrowDownService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/login/admin/category")
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class CategoryController extends BaseController {

	@Autowired
	CategoryService service;

	@Autowired
	CategoryFormValidator validator;

	@Autowired
	SeriesService seriesService;

	@Autowired
	TemplateService templateService;

	@Autowired
	TemplateCategoryService templateCategoryService;
	
	@Autowired
	NarrowDownService narrowDownService;

	@Autowired
    MessageSource messagesource;

	@Autowired
	HttpSession session;

	@Autowired
    HttpServletRequest req;

    @Autowired
	Environment env;

    @Autowired
	LibHtml html;

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
	 * 全重み
	 */
	@GetMapping({ "/allOrderUpdate" })
	public ModelAndView allSeriesOrder(
										ModelAndView mav,
										@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
										HttpServletRequest request) {

		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(10*60);

		ErrorObject err = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");

		Category root = service.getRoot(s_state.getLang(), ModelState.TEST, s_state.getCategoryType(), err);
		List<Category> list = service.listAll(root.getId(), true, err);
		if (list != null && list.size() > 0) {
			// 一旦、9999に設定して、1から順に振り直し
			seriesService.resetOrder(s_state.getLang(), ModelState.TEST, s_user);
			for(Category c : list) {
				Category wc = service.getWithChildren(c.getId(), null, err);
				List<Category> ch = wc.getChildren();
				seriesService.updateOrder(ch, s_state.getLang(), ModelState.TEST, s_user);
			}
		}
		mav.setViewName("/login/admin/category/list");
		s_state.setLayer1Category(true); // 大カテゴリに戻す

		// リスト表示設定
		setDispListParam(mav, root.getId(), s_state.getLang(), s_state.getViewState(), s_state.getCategoryType());

		return mav;
	}
	/**
	 * 全テンプレート
	 */
	@GetMapping({ "/allCategoryTemplateUpdate" })
	public ModelAndView allCategoryTemplate(
										ModelAndView mav,
										@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
										HttpServletRequest request) {

		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(10*60);

		ErrorObject err = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");

		Category root = service.getRoot(s_state.getLang(), ModelState.TEST, s_state.getCategoryType(), err);
		List<Category> list = service.listAll(root.getId(), true, err);
		if (list != null && list.size() > 0) {
			for(Category c : list) {
				TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(c.getLang(), c.getState(), c.getId());
				if (tc != null && tc.getHeartCoreID() != null && tc.getHeartCoreID().isEmpty() == false) {
					templateCategoryService.setHeartCore(tc);
					tc.setUser(s_user);
					templateCategoryService.save(tc);
				}
			}
		}
		mav.setViewName("/login/admin/category/list");
		s_state.setLayer1Category(true); // 大カテゴリに戻す

		// リスト表示設定
		setDispListParam(mav, root.getId(), s_state.getLang(), s_state.getViewState(), s_state.getCategoryType());

		return mav;
	}
	/**
	 * 全アップ
	 */
	@GetMapping({ "/allUpdate" })
	public ModelAndView allUpdate(
										ModelAndView mav,
										@RequestParam(name = "page", required = false, defaultValue = "0") String page,
										@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state,
										HttpServletRequest request) {

		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(10*60);

		ErrorObject err = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");
		int p = 0;
		if (page.equals("0") == false) {
			p = Integer.parseInt(page);
		}

		Category root = service.getRoot(s_state.getLang(), ModelState.TEST, s_state.getCategoryType(), err);
		List<Category> list = service.listAll(root.getId(), true, err);
		if (list != null && list.size() > 0) {
			//html.Init(getLocale(s_state.getLang()), messagesource);
			int cnt = 0;
			for(Category c : list) {
				if (c.getParentId().equals(root.getId())) { // 大カテゴリのみ処理対象
					cnt++;
					if (p == 1) {
						if (cnt > 5) break;
					} else if (p == 2) {
						if (cnt <= 5) continue;
						if (cnt > 15) break;
					} else if (p >= 3) {
						if (cnt <= 15) continue;
					}
					try {
						if (c.isActive() == false) {
							// HTML出力削除
							deleteHtml(html, c);
							continue;
						}
						err = service.changeStateToProdAll(c.getId(), s_user);
						if(err.isError()) {
							mav.addObject("error", err.getMessage() );
						} else {
							Category prod = null;
							if (c.getState().equals(ModelState.PROD) == false) {
								prod = service.getStateRefId(c, ModelState.PROD, err);
							} else {
								prod = c;
							}
							err = outputHtml(html, prod); // HTML出力
						}
					} catch (Exception e) {
						log.error("Error! message=" + e.getMessage());
					}
				}
			}
		}
		mav.setViewName("/login/admin/category/list");
		s_state.setLayer1Category(true); // 大カテゴリに戻す

		// リスト表示設定
		setDispListParam(mav, root.getId(), s_state.getLang(), s_state.getViewState(), s_state.getCategoryType());

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
										@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		ErrorObject err = new ErrorObject();
		User s_user = (User)session.getAttribute("SessionUser");

		Category c = service.get(id, err);
		String pId = c.getParentId();
		String success = "";

		if (state != null && state.equals("PROD")) { // カテゴリにArchiveは無し
			err = service.changeStateToProdAll(id, s_user);
			if(err.isError()) {
				mav.addObject("error", err.getMessage() );
			} else {
				//html.Init(getLocale(c.getLang()), messagesource);

				Category prod = null;
				if (c.getState().equals(ModelState.TEST)) {
					prod = service.getStateRefId(c, ModelState.PROD, err);
				} else {
					prod = c;
				}
				if (prod.getType().equals(CategoryType.CATALOG)) {
					err = outputHtml(html, prod); // HTML書き出し
					success+= "success. name="+prod.getName()+" count=";
				} else if (err.isError() == false){
					// OTHERなら書き出し無し
					success+= "success. name="+prod.getName()+" count=";
				}

				if(err.isError()) {
					mav.addObject("error", err.getMessage() );
				} else {
					mav.addObject("message" , success + err.getCount());
				}
			}
		}
		mav.setViewName("/login/admin/category/list");

		// リスト表示設定
		setDispListParam(mav, pId, s_state.getLang(), s_state.getViewState(), s_state.getCategoryType());

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > ソート
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 */
	@PostMapping({ "/sort", "/other/sort" })//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView sort(
			ModelAndView mav,
			@RequestParam(name = "ids", required = false, defaultValue = "") String ids,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		String ret = "failed";

		String typeUrl = "";
		if (req.getRequestURL().indexOf("/other") > 0) {
			typeUrl = "/other";
		}

		String[] arr = ids.split("-");
		String parentId = null;
		List<String> listId = new ArrayList<String>();

		ErrorObject err = new ErrorObject();
		for(String tmp : arr) {
			if (tmp != null && tmp.isEmpty() == false) {
				if (tmp.indexOf("[parent]") > -1) {
					parentId = tmp.replace("[parent]", "");
					if (parentId == null || parentId.isEmpty()) {
						// root の時はここにくる
						Category c = service.get(arr[1], err);
						if (c != null) parentId = c.getParentId();
					}
				}
				else {
					listId.add(tmp.trim());
				}
			}
		}
		// Set view
		mav.setViewName("redirect:/login/admin/category" + typeUrl + "/" + parentId + "/?error=error.");

		if (listId.size() > 0)
		{
			err = service.sort(parentId, listId);
			if (!err.isError()) {
				ret = "success.count=" + err.getCount();
				mav.setViewName("redirect:/login/admin/category" + typeUrl + "/" + parentId + "/?message=" + ret);
			}
			else {
				mav.addObject("error", err.getMessage());
			}
		}

		return mav;
	}

	/**
	 * 管理系 > カテゴリ > 検索
	 * @param mav
	 * @param state ?state= でTEST,PRODを切替可能
	 * @return
	 * @note {id}が無い場合は全部だしてしまうと多いので、カテゴリに属さないシリーズの一覧
	 */
	@GetMapping( {"/search", "/other/search"} )//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView search(ModelAndView mav,
			@RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
			@RequestParam(name = "type", required = false, defaultValue = "") String type,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		//Set view
		if (type != null && type.equalsIgnoreCase("other") ) {
			s_state.setView(ViewState.OTHER.toString());
			s_state.setType(CategoryType.OTHER.toString());
			s_state.setBackUrl("/login/admin/category/other/search");
		} else if (req.getRequestURL().indexOf("/other") > 0) {
			s_state.setView(ViewState.OTHER.toString());
			s_state.setType(CategoryType.OTHER.toString());
			s_state.setBackUrl("/login/admin/category/other");
		} else {
			s_state.setView(ViewState.CATEGORY.toString());
			s_state.setType(CategoryType.CATALOG.toString());
			s_state.setBackUrl("/login/admin/category/search");
		}
		mav.setViewName("/login/admin/category/search_result");
		s_state.setKeyword( keyword);
		s_state.setLayer1Category(true); // 大カテゴリに戻す

		ErrorObject err = new ErrorObject();
		List<Category> list = service.search(
				keyword, s_state.getLang(), s_state.getViewState(), s_state.getCategoryType(), null, err);

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
	@GetMapping({ "", "/", "/{id}", "/other", "/other/{id}", "/other/{id}/" })//一番上のRequestMappingで起点パスは指定してある
	public ModelAndView list(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@RequestParam(name = "type", required = false, defaultValue = "") String type,
			@RequestParam(name = "lang", required = false, defaultValue = "") String lang,
			@RequestParam(name = "state", required = false, defaultValue = "") String state,
			@RequestParam(name = "locale", required = false, defaultValue = "") String locale,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		if (type != null && type.equalsIgnoreCase("other") ) {
			s_state.setView(ViewState.OTHER.toString());
			s_state.setType(CategoryType.OTHER.toString());
			s_state.setBackUrl("/login/admin/category/other");
		} else if (req.getRequestURL().indexOf("/other") > 0) {
			s_state.setView(ViewState.OTHER.toString());
			s_state.setType(CategoryType.OTHER.toString());
			s_state.setBackUrl("/login/admin/category/other");
		} else {
			s_state.setView(ViewState.CATEGORY.toString());
			s_state.setType(CategoryType.CATALOG.toString());
			s_state.setBackUrl("/login/admin/category/");
		}
		mav.setViewName("/login/admin/category/list");

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
		String _lang =s_state.getLang();
		// 大カテゴリか判定
		if (id == null) {
			s_state.setLayer1Category(true); // 大カテゴリに戻す
		} else {
			ErrorObject err = new ErrorObject();
			Category c = service.get(id, err);
			if (c != null) {
				Category root = service.getRoot(_lang, _state, _type, err);
				if (c.getId().equals(root.getId())) {
					s_state.setLayer1Category(true); // 大カテゴリに戻す
				} else {
					s_state.setLayer1Category(false); 
				}
			}
		}

		log.debug("ScreenStatusHolder=" + s_state);

		// リスト表示設定
		setDispListParam(mav, id, _lang, _state, _type);

		return mav;
	}

	@GetMapping({ "/del/{id}" })
	public ModelAndView delete(
			ModelAndView mav,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		//Set view
		mav.setViewName("/login/admin/category/list");
		s_state.setView(ViewState.CATEGORY.toString());

		ErrorObject err = new ErrorObject();
		String parentId = null;
		Category c = service.get(id, err);
		if (c != null) {
			String typeUrl = "";
			if (c.getType() != null && c.getType().equals(CategoryType.OTHER) ) {
				s_state.setView(ViewState.OTHER.toString());
				s_state.setType(CategoryType.OTHER.toString());
				typeUrl = "/other";
			} else {
				s_state.setView(ViewState.CATEGORY.toString());
				s_state.setType(CategoryType.CATALOG.toString());
			}

			parentId = c.getParentId();

			log.debug("ScreenStatusHolder=" + s_state);

			err = service.checkDelete(id);
			if (err.isError()) {
				mav.addObject("error", err.getMessage());
			} else {
				// PRODなら公開HTMLも削除
				if (c.getState().equals(ModelState.PROD)) {
					//html.Init(getLocale(c.getLang()), messagesource);
					deleteHtml(html, c);
				}
				deleteNarrowdown(c); // 絞り込み削除
				err = service.delete(id);
				if (!err.isError()) {
					String res = "success.count=" + err.getCount();

					mav.setViewName("redirect:/login/admin/category" + typeUrl + "/" + parentId + "/?message=" + res);
				} else {
					mav.addObject("error", err.getMessage());
				}
			}

			// リスト表示設定
			Category p = null;
			p = service.getWithChildren(parentId, active, err);

			if (p != null) log.debug("c=" + p.toString());

			//Add to View
			mav.addObject("category", p);

			//パンくず
			setBreadcrumb(mav, p);

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
			@ModelAttribute("categoryForm") CategoryForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Get by id
		ErrorObject obj = new ErrorObject();
		Category ca = service.get(id, obj);
		String setId = null;
		if (ca.getState().equals(ModelState.PROD)) {
			mav.setViewName("redirect:/login/admin/category/edit/" + ca.getId() + "?lang="+ ca.getLang() + "&notTest=1");
			return mav;
		}
		else if (ca.getLang().equals(lang)) {
			setId = ca.getParentId();
		} else {
			List<Category> list = service.listLangRef(ca.getParentId(), obj);
			for(Category c : list) {
				if (c.getLang().equals(lang)) {
					setId = c.getId();
					break;
				}
			}
		}
		if (setId != null) {
			mav.setViewName("redirect:/login/admin/category/new?parentId=" + setId + "&langRefId=" + ca.getId());
		} else {
			// エラー
			mav.setViewName("redirect:/login/admin/category/edit/" + ca.getId() + "?lang="+ ca.getLang() + "&notParent=1" );
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
			@ModelAttribute("categoryForm") CategoryForm myform,
			@RequestParam(name = "parentId", required = false, defaultValue = "") String parentId,
			@RequestParam(name = "langRefId", required = false, defaultValue = "") String langRefId) {

		//Set view
		mav.setViewName("/login/admin/category/edit");
		myform.setParentId(parentId);

		ErrorObject obj = new ErrorObject();
		if (parentId != null && parentId.isEmpty() == false) {
			Category ca = service.get(parentId, obj);
			myform.setLang(ca.getLang());
			myform.setState(ca.getState());
			myform.setActive(true);
			myform.setType(ca.getType());
		}
		if (langRefId != null && langRefId.isEmpty() == false) {
			setEditRefParam(mav, service.get(langRefId, obj));
		}

		//get breadcrumb
		//パンくず取得
		List<Category> breadcrumb = service.getParents(parentId,  active, new ErrorObject());
		//Add to View
		if (breadcrumb != null) {
			mav.addObject("breadcrumb", breadcrumb);
		}

		//Set FormObject to view
		mav.addObject(myform);

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
			@ModelAttribute("categoryForm") CategoryForm myform,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "lang", required = false) String lang,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		// Get by id
		ErrorObject obj = new ErrorObject();
		Category ca = service.get(id, obj);
		String setId = null;
		if (ca.getLang().equals(lang)) {
			setId = ca.getId();
		} else {
			List<Category> list = service.listLangRef(id, obj);
			for(Category c : list) {
				if (c.getLang().equals(lang)) {
					setId = c.getId();
					break;
				}
			}
		}
		if (setId != null) {
			mav.setViewName("redirect:/login/admin/category/edit/" + setId);
		}
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
			@ModelAttribute("categoryForm") CategoryForm myform,
			@PathVariable(name = "id", required = false) String id,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		// Set view
		mav.setViewName("/login/admin/category/edit");

		// Get by id
		ErrorObject obj = new ErrorObject();
		Category category = service.get(id, obj);
		s_state.setProd(category.getState().equals(ModelState.PROD));

		setEditRefParam(mav, category);

		// Map(Copy) Category -> Form
		modelMapper.map(category, myform);

		//Add Form to View
		mav.addObject(myform);
		mav.addObject("category", category);

		//パンくず
		setBreadcrumb(mav, category);

		return mav;
	}

	/**
	 * POSTされたデータからDB更新
	 * @param mav
	 * @param form
	 * @param result
	 * @implNote 2024/10/24 Compare追加。小カテゴリのみ。配下のSeriesのSpecを集計
	 * @return
	 */
	@RequestMapping(value = "/post", method = RequestMethod.POST)
	public ModelAndView post(
			ModelAndView mav,
			@RequestParam(name = "import_before", required = false, defaultValue = "") String beforeUrl,
			@Validated @ModelAttribute("categoryForm") CategoryForm form,
			BindingResult result) {
		// XXX BindingResultは@Validatedの直後の引数にする

		// Set view
		mav.setViewName("/login/admin/category/edit");
		ErrorObject obj = new ErrorObject();

		log.debug(form.toString());

		User s_user = (User)session.getAttribute("SessionUser");
		// 1) - 7)までが基本的な更新処理の流れ

		Category category = new Category();

		// 1) idがあれば(=編集) dbから取得
		if (form.getId() != null && form.getId().isEmpty() == false) {
			category = service.get(form.getId(), obj);
			// 3) フォームvalidate
			validator.validateUpate(result, form);

		} else {
			//新規の場合はそのまま
			validator.validateNew(result, form);
		}

		log.debug(result.toString());

		// 4) エラー判定
		if (!result.hasErrors()) {

			// UpdateならSlugを確認して、SeriesのBreadcrumbにあるSlugを更新
			if (form.getId() != null && form.getId().isEmpty() == false) {
				String slug = form.getSlug();
				Category before = service.get(form.getId(), obj);
				if (slug.equals(before.getSlug()) == false)
				{
					seriesService.updateSlug(form.getId(), before.getSlug(), slug);
				}
			}
			String htmlPath = "";
			String tmp = "";
			try {
				htmlPath = env.getProperty("smc.webcatalog.static.page.path");
			} catch (Exception e) {
				e.printStackTrace();
				htmlPath = tmp;
		    }
			boolean isUpdate = false;
			// 本番修正時
			if (form.getState().equals(ModelState.PROD) && form.getId() != null && form.getId().isEmpty() == false) {
				String afterSlug = form.getSlug();
				String preSlug = category.getSlug();
				if (preSlug != null && preSlug.equals(afterSlug) == false) {
					// ディレクトリの移動
					File file = FileUtils.getFile(htmlPath + form.getLang() + "/" + preSlug);
					File file2 = FileUtils.getFile(htmlPath + form.getLang() + "/" + afterSlug);
					try {
						FileUtils.moveDirectory(file, file2);
					} catch (IOException e) {
						log.error("moveDirectory() from:"+file+ " to:"+ file2);
					}
					isUpdate = true;
				} else if (form.getName().equals(category.getName()) == false) {
					isUpdate = true;
				}
			}
			
			// 2024/10/24 配下のSeriesのSpecを集計
			if (form.getId() != null) { // 新規は除く
				Category p = service.get(form.getParentId(), obj);
				if (p.isRoot() == false) {
					// narrow_down_compareは削除。2025/11
					/*if (form.isCompare()) {
						
						Category c = service.getWithSeries(form.getId(), true, obj);
						
						if (c != null && c.isCompare() == false && form.isCompare()) { 

							narrowDownService.deleteCategoryCompare(form.getId()); // 一旦全削除
							
							List<Series> sList = c.getSeriesList();
							List<String> saveList = new LinkedList<>();
							for(Series s : sList) {
								String spec = s.getSpec();
								try {
									JSONArray res = new JSONArray(spec.replace("\r\n", "").replace("\t", ""));
									JSONArray arr = (JSONArray)(res.get(0));
									for(Object ob : arr) {
										String str = (String)ob;
										if (str != null && str.isEmpty() == false) {
											if (str.trim().equals("シリーズ") || str.trim().equals("Series") || str.trim().equals("系列")) {
												if (saveList.contains(str) == false) saveList.add(0, str); // シリーズは必ず先頭！
											} else if (str.indexOf('[') == 0) {
												// [ で始まる場合は無視。[2DCAD]など。
											} else if (saveList.contains(str)) {
												// すでに入っている場合は何もしない。
											} else {
												saveList.add(str);
											}
										}
								    }
								} catch (Exception e) {
									log.error("category post() JSONObject parse error. source:"+spec);
								}
							}
							if (saveList.size() > 0) {
								int order = 0;
								for(String str : saveList) {
									NarrowDownCompare comp = new NarrowDownCompare();
									comp.setActive(true);
									comp.setTitle(str);
									comp.setId(null);
									comp.setCategoryId(form.getId());
									comp.setOrder(order);
									narrowDownService.saveCompare(comp);
									order++;
								}
							}
						}
					} else {
						// delete
						narrowDownService.deleteCategoryCompare(form.getId());
					}*/
					if (form.isNarrowdown() == false) {
						narrowDownService.deleteCategoryColumn(form.getId());
						narrowDownService.deleteCategoryValue(form.getId());
					}
				}
			}

			// 5) フォームからCategoryにコピー
			modelMapper.map(form, category);
			log.debug("Category(FORM)="+category.toString());
			category.setUser(s_user);

			// 6) 保存
			obj = service.save(category);

			if (obj.isError() == false) {
				if (isUpdate && category.getState().equals(ModelState.PROD)) {
					// 本番を編集した場合はHTMLも書き出し
					//html.Init(getLocale(form.getLang()), messagesource);
					outputHtml(html, category);
				}
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
		mav.addObject("category", category);

		//パンくず
		setBreadcrumb(mav, category);

		return mav;
	}
	/**
	 * 管理系 > カテゴリ > CSVダウンロード
	 * @param myform
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "/csv", method = RequestMethod.GET)
	public void csv(
			HttpServletResponse response,
			@RequestParam(name = "type", required = false, defaultValue = "") String type, // detailならDetail+SpecをUTF-8で返却 2025/3/21
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		String lang = "ja-jp";
		String code = "utf-8";
		if (s_state.getLang() != null && s_state.getLang().isEmpty() == false) {
			lang = s_state.getLang();
			if (type.equals("detail") == false) {
				if (lang.equals("ja-jp") || lang.equals("en-jp")) {
					code = "MS932";
				}
			}
		}
		//文字コードと出力するCSVファイル名を設定
        response.setContentType(MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE + ";charset="+code);
        response.setHeader("Content-Disposition", "attachment; filename=\"series_list_"+lang+".csv\"");

		// Get by id
		ErrorObject obj = new ErrorObject();
		Category root = service.getRoot(s_state.getLang(), ModelState.TEST, CategoryType.CATALOG, obj);
		
		String csv = "";
		String pdfTitle = "カタログ閲覧";
		String movieTitle = "動画";
		if (lang.equals("en-jp")) {
			pdfTitle = "Catalogs";
			movieTitle = "Video";
		} else if (lang.equals("zh-cn")) {
			pdfTitle = "产品样本";
			movieTitle = "视频";
		} else if (lang.equals("zh-cn")) {
			pdfTitle = "目錄瀏覽";
			movieTitle = "影片";
		}

		if (root != null) {
			Category rc = service.getWithChildren(root.getId(), null, obj);
			if (rc != null) {
				for(Category c1 : rc.getChildren()) {
					Category c1ch = service.getWithChildren(c1.getId(), null, obj);
					if (c1ch != null) {
						for(Category c2 : c1ch.getChildren()) {
							Category c2ws = service.getWithSeries(c2.getId(), null, obj);
							if (c2ws != null) {
								for(Series s : c2ws.getSeriesList()) {
									// [WEBカタログ] #348 WEBカタログ情報の件 2023/08/17 11:22:50
									// 大カテゴリ、中カテゴリ、名称、シリーズ名、SeriesID、id
									// [WEBカタログ] #780 WEBカタログIDの件 2024/09/13 14:47
									// [WEBカタログ] #964 WEBカタログ写真パス出力の件
									// [SMC更新] #9377 WEBカタログ csv 書き出しに関して 2025/3/21 Detail,Spec追加
									// カタログPDFのURLと動画のURL
									String image = "";
									if (s.getImage() != null) {
										image = AppConfig.ImageProdUrl + s.getLang() + "/" + s.getImage();
									}
									String pdf = "";
									String movie = "";
									List<SeriesLink> sLink = seriesService.getLink(s.getId(), obj);
									for(SeriesLink link : sLink) {
										if (link.getLinkMaster().getName().equals(pdfTitle)) {
											pdf = link.getUrl();
										} else if (link.getLinkMaster().getName().equals(movieTitle)) {
											movie = link.getUrl();
											break;
										}
									}
									String spec = "";
									List<Integer> sortList = new LinkedList<>();
									if (type.equals("detail") && s.getSpec() != null && s.getSpec().isEmpty() == false) {
										try {
											JSONArray res = new JSONArray(s.getSpec().replace("\r\n", "").replace("\t", "").replace("null", "")); // 改行があると読み込みエラーになるので、一旦タブへ。
											int cnt = 0;
											for (Object r : res) {
												if (cnt > 0) {
													if (spec != null && spec.isEmpty() == false) {
														spec = spec.substring(0, spec.length()-1);
														spec += "〓";
													}
												}
												if (cnt == 0) { // 項目行
													int col = 0;
													for(Object ob : (JSONArray)r) {
														String str = (String)ob;
														if (str != null && str.isEmpty() == false) {
															if (str.trim().equals("シリーズ") || str.trim().equals("Series") || str.trim().equals("系列")) {
																if (sortList.contains(col) == false) sortList.add(0, col); // シリーズは必ず先頭！
																spec = str+"\t"+spec;
															} else if (str.indexOf('[') == 0) {
																// [ で始まる場合は無視。[2DCAD]など。
															} else if (sortList.contains(col)) {
																// すでに入っている場合は何もしない。
															} else {
																if (sortList.contains(col) == false) sortList.add(col);
																spec += str+"\t";
															}
														}
														col++;
												    }
												} else if (sortList.size() > 0){
													JSONArray arr = (JSONArray)r;
													for(Integer i : sortList) {
														spec += (String)arr.get(i)+"\t";
													}
												}
												cnt++;
											}
											if (spec != null && spec.isEmpty() == false) spec = spec.substring(0, spec.length()-1);
										} catch (Exception e) {
											log.error("category csv() JSONObject parse error. sid:"+s.getModelNumber());
											log.error("category csv() JSONObject parse error. source:"+spec);
											log.error("category csv() JSONObject parse error. Exception:"+e);
										}
									} 
									if (type.equals("detail") == false) {
										csv += '"'+c1.getName()+"\",\""+c2.getName()+"\",\""+s.getName()+"\",\""+s.getNumber()+"\",\""+s.getModelNumber()+"\",\""+
												image+"\",\""+pdf+"\",\""+movie+"\","+s.isActive()+","+s.getId()+"\r\n";
									} else {
										csv += '"'+c1.getName()+"\",\""+c2.getName()+"\",\""+s.getName()+"\",\""+s.getNumber()+"\",\""+s.getModelNumber()+"\",\""+
											image+"\",\""+pdf+"\",\""+movie+"\",\""+s.getDetail()+"\",\""+spec+"\","+s.isActive()+","+s.getId()+"\r\n";
									}
									
								}
							}
						}
					}
					
				}
			}
			try (PrintWriter pw = response.getWriter()) {
	            //CSVファイルに書き込み
				if (type.equals("detail") == false && code.equals("MS932")) {
					csv = new String(csv.getBytes(Charset.forName("MS932")),Charset.forName("MS932"));
				}
	            pw.print(csv);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}

		return ;
	}

	// ========== private ==========
	public static Path getApplicationPath(Class<?> cls) throws URISyntaxException {
		ProtectionDomain pd = cls.getProtectionDomain();
		CodeSource cs = pd.getCodeSource();
		URL location = cs.getLocation();
		URI uri = location.toURI();
		Path path = Paths.get(uri);
		return path;
	}
	// 2026/3 デザインリニューアルに伴い、templateにもTEST PRODを追加。書き出しは必ずPROD!!
	private ErrorObject outputHtml(LibHtml html, Category category) {
		// 配下すべてのHTML更新
		Category c1 = category;
		Category c2 = null;
		ErrorObject err = new ErrorObject();
		Template t = templateService.getTemplateFromBean(category.getLang(), category.getState()); 

		boolean isFirstCategory = false; // 大カテゴリ
		Category root = service.getRoot(category.getLang(), category.getState(), category.getType(), err);
		if (root.getId().equals(category.getParentId())) {
			isFirstCategory = true;
		}
		TemplateCategory tc = null;
		if (category.getState().equals(ModelState.PROD)) {
			if (isFirstCategory) {
				tc = templateCategoryService.findByCategoryIdFromBean(category.getLang(), category.getState(), category.getId());
			} else {
				tc = templateCategoryService.findByCategoryIdFromBean(category.getLang(), category.getState(), category.getParentId());
				c1 = service.get(category.getParentId(), err);
				c2 = category;
				err = new ErrorObject();
			}
			if (tc == null) {
				err = new ErrorObject();
				err.setCode(ErrorCode.E10001);
				err.setMessage("Template is Empty!");
			}
		} else {
			// PRODのカテゴリを探す
			Category prodC = service.getStateRefId(c2, ModelState.PROD, err);
			if (isFirstCategory) {
				tc = templateCategoryService.findByCategoryIdFromBean(prodC.getLang(), prodC.getState(), prodC.getId());
			} else {
				tc = templateCategoryService.findByCategoryIdFromBean(prodC.getLang(), prodC.getState(), prodC.getParentId());
				c1 = service.get(prodC.getParentId(), err);
				c2 = prodC;
				err = new ErrorObject();
			}
			if (tc == null) {
				err = new ErrorObject();
				err.setCode(ErrorCode.E10001);
				err.setMessage("Template is Empty!");
			}
		}

		if (err.isError() == false) {
			Category withSeries = service.getWithSeries(category.getId(), true, err);
			if (withSeries != null) {
				if (category.getState().equals(ModelState.PROD)) {
					// SeriesのTESTが Active=false の場合、PRODもActive=falseへ
					for(Series s : withSeries.getSeriesList()) {
						Series testS = seriesService.get(s.getStateRefId(), err);
						if (testS != null && testS.isActive() == false) {
							s.setActive(false);
							seriesService.save(s);
						}
					}
				}
				// カテゴリ静的Html吐き出し
				boolean r = false;
				if (tc.is2026()) {
					r = html.outputTemplateCategoryToHtml2026(t, tc, c1, c2, withSeries.getSeriesList(), service, seriesService);
				} else {
					r = html.outputTemplateCategoryToHtml(t, tc, c1, c2, withSeries.getSeriesList(), service, seriesService);
				}
				if (r == false) {
					err = new ErrorObject();
					err.setCode(ErrorCode.E10001);
					err.setMessage("Error! Didn't make Category HTML! slug="+ c1.getSlug());
				}
				
				if (c2 == null) {
					// 大カテゴリなら配下のカテゴリも更新
					Category withChild = service.getWithChildren(category.getId(), true, err);
					if (withChild != null) {
						for(Category ch : withChild.getChildren()) {
							withSeries = service.getWithSeries(ch.getId(), true, err);
							if (withSeries != null &&  withSeries.getSeriesList() != null) {
								for(Series s : withSeries.getSeriesList()) {
									Series testS = seriesService.get(s.getStateRefId(), err);
									if (testS != null && testS.isActive() == false) {
										s.setActive(false);
										seriesService.save(s);
									}
								}
							}
							boolean r2 = false;
							if (tc.is2026()) {
								r2 = html.outputTemplateCategoryToHtml2026(t, tc, category, ch, withSeries.getSeriesList(), service, seriesService); // 小カテゴリ静的Html吐き出し
							} else {
								r2 = html.outputTemplateCategoryToHtml(t, tc, category, ch, withSeries.getSeriesList(), service, seriesService); // 小カテゴリ静的Html吐き出し
							}
							if (r2 == false) {
								err = new ErrorObject();
								err.setCode(ErrorCode.E10001);
								err.setMessage("Error! Didn't make Category HTML! slug="+category.getSlug() + "/" + ch.getSlug());
							}

						}
					}
					
				}
			}
		}
		return err;
	}

	// 配下のHtml、Dirをすべて削除。Prodの削除時。（非Active時はProductControllerで制御）
	private ErrorObject deleteHtml(LibHtml html, Category category) {
		ErrorObject err = new ErrorObject();

		// 配下のカテゴリがあれば削除
		Category withChild = service.getWithChildren(category.getId(), null, err);
		if (withChild != null) {
			for(Category ch : withChild.getChildren()) {
				html.deleteCategory(category, ch, service);
			}
		}
		Category c = service.get(category.getId(), err);
		if (c != null) {
			html.deleteCategory(category, null, service);
		}

		return err;
	}

	// 配下のNarrowdown、Column, valueを削除。
	private void deleteNarrowdown(Category category) {

		//narrowDownService.deleteCategoryCompare(category.getId()); // narrow_down_compare削除。2025/11
		narrowDownService.deleteCategoryColumn(category.getId());
		narrowDownService.deleteCategoryValue(category.getId());

		return ;
	}
	private ErrorObject setDispListParam(ModelAndView mav, String id, String lang, ModelState state, CategoryType type) {
		ErrorObject err = new ErrorObject();
		Category c = null;
		if (id == null || id.isEmpty())
		{
			List<Category> list = service.listAll(lang, state, type, err);
			if (list != null) {
				c = service.getWithChildren(list.get(0).getId(), active, err);
			}
		}
		else
		{
			c = service.getWithChildren(id, active, err);
		}

		if (c != null) {
			log.debug("category=" + c.toString());
		} else { // カラでもlang,state,typeを入れておかないと「新規」ボタンを出せない。
			c = new Category();
			c.setLang(lang);
			c.setState(state);
			c.setType(type);
		}

		//Add to View
		mav.addObject("category", c);
		if (lang != null) mav.addObject("lang", lang);

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
	private ErrorObject setEditRefParam(ModelAndView mav, Category category) {
		ErrorObject err = new ErrorObject();
		// Get by langRefId
		// langRefIdがnullなら元になるID
		String langBaseId = null;
		if (category.getLangRefId() == null || category.getLangRefId().isEmpty()) {
			langBaseId = category.getId();
		}
		else {
			langBaseId = category.getLangRefId();
		}
		List<Category> langRefList = service.listLangRef(langBaseId, err);
		Category langBaseCategory = service.get(langBaseId, err);
		if (langBaseCategory != null) {
			langRefList.add(0, langBaseCategory);
		}
		List<String> langs = new ArrayList<String>();
		for(Category c : langRefList) {
			langs.add(c.getLang());
		}

		// Get by TEST or PROD
		Category diffCategory = null;
		if (category.getState().equals(ModelState.TEST)) {
			diffCategory = service.getStateRefId(category, ModelState.PROD, err);
		} else {
			diffCategory = service.getStateRefId(category, ModelState.TEST, err);
		}

		mav.addObject("langRefList", langs);
		mav.addObject("diffCategory", diffCategory);
		mav.addObject("langBaseId", langBaseId);

		return err;
	}
	private void setBreadcrumb(ModelAndView mav, Category c) {
		List<Category> breadcrumb = null;
		ErrorObject err = new ErrorObject();

		if (c != null) {
			if (c.getParentId() != null && c.getParentId().isEmpty() == false) {
				breadcrumb = service.getParents(c.getId(), active, err);
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
	}

}
