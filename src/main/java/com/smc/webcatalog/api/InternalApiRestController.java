package com.smc.webcatalog.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smc.omlist.model.Omlist;
import com.smc.omlist.service.OmlistService;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesHtml;
import com.smc.webcatalog.model.SeriesLink;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.TemplateCategory;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

/**
 * HeartCoreから呼ばれるextension
 * @author tfujishima
 * @note 呼び出し例）@@@extension:webapi/sidemenu(lang=ja-jp)@@@
 * @see デザインの切り替えに関して、ローカルIPで呼ばれるので、HttpRequestでホスト取得はムリ。なので、Templateを見て判断。2026/4/13
 */
@RestController
@ResponseBody
@RequestMapping("/api")
@Slf4j
public class InternalApiRestController {

	@Autowired
	LangService langService;

	@Autowired
	CategoryService service;

	@Autowired
	SeriesService seriesService;

	@Autowired
	TemplateService templateService;

	@Autowired
	TemplateCategoryService templateCategoryService;

	@Autowired
	OmlistService omlistService;

	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml html;

	@Autowired
    HttpServletRequest req;


	/**
	 * サイドメニュー取得
	 * @param lang
	 * @return
	 * @note ModelState.TESTになっていたが、PROD専用に。
	 */
	@GetMapping({ "/heartcore/sidemenu/"})
	public String getSideMenu(@RequestParam(name = "lang", required = true) String lang) {
		String ret = "";
		ErrorObject err = new ErrorObject();

		try {
			if (lang != null && lang.isEmpty() == false) {
				if (lang.equals("ja") || lang.equals("jp") ) {
					lang = "ja-jp";
				} else if (lang.equals("en")) {
					lang="en-jp";
				} else if (lang.equals("zh")) {
					lang="zh-cn";
				} else if (lang.equals("zhtw")) {
					lang="zh-tw";
				}
			}
			
			Category c = service.getLang(lang, ModelState.PROD, CategoryType.CATALOG, true, err);
			TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(lang, ModelState.PROD, c.getId());
			if (tc.is2026()) {
				ret = tc.getSidebar();
				List<Category> cList = service.listAll(lang, ModelState.PROD, CategoryType.CATALOG, err);
				List<Category> setCategoryList = new LinkedList<>();
				for(Category cate :  cList) {
					Category setC = service.getWithSeries(cate.getId(), null, err);
					if (setC != null) setCategoryList.add( setC );
				}
				List<String> category = html.getCategoryMenu2026(lang, null, null, setCategoryList);
				ret = StringUtils.replace(ret,"$$$category$$$",category.get(0));
				if (category.size() > 1) ret = StringUtils.replace(ret,"$$$category2$$$",category.get(1));
				else ret = StringUtils.replace(ret,"$$$category2$$$","");
				ret = StringUtils.replace(ret,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない
			} else {
				ret = tc.getSidebar().replace("class=\"child open\"", "class=\"child\"").replace("class=\"side_mylist\"", "class=\"side_mylist hide\"");
				List<Category> cList = service.listAll(lang, ModelState.PROD, CategoryType.CATALOG, err);
				List<String> category = html.getCategoryMenu(lang, null, null, cList);
				ret = StringUtils.replace(ret,"$$$category$$$",category.get(0));
				ret = StringUtils.replace(ret,"$$$category2$$$",category.get(1));
				ret = StringUtils.replace(ret,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない
			}

		} catch (Exception e) {
			log.error("getSideMenu() ERROR! message="+e.getMessage());
		}

		return ret;
	}

	/**
	 *  OTHERのメニュー取得
	 * @param lang
	 * @param basepath
	 * @param index 大カテゴリを跨ぐ場合、同じIndexにする
	 * @param req request。該当の大カテゴリがあればopenを設定。
	 * @return
	 */
	@GetMapping({ "/heartcore/other/menu", "/heartcore/other/menu/"})
	public String getOtherMenu(@RequestParam(name = "lang", required = true) String lang,
			@RequestParam(name = "basepath", required = true) String basepath,
			@RequestParam(name = "index", required = true) String index,
			@RequestParam(name = "req", required = false) String req,
			@RequestParam(name = "test", required = false) String test) {

		String ret = "";
		ErrorObject err = new ErrorObject();
		ModelState state = ModelState.PROD;
		Boolean isActive = true;
		if (test != null && test.equals("1")) {
			state = ModelState.TEST;
			isActive = null;
		}
		
		Template t = templateService.getTemplateFromBean(lang, state);

		try {
			if (lang != null && lang.isEmpty() == false) {
				if (lang.equals("ja") || lang.equals("jp") ) {
					lang = "ja-jp";
				} else if (lang.equals("en")) {
					lang="en-jp";
				} else if (lang.equals("zh")) {
					lang="zh-cn";
				} else if (lang.equals("zhtw")) {
					lang="zh-tw";
				}
			}

			Category root = service.getRoot(lang, state, CategoryType.OTHER, err);

			List<Category> cList = service.listOtherAll(index, lang, state, isActive, err);
			if (t.is2026()) {
				ret = html.getOtherMenu2026(lang, basepath, req, root, cList);
			} else {
				ret = html.getOtherMenu(lang, basepath, req, root, cList);
			}
		} catch (Exception e) {
			log.error("getOtherMenu() ERROR! message="+e.getMessage());
		}

		return ret;
	}

	@GetMapping({"/heartcore/other/guide", "/heartcore/other/guide/"})
	public String getHeartCoreOtherGuide(
			@RequestParam(name = "slug", required = true) String slug,
			@RequestParam(name = "index", required = true) String index,
			@RequestParam(name = "lang", required = false) String la,
			@RequestParam(name = "test", required = false) String test) {

		String ret = "";
		ErrorObject err = new ErrorObject();
		List<String> slugArr = null;
		Category c1 = null;
		Category c2 = null;

		ModelState state = ModelState.PROD;
		Boolean isActive = true;
		if (test != null && test.equals("1")) {
			state = ModelState.TEST;
			isActive = null;
		}

		String lang = "ja-jp";
		if (la != null && la.isEmpty() == false) {
			if (la.equals("ja") || la.equals("jp") ) {
				//
			} else if (la.equals("en")) {
				lang="en-jp";
			} else if (la.equals("zh")) {
				lang="zh-cn";
			} else if (la.equals("zhtw")) {
				lang="zh-tw";
			}
		}
		Template t = templateService.getTemplateFromBean(lang, state);
		
		String[] arr = slug.split("/");
		int cnt = 0;
		for(String tmp : arr) {
			if (tmp != null && tmp.isEmpty() == false) {
				if (cnt == 0) slugArr = new LinkedList<String>();
				slugArr.add(tmp);
				cnt++;
			}
		}

		if (slugArr.size() > 0) {
			c1 = service.getFromSlug(slugArr.get(0), lang, state, CategoryType.OTHER, 1, true, err);
			if (c1 != null && slugArr.size() > 1) {
				c2 = service.getFromSlugSecond(slugArr.get(1), c1.getId(), lang, state, CategoryType.OTHER, true, err);
			}
		}
		Category ca = null;
		if (c2 != null) {
			ca = service.getWithSeries(c2.getId(), true, err);
		} else if (c1 != null) {
			// 大カテゴリのみ
			ca = service.getWithSeries(c1.getId(), true, err);
		}
		List<Series> sList = ca.getSeriesList();
		cnt = 0;
		int max = sList.size(); 
		for(Series s: sList) {
			if (s == null ) continue;
			if (s.getLang().equals(lang) == false) {
				lang = s.getLang();
			}
			String tmp = html.getFileFromHtml(lang + "/series/" + s.getModelNumber() + "/s.html");
			if (t.is2026()) {
				if (max - 1 > cnt) ret += "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>"; // 区切り線
			} else {
				// catpanを消す
				ret += tmp.replaceFirst("</h2>(.*)\\r\\n", "</h2>") // </h2>の後ろから改行まで。
				      .replaceFirst("<div class=\"result\">", "<div class=\"result\" data-id=\""+s.getModelNumber()+"\">"); // </h2>の後ろから改行まで。
			}
			cnt++;
		}
		ret = "<div class=\"p_block\">\r\n" + ret + "\r\n</div><!-- .p_block -->";
		ret += SeriesHtml._seriesCadModal ;

		return ret;
	}

	// ModelState.PROD active=true固定
	@GetMapping({"/heartcore/guide", "/heartcore/guide/"})
	public String getHeartCoreGuide(
			@RequestParam(name = "id", required = false) String[] ids,
			@RequestParam(name = "lang", required = false) String la) {
		String ret = "";
		ErrorObject err = new ErrorObject();

		String lang = "ja-jp";
		if (la != null && la.isEmpty() == false) {
			if (la.equals("ja") || la.equals("jp") ) {
				//
			} else if (la.equals("en")) {
				lang="en-jp";
			} else if (la.equals("zh")) {
				lang="zh-cn";
			} else if (la.equals("zhtw")) {
				lang="zh-tw";
			}
		}

		for(String id: ids) {
			if (id == null || id.isEmpty()) continue;
			String[] arr = null;
			if (id.indexOf(",") > 0) { // ,区切りもある。
				arr = id.split(",");
			} else {
				arr = new String[] {id};
			}
			if (arr != null) {
				int cnt = 0;
				int max = arr.length; 
				for(String sid : arr) {
					if (seriesService.isModelNumberExists(sid, ModelState.PROD, true, err) == false) continue;
					Series s = seriesService.getFromModelNumber(sid, ModelState.PROD, err);
					if (s == null) continue;
					if (s.getLang().equals(lang) == false) {
						lang = s.getLang();
					}
					String tmp = html.getFileFromHtml(lang + "/series/" + sid + "/s.html");
					if (tmp.indexOf("product-card-999") > -1) { // 2026新デザインのガイド
						tmp = StringUtils.replace(tmp, "id=\"product-card-999\"", "data-ml-entry=\"\" data-ml-product-key=\""+sid+"\" id=\"product-card-999\"");
						ret += tmp.replaceFirst("product-card-999", "product-card-"+(cnt+1));
						if (max - 1 > cnt) ret += "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>"; // 区切り線
					} else  {
						// catpanを消す
						String title = s.getName()+" "+s.getNumber();
						String link = AppConfig.ProdRelativeUrl+s.getLang()+"/seriesList/?type=GUIDE&id="+ s.getModelNumber();
						tmp = tmp.replaceFirst("</h2>(.*)\\r\\n", "</h2>") // </h2>の後ろから改行まで。
						      .replaceFirst("<div class=\"result\">", "<div class=\"result\" data-ml-entry=\"\" data-ml-product-key=\""+sid+"\">") // </h2>の後ろから改行まで。
						      .replaceFirst("<h2>", "\r\n<span class=\"chk_area\" data-ml-checkbox-slot></span>\r\n<h2 data-ml-title=\"primary\" data-product-url=\""+link+"\" data-product-title=\""+title+"\">");
						ret+= tmp;
					}
					cnt++;
				}
			}
		}
		if (ret.isEmpty() == false) {
			ret = "<div class=\"p_block\">\r\n" + ret + "\r\n</div><!-- .p_block -->";
			ret += SeriesHtml._seriesCadModal ;
		}

		return ret;
	}

	@GetMapping(value={"/guide/{id}/", "/guide/{id}/{show_page}", "/{lang}/guide/{id}/", "/{lang}/guide/{id}/{show_page}"}, produces="text/html;charset=UTF-8")
	public String getGuide(
			@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "id", required = false) String id,
			@PathVariable(name = "show_page", required = false) String show_page) {

		String ret = "";
		ErrorObject err = new ErrorObject();

		Lang langObj = langService.getLang(lang, err);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		Series s = seriesService.getFromModelNumber(id, ModelState.PROD, err);
		if (lang == null || lang.isEmpty() || lang.equals("null")) {
			lang = s.getLang();
		}
		if (s.isActive() && s.getLang().equals(baseLang)) {
			//SeriesHtml html = new SeriesHtml(getLocale(lang), messagesource);
			boolean isAdvantage = false;
			if (show_page != null) isAdvantage = show_page.equals("1");

			ret = html.getFileFromHtml(baseLang + "/series/" + id + "/guide.html");
			if (ret == null || ret.isEmpty()) {
				log.error("getFileFromHtml() return Empty! uri=" + baseLang + "/series/" + id + "/guide.html");
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "Empty! uri=" + baseLang + "/series/" + id + "/guide.html");
			}
			ret = ret.replace("$$$backUrl$$$", "");
			if (isAdvantage == false) {
				ret = ret.replace("<div id=\"detail\" ", "<div id=\"detail\" style=\"display:none;\" ");
			}
			ret += SeriesHtml._seriesCadModal ;
			//ret = html.get(s, 1, c, null, url, c.getLang(), isAdvantage);
			//ret = "<div class=\"p_block\">\r\n" + ret.replace("$$$backUrl$$$", "") + "\r\n</div><!-- .p_block -->";
		} else {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}

		return ret;
	}
	
	@GetMapping(value={ "/{lang}/guide/"}, produces="text/html;charset=UTF-8")
	public String getGuideList(@PathVariable(name = "lang", required = false) String lang,
			@RequestParam(name = "id", required = false) String[] ids) {
		String ret = "";
		ErrorObject err = new ErrorObject();
		boolean isAdvantage = false; // 複数の場合は特長無し、固定。

		if (lang.equals("heartcore")) {
			return getHeartCoreGuide(ids, lang);
		}
		Lang langObj = langService.getLang(lang, err);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		int cnt = 0;
		for(String id : ids) {
			Series s = seriesService.getFromModelNumber(id, ModelState.PROD, err);
			if (lang == null || lang.isEmpty() || lang.equals("null")) {
				lang = s.getLang();
			}
			if (s != null && s.isActive() && s.getLang().equals(baseLang)) {
				if (cnt == 0) {
					ret = html.getFileFromHtml(baseLang + "/series/" + id + "/guide.html");
					if (ret == null || ret.isEmpty()) {
						log.error("getFileFromHtml() return Empty! uri=" + baseLang + "/series/" + id + "/guide.html");
						throw new ResponseStatusException(
								  HttpStatus.NOT_FOUND, "Empty! uri=" + baseLang + "/series/" + id + "/guide.html");
					}
					ret = ret.replace("$$$backUrl$$$", "");
					if (isAdvantage == false) {
						ret = ret.replace("<div id=\"detail\" ", "<div id=\"detail\" style=\"display:none;\" ");
					}
					//ret = html.get(s, 1, c, null, url, c.getLang(), isAdvantage);
					//ret = "<div class=\"p_block\">\r\n" + ret.replace("$$$backUrl$$$", "") + "\r\n</div><!-- .p_block -->";
				} else {
					String tmp = html.getFileFromHtml(baseLang + "/series/" + id + "/guide.html");
					if (tmp != null && tmp.isEmpty() == false) {
						// <div class="result">内を取得し結合
						String[] arr = tmp.split("<div class=\"result\">");
						if (arr.length > 1) {
							String[] arr2 = arr[1].split("<!--result-->");
							if (arr2.length > 1) {
								String[] arr3 = ret.split("<!--result-->");
								ret = arr3[0] + "<div class=\"result\">" + arr2[0] + "<!--result-->" + arr3[1];
							}
						}

					}
				}
				ret += SeriesHtml._seriesCadModal ;
			} else {
				log.error("Lang is Bad or Empty! lang=" + lang);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "Lang is Empty!");
			}
			cnt++;
		}

		return ret;
	}

	// 新製品情報でシリーズIDからデジカタへリダイレクト
	@GetMapping(value={"/{lang}/catalog/{id}", "/{lang}/catalog/**"}, produces="text/html;charset=UTF-8")
	public ResponseEntity  getCatalog(
			@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "id", required = false) String id,
			HttpServletRequest request,
			HttpServletResponse response) {

		ErrorObject err = new ErrorObject();
		if (id == null) {
			String tmp = request.getRequestURI();
			if (tmp != null) {
				int s = tmp.indexOf("/catalog/");
				id = tmp.substring(s+"/catalog/".length());
			}
		}

		Lang langObj = langService.getLang(lang, err);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		Series s = seriesService.getFromModelNumber(id, ModelState.PROD, err);
		if (s.isActive() && s.getLang().equals(baseLang)) {

			List<SeriesLink> list = seriesService.getLink(s.getId(), err);
			boolean isFound = false;
			String str = "";
			for(SeriesLink link : list) {
				if (link.getLinkMaster().getIconClass().equals("cat_pdf")) {
					isFound = true;
					String url = link.getUrl();

					if (lang.equals("ja-jp") || baseLang.equals("en-jp")) { // デジタルカタログは日本語、英語のみ
						String href = url;
						int start = href.indexOf("/data/");
						if (start > -1) {
							href = href.substring(0,start) + "/index.html";
							str = href;
							break;
						} else {
							start = href.indexOf("/6-");
							if (start > -1) {
								href = href.replace("/index.pdf", "/index.html");
								href = href.replace("/pdf/catalog/", "/catalog/");
								str = href;
								break;
							}
						}
					}
					String pdf = url;
					if (pdf.indexOf("/index.pdf") > -1 || pdf.indexOf("/index.html") > -1) {
						pdf = pdf.replace("/index.pdf", "");
						pdf = pdf.replace("/index.html", "");
						pdf = pdf.replace("/pdf/catalog/", "/catalog/");
						String[] arr = pdf.split("/");
						String tmp = arr[arr.length-1];
						pdf = "";
						for(String tmp2 : arr) {
							if (tmp2.isEmpty() == false) pdf += "/" + tmp2;
						}
						pdf += "/data/" + tmp+".pdf";
					}
					str = pdf;
					break;
				}
			}
			if (isFound == false) {
				log.error("getCatalog() ERROR! file not found. id=" + id + " lang="+lang);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			} else {
				try {
					response.sendRedirect(str);
				}catch(Exception e) {
					log.error("ERROR! file not found. e= "+ e.getMessage());
					return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
				}
			}
		} else {
			log.error("getCatalog() ERROR! file not found. id=" + id + " lang="+lang);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		return null;
	}

	// オーダーメイドリスト
	@GetMapping(value={"/omlist/{lang}/{cat}", "/omlist/{lang}/{cat}/"}, produces="text/html;charset=UTF-8")
	public String getOmlist(
			@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "cat", required = true) String cat) {
		ErrorObject err = new ErrorObject();
		String ret = "";
		log.debug("=== start omlist. ===");
		
		try {
			String div = "簡易特注";
			if (lang.equals("en-jp")) {
				if (cat.equals("OM")) {
					div = "Made to Order Common Specifications";
				} else {
					div = "Simple Specials";
				}
			} else {
				if (cat.equals("OM")) {
					div = "オーダーメイド";
				}
			}
			// TODO 中国語

			List<Omlist> list = omlistService.searchKeyword(null, null, div, null, lang);
			if (list != null && list.size() > 0) {
				Template t = templateService.getTemplateFromBean(lang, ModelState.PROD);
				if (t.is2026()) {
					ret = omlistService.getTableHtml2026(list, lang);
				} else {
					ret = omlistService.getTableHtml(list, lang);
				}
			} else {
				ret = "<div>表示対象がありません。</div>";
			}

		} catch (Exception e) {
			log.error("Error! Thread2 running. Exception="+ e.getMessage());
			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);

		    pw.append("+++Start printing trace:\n");
		    e.printStackTrace(pw);
		    pw.append("---Finish printing trace");
		    System.out.println(sw.toString());
		}
		log.debug("=== end omlist. ===");
		return ret;
	}

}
