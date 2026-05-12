package com.smc.webcatalog.web;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.smc.omlist.service.OmlistServiceImpl;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.NarrowDownColumn;
import com.smc.webcatalog.model.NarrowDownValue;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesHtml;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.NarrowDownService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

/**
 * テストで表示を見たい場合。
 * TEST確認後PRODへアップ
 * @author tfujishima
 *
 */
@Controller
@Slf4j
@RequestMapping("/login/admin/preview")
@ResponseBody
public class PreviewController extends BaseController {

	@Autowired
	LangService langService;

	@Autowired
	SeriesService seriesService;

	@Autowired
	CategoryService categoryService;

	@Autowired
	TemplateService templateService;

	@Autowired
	TemplateCategoryService templateCategoryService;

	@Autowired
	OmlistServiceImpl omlistService;
	
	@Autowired
	NarrowDownService narrowDownService;
	
    @Autowired
    SeriesFaqRepository faqRepo;

	@Autowired
    MessageSource messagesource;

	@Autowired
    HttpServletRequest req;

    @Autowired
	LibHtml html;

	/**
	 * @note 対象URL
	 * 言語トップ:/products/ja-jp/
	 * 大カテゴリ:/products/ja-jp/{slug}
	 * 小カテゴリ(対象シリーズリスト):/products/ja-jp/{slug}/{slug2}
	 * 検索結果(対象シリーズリスト):/products/ja-jp/search/?kw={kw}
	 * シリーズ:/products/ja-jp/series/{seid}
	 */
	/**
	 * 言語トップ
	 * @return String HTML
	 */
	@GetMapping({ "/{lang}"})
	public String lang(@PathVariable(name = "lang", required = false) String lang) {

		String ret = "";

		ErrorObject err = new ErrorObject();
		Lang langObj = langService.getFromContext(lang);
		String baseLang = lang;
		if (langObj == null) {
			log.error("Bad lang! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Bad lang!!"
					);
		}
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		Template t = templateService.getLangAndModelState(baseLang, ModelState.TEST, null, err); // previewなのでTEST固定
		Template orgT = templateService.getLangAndModelState(lang, ModelState.TEST, null, err);
		if (t == null) {
			log.error("Template is Empty! lang=" + baseLang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Template is Empty!"
					);
		} else if (langObj.isVersion()) {
			if (orgT != null && orgT.getHeader() != null && orgT.getHeader().isEmpty() == false) {
				ret+=orgT.getHeader();
			} else {
				ret+= t.getHeader();
			}
			if (orgT != null && orgT.getContents() != null && orgT.getContents().isEmpty() == false) {
				ret+=orgT.getContents();
			} else {
				ret+= t.getContents();
			}
			if (orgT != null && orgT.getFooter() != null && orgT.getFooter().isEmpty() == false) {
				ret+=orgT.getFooter();
			} else {
				ret+= t.getFooter();
			}
		} else {
			ret+=t.getHeader();
			ret+=t.getContents();
			ret+=t.getFooter();
		}
		if (langObj.isVersion()) {
			// 変換処理
			//html.Init(getLocale(baseLang), messagesource);
			Template toT = templateService.getLangAndModelState(lang, ModelState.TEST, null, err); // previewなのでTEST固定
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);

		}
		return ret;
	}

	/**
	 * Slugから大カテゴリ
	 * @return String HTML
	 */
	@GetMapping({ "/{lang}/{slug}"})
	public String slug(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "slug", required = false) String slug) {

		String ret = "";
		ErrorObject err = new ErrorObject();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		// プレビューなので、active=falseでも見せる。
		Boolean active = null;
		Category c = categoryService.getFromSlug(slug, baseLang, ModelState.TEST, CategoryType.CATALOG, 1, active, err);
		if (c == null) {
			c = categoryService.getFromSlug(slug, baseLang, ModelState.TEST, CategoryType.OTHER, 1, active, err);
		}
		Template t = templateService.getTemplateFromBean(baseLang, ModelState.TEST); // previewはTEST固定
		TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(baseLang, c.getState(), c.getId());

		if (t == null) {
			log.error("Template is Empty! lang=" + baseLang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Template is Empty!"
					);
		} else if (tc == null) {
			log.error("Category Template is Empty! categoryId=" + c.getId());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Category Template is Empty!"
					);
		} else if (tc.is2026()) {
			// 2026 リニューアル
			String temp = tc.getTemplate();

			String catpan = c.getName();
			String formbox = tc.getFormbox();
			String h1box =tc.getH1box();
			String content = tc.getContent();

			Category withSeries = categoryService.getWithSeries(c.getId(), true, err);
			List<Series> list = withSeries.getSeriesList();
			if (list != null && list.size() > 0 ) { // 大カテゴリにシリーズ一覧がある場合

				SeriesHtml sHtml = new SeriesHtml(getLocale(baseLang), messagesource, omlistService, faqRepo);
				for(Series s: list) {
					s.setLink(seriesService.getLink(s.getId(), err));
					content+= sHtml.get(s, c, null, AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug()+"/", c.getLang(), false, false);
				}
			}
			
			// catpan
			List<String> titleList = new LinkedList<>();
			titleList.add(catpan);
			List<String> slugList = new LinkedList<>();
			slugList.add(c.getSlug());
			temp = StringUtils.replace(temp,"$$$catpan$$$", html.getCatpan2026(c.getLang(), tc.getCatpan(), titleList, slugList));
			
			// sidebar
			String sidebar = tc.getSidebar();
			List<Category> cList = categoryService.listAll(c.getLang(), c.getState(), c.getType(), err);
			List<Category> setCategoryList = new LinkedList<>();
			for(Category cate :  cList) {
				Category setC = categoryService.getWithSeries(cate.getId(), null, err);
				if (setC != null) setCategoryList.add( setC );
			}
			List<String> strCate = html.getCategoryMenu2026(lang, c.getId(), null, setCategoryList);
			sidebar = StringUtils.replace(sidebar,"$$$category$$$",strCate.get(0));
			sidebar = StringUtils.replace(sidebar,"$$$category2$$$",strCate.get(1));
			sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", "");
			temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);

			temp = StringUtils.replace(temp,"$$$formbox$$$", formbox);
			if (lang.equals("ja-jp")) {
				temp = StringUtils.replace(temp,"$$$h1box$$$", html.getH1box2026(h1box, c.getName()));
			} else {
				temp = StringUtils.replace(temp,"$$$h1box$$$", html.getH1box2026(h1box, c.getName()));
			}
			temp = StringUtils.replace(temp, "$$$content$$$", content);
			
			ret = t.getHeader();
			ret+=temp;
			ret+=t.getFooter();
			
		} else {
			String temp = tc.getTemplate();
			Category withSeries = categoryService.getWithSeries(c.getId(), true, err); // カテゴリ一覧では「非公開」は抜く。シリーズ単体なら表示可能
			ret = t.getHeader();
			String catpan = c.getName();
			String formbox = tc.getFormbox();
			String h1box =tc.getH1box().replace("$$$title$$$", c.getName());
			String content = tc.getContent();
			List<Series> list = withSeries.getSeriesList();
			if (list != null && list.size() > 0 ) { // 大カテゴリにシリーズ一覧がある場合

				SeriesHtml sHtml = new SeriesHtml(getLocale(baseLang), messagesource, omlistService, faqRepo);
				for(Series s: list) {
					s.setLink(seriesService.getLink(s.getId(), err));
					content+= sHtml.get(s, c, null, AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug()+"/", c.getLang(), false, false);
				}
			}
			temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", catpan));
			String sidebar = tc.getSidebar();
			List<Category> cList = categoryService.listAll(c.getLang(), c.getState(), c.getType(), err);
			List<Category> setCategoryList = new LinkedList<>();
			for(Category cate :  cList) {
				Category setC = categoryService.getWithSeries(cate.getId(), null, err);
				if (setC != null) setCategoryList.add( setC );
			}
			List<String> category = html.getCategoryMenu(c, null, setCategoryList);
			sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
			sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
			sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", "");
			temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);
			temp = StringUtils.replace(temp,"$$$formbox$$$", formbox);
			temp = StringUtils.replace(temp,"$$$h1box$$$", h1box);
			temp = StringUtils.replace(temp, "$$$content$$$", content);
			ret+=temp;
			ret+=t.getFooter();
		}
		if (langObj.isVersion()) {
			// 変換処理
			//html.Init(getLocale(baseLang), messagesource);
			Template toT = templateService.getLangAndModelState(lang, ModelState.TEST, null, err); // previewなのでTEST固定
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);

		}
		return ret;
	}

	/**
	 * Slug配下の一覧
	 * @return String HTML
	 */
	@GetMapping({ "/{lang}/{slug}/{slug2}"})
	public String listBySlug(HttpServletRequest request,
			@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "slug", required = true) String slug,
			@PathVariable(name = "slug2", required = true) String slug2,
			@RequestParam(name = "view", required = false) String view,
			@RequestParam(name = "action", required = false) String action,
			@RequestParam(name = "narrowKey", required = false) String[] key,
			@RequestParam(name = "narrowCnt", required = false) String narrowDownCount,
			@RequestParam(name = "nCnt", required = false) String nCnt,
			@CookieValue(value="WebCatalogListState", required=false) Cookie cookie) {

		String ret = "";
		ErrorObject err = new ErrorObject();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		
		int ndCnt = 0; // 絞り込み表示の項目数
		if (key != null && key.length > 0) ndCnt = key.length; // narrowKeyが無くなれば削除！2025/11/25
		else if (narrowDownCount != null) ndCnt = Integer.parseInt(narrowDownCount);
		else if (nCnt != null && ndCnt == 0) ndCnt = Integer.parseInt(nCnt);

		// プレビューなので、active=falseでも見せる。
		Boolean active = true;
		Category c = categoryService.getFromSlug(slug, baseLang, ModelState.TEST, CategoryType.CATALOG, 1, active, err); //
		if (c == null) {
			c = categoryService.getFromSlug(slug, baseLang, ModelState.TEST, CategoryType.OTHER, 1, active, err);
		}
		Category c2 = categoryService.getFromSlugSecond(slug2, c.getId(), baseLang, ModelState.TEST, CategoryType.CATALOG,  active, err);
		if (c2 == null) {
			c2 = categoryService.getFromSlugSecond(slug2, c.getId(), baseLang, ModelState.TEST, CategoryType.OTHER, active, err);
		}

		Template t = templateService.getTemplateFromBean(baseLang, ModelState.TEST);
		TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(c.getLang(), c.getState(), c.getId());
		
		// リスト取得
		List<Series> list = null;
		if (action == null || action.isEmpty()) {
			list = seriesService.listSlug(c2, true, err); // カテゴリ一覧では「非公開」は抜く。シリーズ単体なら表示可能
		} else {
			// 絞り込み検索
			list = narrowDownService.getNarrowDown(c2.getId(), request, err);
		}
		if (list != null) log.debug("list=" + list.toString());

		if (t == null) {
			log.error("Template is Empty! lang=" + baseLang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Template is Empty!");
		} else if (tc == null) {
			log.error("Category Template is Empty! categoryId=" + c.getId());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Category Template is Empty!"
					);
		} else if (tc.is2026()) {
			// 2026 リニューアル
			String temp = tc.getTemplate();
			String formbox = tc.getFormbox();
			String h1box =tc.getH1box();
			// catpan
			List<String> titleList = new LinkedList<>();
			titleList.add(c.getName());
			titleList.add(c2.getName());
			List<String> slugList = new LinkedList<>();
			slugList.add(c.getSlug());
			slugList.add(c2.getSlug());
			temp = StringUtils.replace(temp,"$$$catpan$$$", html.getCatpan2026(c.getLang(), tc.getCatpan(), titleList, slugList));
			// sidebar
			String sidebar = tc.getSidebar();
			List<Category> cList = categoryService.listAll(c.getLang(), c.getState(), c.getType(), err);
			List<Category> setCategoryList = new LinkedList<>();
			for(Category cate :  cList) {
				Category setC = categoryService.getWithSeries(cate.getId(), null, err);
				if (setC != null) setCategoryList.add( setC );
			}
			List<String> strCate = html.getCategoryMenu2026(lang, c.getId(), null, setCategoryList);
			sidebar = StringUtils.replace(sidebar,"$$$category$$$",strCate.get(0));
			sidebar = StringUtils.replace(sidebar,"$$$category2$$$",strCate.get(1));
			String narrowDown = html.getNarrowDown2026(c.getLang(), c, c2, request); // 絞り込み検索
			sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", narrowDown);
			
			temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);
			temp = StringUtils.replace(temp,"$$$formbox$$$", formbox);
			
			String viewStr = html.getListDisplaySelection2026(lang, c2, view, action, ndCnt, request);
			String h1Str = html.getH1box2026(h1box, c2.getName());

			String content = "<div class=\"mb48\">\r\n";
			SeriesHtml sHtml = new SeriesHtml(getLocale(baseLang), messagesource, omlistService, faqRepo);
			if (list != null && list.size() > 0) {
				if (view != null && view.equals("list")) {
					int cnt = 0;
					int max = list.size();
					for(Series s: list) {
						s.setLink(seriesService.getLink(s.getId(), err));
						String tmp = sHtml.getGuide2026(s, c, c2, request.getRequestURI(), c.getLang(), false, false);
						content += tmp.replace("<div class=\"isLoginFalse w-full\" ", "<p>シリーズID:"+s.getModelNumber()+"</br>DB ID:"+s.getId()+"</p><div class=\"isLoginFalse w-full\" ");
						if (cnt < max-1) {
							content += "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>";
						}
						cnt++;
					}
					h1Str += viewStr;
				} else if (view != null && view.equals("compare")) {
					HashMap<String, List<NarrowDownValue>> map = new HashMap<>();
					for (Series s : list) {
						List<NarrowDownValue> valList = narrowDownService.getCategorySeriesValue(c2.getId(), s.getId(), true, err);
						map.put(s.getId(), valList);
					}
					List<NarrowDownColumn> colList = narrowDownService.getCategoryColumn(c2.getId(), true, err);
					content += sHtml.getCompareHtml2026(lang, baseLang, c, c2, colList, list, map, null);
					h1Str += viewStr;
				} else {
					content += sHtml.getPictureList2026(c, c2, viewStr, list);
				}
			} else {
				if (baseLang.indexOf("en-") > -1) {
					content = "There were no series that matched the criteria.";
				} else if(baseLang.equals("zh-tw")){
					content = "<h4>沒有符合標準的系列。</h4>";
				} else if (baseLang.indexOf("zh-") > -1) {
					content = "<h4>没有符合标准的系列。</h4>";
				} else {
					content = "<h4>条件に一致したシリーズがありませんでした。</h4>";
				}
			}
			temp = StringUtils.replace(temp,"$$$h1box$$$",h1Str);
			
			if (view != null && view.equals("compare")) {
				content += "</div>\r\n";
			} else {
				content += "</div>\r\n" + tc.getProductsSupport();
			}
			temp = StringUtils.replace(temp, "$$$content$$$", content);
			
			ret = t.getHeader();
			ret+=temp;
			ret+=t.getFooter();
		} else {
			// Html生成
			ret+=t.getHeader();
			String temp = tc.getTemplate();
			String catpan = "";
			catpan +="<a href='"+AppConfig.ProdRelativeUrl+lang+"/"+slug+"'>"+c.getName()+"</a>";
			catpan +="&nbsp;»&nbsp;";
			catpan +="<a href='"+AppConfig.ProdRelativeUrl+lang+"/"+slug+"/"+slug2+"'>"+c2.getName()+"</a>";
			temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", catpan));
			String sidebar = tc.getSidebar();
			List<Category> cList = categoryService.listAll(c.getLang(), c.getState(), c.getType(), err);
			List<Category> setCategoryList = new LinkedList<>();
			for(Category cate :  cList) {
				Category setC = categoryService.getWithSeries(cate.getId(), null, err);
				if (setC != null) setCategoryList.add( setC );
			}
			String narrowDown = html.getNarrowDown(c.getLang(), c, c2, request); // 2024/10/24 絞り込み検索
			sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", narrowDown); // 2024/10/24 絞り込み検索
			List<String> category = html.getCategoryMenu(lang, c.getId(), c2.getId(), setCategoryList);
			sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
			if (category.size() > 1) {
				sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
			} else {
				sidebar = StringUtils.replace(sidebar,"$$$category2$$$","");
			}
			temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);
			temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getH1box().replace("$$$title$$$",c2.getName())); // ↓小カテゴリは逆
			// 2024/11/05 表示形式 一覧、写真、比較
			if (view == null || view.isEmpty()) {
				if (cookie == null) view = "picture";
				else {
					view = cookie.getValue();
					if (view == null) view = "picture";
					else if (view.equals("picture") == false && view.equals("list") == false && view.equals("compare")) view = "picture";
					else if (c2.isCompare() == false && view.equals("compare")) view = "picture";
				}
			}
			String viewStr = html.getListDisplaySelection(lang, c2, view, action, ndCnt, request);
			temp = StringUtils.replace(temp,"$$$h1box$$$", tc.getFormbox() + viewStr); // ↑小カテゴリは逆

			String content = "";
			SeriesHtml sHtml = new SeriesHtml(getLocale(baseLang), messagesource, omlistService, faqRepo);
			if (list != null && list.size() > 0) {
				if (view.equals("list")) {
					for(Series s: list) {
						s.setLink(seriesService.getLink(s.getId(), err));
						String tmp = sHtml.get(s, c, c2, request.getRequestURI(), c.getLang(), false, false);
						content += tmp.replace("<div class=\"isLoginFalse\" style=\"display:block;\">", "<p>シリーズID:"+s.getModelNumber()+"</br>DB ID:"+s.getId()+"</p><ul class=\"pro_service_bt\">");
					}
				} else if (view.equals("compare")) {
//					List<NarrowDownCompare> compareList = narrowDownService.getCategoryCompare(c2.getId(), true, err);
					HashMap<String, List<NarrowDownValue>> map = new HashMap<>();
					for (Series s : list) {
						List<NarrowDownValue> valList = narrowDownService.getCategorySeriesValue(c2.getId(), s.getId(), true, err);
						map.put(s.getId(), valList);
					}
					List<NarrowDownColumn> colList = narrowDownService.getCategoryColumn(c2.getId(), true, err);
					content += sHtml.getCompareHtml(lang, baseLang, c, c2, colList, list, map, null); // old ～2026
				} else {
					content += sHtml.getPictureList(c, c2, list);
				}
			} else {
				if (baseLang.indexOf("en-") > -1) {
					content = "There were no series that matched the criteria.";
				} else if(baseLang.equals("zh-tw")){
					content = "<h4>沒有符合標準的系列。</h4>";
				} else if (baseLang.indexOf("zh-") > -1) {
					content = "<h4>没有符合标准的系列。</h4>";
				} else {
					content = "<h4>条件に一致したシリーズがありませんでした。</h4>";
				}
			}
			content = "<div class=\"p_block\">" + content + "</div>";
			temp = StringUtils.replace(temp, "$$$content$$$", content);
			ret+=temp;
			ret+= SeriesHtml._seriesCadModal;
			ret+=t.getFooter();
		}
		if (langObj.isVersion()) {
			// 変換処理
			//html.Init(getLocale(baseLang), messagesource);
			Template toT = templateService.getLangAndModelState(lang, ModelState.TEST, null, err); // previewなのでTEST固定
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);

		}

		return ret;
	}

	/**
	 * シリーズ取得
	 * @note 最後の0はページを作成しない場合。
	 * @return String HTML
	 */
	@GetMapping({ "/{lang}/{slug}/{slug2}/**","/{lang}/{slug}/{slug2}/{seId}", "/{lang}/series/{seId}/0"})
	public String getSeries(			HttpServletRequest request,
			@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "slug2", required = false) String slug2,
			@PathVariable(name = "seId", required = false) String seid) {

		String ret = "";
		ErrorObject err = new ErrorObject();

		if (seid == null && slug2 != null) {
			String tmp = request.getRequestURI();
			if (tmp != null) {
				int s = tmp.indexOf("/"+slug2+"/");
				seid = tmp.substring(s+("/"+slug2+"/").length());
			}
		}

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		boolean isSeriesOnly = false;
		if (slug == null && slug2 == null) isSeriesOnly = true;

		if (seriesService.isModelNumberExists(seid, ModelState.TEST, null, err) == false) {
			if (seid.contains(" ")) {
				String tmp = seid.replace(" ", "/"); // 半角スペースは/の場合あり。
				if (seriesService.isModelNumberExists(tmp, ModelState.TEST, null, err) == false) {
					log.error("ModelNumber Empty! seid=" + tmp);
					throw new ResponseStatusException(
							  HttpStatus.NOT_FOUND, "ModelNumber not found"
							);
				} else {
					seid = tmp;
				}
			} else {
				log.error("ModelNumber Empty! seid=" + seid);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ModelNumber not found"
						);
			}
		}
		Category c = null;
		if (slug != null && slug.isEmpty() == false ) {
			c = categoryService.getFromSlug(slug, baseLang, ModelState.TEST, CategoryType.CATALOG, 1, true, err);
			if (c == null) {
				log.error("Bad slug or Empty! slug=" + slug);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "slug not found"
						);
			}
		}
		Series s = seriesService.getFromModelNumber(seid,  ModelState.TEST, err);
		log.debug("series=" + s.toString());

		Category c2 = null;
		if (slug2 != null && slug2.isEmpty() == false ) {
			c2 = categoryService.getFromSlugSecond(slug2, c.getId(), baseLang, ModelState.TEST, CategoryType.CATALOG,  true, err);
			if (c2 == null) {
				log.error("Bad slug2 or Empty! slug2=" + slug2);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "slug2 not found"
						);
			}
		} else {
			String[] catList = s.getCatpansSlug(slug);
			if (catList != null && catList.length == 2) {
				slug2 = catList[1];
				c2 = categoryService.getFromSlug(slug2, baseLang, ModelState.TEST, CategoryType.CATALOG, 2, true, err);
			}
			if (catList != null && slug == null) {
				slug = catList[0];
				c = categoryService.getFromSlug(slug, baseLang, ModelState.TEST, CategoryType.CATALOG, 1, true, err);
			}
		}
		Template t = templateService.getTemplateFromBean(baseLang, ModelState.TEST);
		TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(baseLang, c.getState(), c.getId());

		// Html生成
		if (t == null) {
			ret = "Template is Empty!";
		} else if (tc == null) {
			ret = "Category Template is Empty!";
		} else if (tc.is2026()) {
			if (isSeriesOnly == false) ret+=t.getHeader();
			String temp = tc.getTemplate();
			
			if (isSeriesOnly == false) {
				List<String> titleList = new LinkedList<>();
				titleList.add(c.getName());
				titleList.add(c2.getName());
				titleList.add(s.getName());
				List<String> slugList = new LinkedList<>();
				slugList.add(c.getSlug());
				slugList.add(c2.getSlug());
				slugList.add(s.getModelNumber());
				temp = StringUtils.replace(temp,"$$$catpan$$$", html.getCatpan2026(c.getLang(), tc.getCatpan(), titleList, slugList));
			} else {
				temp = StringUtils.replace(temp,"$$$catpan$$$","");
			}
			if (isSeriesOnly == false) {
				String sidebar = tc.getSidebar();
				List<Category> cList = categoryService.listAll(c.getLang(), c.getState(), c.getType(), err);
				List<Category> setCategoryList = new LinkedList<>();
				for(Category cate :  cList) {
					Category setC = categoryService.getWithSeries(cate.getId(), null, err);
					if (setC != null) setCategoryList.add( setC );
				}
				List<String> category = html.getCategoryMenu2026(lang, c.getId(), c2.getId(), setCategoryList);
				sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
				if (category.size() > 1) sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
				else sidebar = StringUtils.replace(sidebar,"$$$category2$$$","");
				sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", "");
				temp = StringUtils.replace(temp,"$$$sidebar$$$",sidebar);
			}
			else temp = StringUtils.replace(temp,"$$$sidebar$$$","");
			temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox());
			temp = StringUtils.replace(temp,"$$$h1box$$$", "" );

			String content = "";
			SeriesHtml sHtml = new SeriesHtml(getLocale(baseLang), messagesource, omlistService, faqRepo);
			s.setLink(seriesService.getLink(s.getId(), err));
			String tmp = sHtml.getGuide2026(s, c, c2, request.getRequestURI(), lang, true, false);
			content += tmp.replace("<div class=\"isLoginFalse w-full\" ", "<p>シリーズID:"+s.getModelNumber()+"</br>DB ID:"+s.getId()+"</p><div class=\"isLoginFalse w-full\" ");

			String backUrl = request.getHeader("REFERER");
			if (backUrl != null && backUrl.isEmpty() == false && isSeriesOnly == false) {
				String mes = messagesource.getMessage("button.back.to.list", null,  getLocale(baseLang));
				String strBtn = "<div class=\"f fc mt16 mb48\">\r\n"
				+ "      <a class=\"button large primary solid w264 gap-8 s-w-full m-w-full\" href=\"$$$url$$$\">\r\n"
				+ "        <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left-white.svg\" alt=\"\" title=\"\">\r\n"
				+ "        <span>$$$message$$$</span>\r\n"
				+ "      </a>\r\n"
				+ "    </div>";
				content = StringUtils.replace(content, "$$$backUrl$$$", tc.getProductsSupport() + strBtn.replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
			} else {
				content = StringUtils.replace(content, "$$$backUrl$$$", tc.getProductsSupport() + ""); // ボタン自体消す
				
			}
			temp = StringUtils.replace(temp, "$$$content$$$", content);
			ret+=temp;
			ret+= SeriesHtml._seriesCadModal;
			if (isSeriesOnly == false) ret+=t.getFooter();
		} else {
			if (isSeriesOnly == false) ret+=t.getHeader();
			String temp = tc.getTemplate();
			String catpan = "";
			catpan +="<a href='"+AppConfig.ProdRelativeUrl+lang+"/"+slug+"'>"+c.getName()+"</a>";
			catpan +="&nbsp;»&nbsp;";
			catpan +="<a href='"+AppConfig.ProdRelativeUrl+lang+"/"+slug+"/"+slug2+"'>"+c2.getName()+"</a>";
			catpan +="&nbsp;»&nbsp;";
			catpan+= s.getName();

			if (isSeriesOnly == false) temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", catpan));
			else temp = StringUtils.replace(temp,"$$$catpan$$$","");
			if (isSeriesOnly == false) {
				String sidebar = tc.getSidebar();
				List<Category> cList = categoryService.listAll(c.getLang(), c.getState(), c.getType(), err);
				List<Category> setCategoryList = new LinkedList<>();
				for(Category cate :  cList) {
					Category setC = categoryService.getWithSeries(cate.getId(), null, err);
					if (setC != null) setCategoryList.add( setC );
				}
				List<String> category = html.getCategoryMenu(c, c2, setCategoryList);
				sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
				if (category.size() > 1) sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
				else  sidebar = StringUtils.replace(sidebar,"$$$category2$$$","");
				sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", "");
				temp = StringUtils.replace(temp,"$$$sidebar$$$",sidebar);
			}
			else temp = StringUtils.replace(temp,"$$$sidebar$$$","");
			temp = StringUtils.replace(temp,"$$$formbox$$$", "");
			temp = StringUtils.replace(temp,"$$$h1box$$$", "");

			String content = "";
			SeriesHtml sHtml = new SeriesHtml(getLocale(baseLang), messagesource, omlistService, faqRepo);
			s.setLink(seriesService.getLink(s.getId(), err));
			String tmp = sHtml.get(s, c, c2, request.getRequestURI(), lang, true, false);
			content += tmp.replace("<ul class=\"pro_service_bt\">", "<p>シリーズID:"+s.getModelNumber()+"</br>DB ID:"+s.getId()+"</p><ul class=\"pro_service_bt\">");

			String backUrl = request.getHeader("REFERER");
			if (backUrl != null && backUrl.isEmpty() == false && isSeriesOnly == false) {
				String mes = messagesource.getMessage("button.back.to.list", null,  getLocale(baseLang));
				content = StringUtils.replace(content, "$$$backUrl$$$", "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
			} else {
				content = StringUtils.replace(content, "$$$backUrl$$$", ""); // ボタン自体消す
			}

//		content = "<div class=\"p_block\">" + content + "</div>"; // シリーズ表示では不要。
			temp = StringUtils.replace(temp, "$$$content$$$", content);
			ret+=temp;
			ret+= SeriesHtml._seriesCadModal;
			if (isSeriesOnly == false) ret+=t.getFooter();
		}
		if (langObj.isVersion()) {
			// 変換処理
			//html.Init(getLocale(baseLang), messagesource);
			Template toT = templateService.getLangAndModelState(lang, ModelState.TEST, null, err); // previewなのでTEST固定
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		return ret;
	}


	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}


}
