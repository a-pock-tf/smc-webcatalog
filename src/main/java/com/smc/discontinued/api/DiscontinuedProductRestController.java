package com.smc.discontinued.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.net.URLCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smc.discontinued.config.DiscontinuedConfig;
import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.discontinued.model.DiscontinuedTemplate;
import com.smc.discontinued.service.DiscontinuedCategoryServiceImpl;
import com.smc.discontinued.service.DiscontinuedSeriesServiceImpl;
import com.smc.discontinued.service.DiscontinuedTemplateServiceImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.TemplateServiceImpl;
import com.smc.webcatalog.util.LibHtml;

import lombok.extern.slf4j.Slf4j;

@RestController
@ResponseBody
@RequestMapping({"/discontinued/", "/discon_closed8811/"})
@Slf4j
public class DiscontinuedProductRestController {

	@Autowired
	DiscontinuedSeriesServiceImpl seriesService;

	@Autowired
	DiscontinuedCategoryServiceImpl categoryService;

	@Autowired
	DiscontinuedTemplateServiceImpl templateService;

	@Autowired
	TemplateServiceImpl tService;

	@Autowired
	LangService langService;

	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml html;

	// カテゴリ一覧
	@GetMapping(value={"/{lang}/", "/{lang}"}, produces="text/html;charset=UTF-8")
	public String getCategory(@PathVariable(name = "lang", required = true) String lang,
			@RequestParam(name = "kw", required = false) String kw,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = "";
		String context = request.getContextPath();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String changeUrl = "";
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		List<DiscontinuedCategory> list = categoryService.listAllActive(baseLang, DiscontinuedModelState.PROD, err);
		if (temp != null) {
			ret = getHtml(temp, list, context, changeUrl, err);
		}

		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
			Template toT = tService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), true);
		}

		return ret;
	}
	// プレビュー　カテゴリ-> シリーズ一覧
	@GetMapping({"/preview/{lang}/{slug}", "/preview/{lang}/category/{categoryId}"})
	public String getCategorySeriesList(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = "";
		String context = request.getContextPath();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String changeUrl = "";
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		if (slug != null && slug.isEmpty() == false) {
			List<DiscontinuedCategory> list = new LinkedList<DiscontinuedCategory>();
			list.add(categoryService.getSlug(slug, baseLang, DiscontinuedModelState.TEST, err));
			if (temp != null) {
				ret = getHtml(temp, list, context, changeUrl, err);
			}
		} else if (categoryId != null && categoryId.isEmpty() == false) {
			List<DiscontinuedCategory> list = new LinkedList<DiscontinuedCategory>();
			list.add(categoryService.get(categoryId, err));
			if (temp != null) {
				ret = getHtml(temp, list, context, changeUrl, err);
			}
		}

		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
//			Template toT = tService.getTemplateByTemplates(lang, m);
			Template toT = tService.getTemplateFromBean("ja-jp", m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), isActive);

		}

		return ret;
	}

	// カテゴリ-> シリーズ一覧
	@GetMapping(value={"/series/{lang}/", "/series/{lang}/{slug}", "/series/{lang}/category/{categoryId}", "/series/{lang}/old/{oldId}/"}, produces="text/html;charset=UTF-8")
	public String getSeriesList(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@PathVariable(name = "oldId", required = false) String oldId,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = "";
		String context = request.getContextPath();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String changeUrl = "";
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		if (slug != null && slug.isEmpty() == false) {
			List<DiscontinuedCategory> list = new LinkedList<DiscontinuedCategory>();
			list.add(categoryService.getSlug(slug, baseLang, DiscontinuedModelState.PROD, err));
			if (temp != null) {
				ret = getHtml(temp, list, context, changeUrl, err);
			}
		} else if (categoryId != null && categoryId.isEmpty() == false) {
			List<DiscontinuedCategory> list = new LinkedList<DiscontinuedCategory>();
			list.add(categoryService.get(categoryId, err));
			if (temp != null) {
				ret = getHtml(temp, list, context, changeUrl, err);
			}
		} else if (oldId != null && oldId.isEmpty() == false) {
			List<DiscontinuedCategory> list = new LinkedList<DiscontinuedCategory>();
			list.add(categoryService.getOldId(oldId, baseLang, DiscontinuedModelState.PROD, err));
			if (temp != null) {
				ret = getHtml(temp, list, context, changeUrl, err);
			}
		}

		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
			Template toT = tService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), isActive);
		}

		return ret;
	}

	@GetMapping(value={"/detail/{lang}/{slug}/{seriesId}", "/detail/{lang}/category/{categoryId}/{seriesId}",
		"/detail/{lang}/old/{oldId}/{seriesId}"}, produces="text/html;charset=UTF-8")
	public String getDetail(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@PathVariable(name = "seriesId", required = false) String seriesId,
			@PathVariable(name = "oldId", required = false) String oldId,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = "";
		String context = request.getContextPath();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String changeUrl = "";
		boolean isTest = false; // 新旧比較詳細のPDFを出力
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
			isTest = true;
		} else {
			String url = request.getRequestURL().toString();
			isTest = (url.indexOf("/discon_closed8811/") > 0 || url.indexOf("test.smcworld.com") > 0 || url.indexOf("ap1.smcworld.com") > 0 || url.indexOf("ap2.smcworld.com") > 0 || url.indexOf("localhost") > 0);
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		DiscontinuedSeries s = seriesService.getSeriesId(seriesId, DiscontinuedModelState.PROD, err);
		if (slug != null && slug.isEmpty() == false && slug.equals("null") == false) {
			DiscontinuedCategory c = categoryService.getSlug(slug, baseLang, DiscontinuedModelState.PROD, err);
			if (temp != null && c != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			} else if (c == null) {
				log.error("Category is not found! lang=" + baseLang + " slug = "+slug);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "Lang is Empty!");
			}
		} else if (categoryId != null && categoryId.isEmpty() == false) {

			DiscontinuedCategory c = categoryService.get(categoryId, err);
			if (temp != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			}
		} else if (oldId != null && oldId.isEmpty() == false) {
			DiscontinuedCategory c = categoryService.getOldId(oldId, baseLang, DiscontinuedModelState.PROD, err);
			if (temp != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			}
		}

		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
			Template toT = tService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), true);
		}
		return ret;
	}

	@GetMapping({"/preview/detail/{lang}/{slug}/{seriesId}", "/preview/detail/{lang}/category/{categoryId}/{seriesId}", "/preview/detail/{lang}/old/{oldId}/{seriesId}"})
	public String getPreviewDetail(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "categoryId", required = false) String categoryId,
			@PathVariable(name = "seriesId", required = false) String seriesId,
			@PathVariable(name = "oldId", required = false) String oldId,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = "";
		String context = request.getContextPath();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String changeUrl = "";
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		DiscontinuedSeries s = seriesService.get(seriesId, err);
		String url = request.getRequestURL().toString();
		boolean isTest = (url.indexOf("test.smcworld.com") > 0 || url.indexOf("ap1.smcworld.com") > 0 || url.indexOf("ap2.smcworld.com") > 0 || url.indexOf("localhost") > 0);
		if (slug != null && slug.isEmpty() == false) {
			DiscontinuedCategory c = categoryService.getSlug(slug, baseLang, DiscontinuedModelState.TEST, err);
			if (temp != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			}
		} else if (categoryId != null && categoryId.isEmpty() == false && categoryId.equals("null") == false) {

			DiscontinuedCategory c = categoryService.get(categoryId, err);
			if (temp != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			}
		} else if (oldId != null && oldId.isEmpty() == false) {
			DiscontinuedCategory c = categoryService.getOldId(oldId, baseLang, DiscontinuedModelState.TEST, err);
			if (temp != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			}
		} else {
			DiscontinuedCategory c = categoryService.get(s.getCategoryId(), err);
			if (temp != null) {
				ret = getDetailHtml(temp, c, s, isTest, context, changeUrl, err);
			}
		}

		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
			Template toT = tService.getLangAndModelState(lang, m, isActive, err);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), true);
		}
		return ret;
	}

	@GetMapping(value={"/searchIndex/{lang}/{index}", "/searchIndex/{lang}/{index}/"}, produces="text/html;charset=UTF-8")
	public String searchIndex(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "index", required = false) String index,
			@RequestParam(name = "categoryId", required = false) String categoryId,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = null;
		String context = request.getContextPath();

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String changeUrl = "";
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		List<DiscontinuedSeries> list = null;
		if (index != null && index.isEmpty() == false) {
			list = seriesService.indexSearch(index, baseLang, DiscontinuedModelState.PROD, true,  err);
			if (temp != null ) {
				if (list == null) list = new LinkedList<DiscontinuedSeries>(); // エラーページを返すためにnewしておく。
				ret = getSearchIndexHtml(baseLang, temp, index, list, context, changeUrl, err);
			} else {
				log.error("invalid index. index=" + index);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "invalid index!");
			}
		}
		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
			Template toT = tService.getLangAndModelState(lang, m, isActive, err);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), true);
		}
		return ret;
	}

	@GetMapping({"/searchKeyword/{lang}","/searchKeyword/{lang}/"})
	public String searchKeyword(@PathVariable(name = "lang", required = false) String lang,
			@RequestParam(name = "kw", required = false) String kw,
			@RequestParam(name = "categoryId", required = false) String categoryId,
			HttpServletRequest request) {
		ErrorObject err = new ErrorObject();
		String ret = null;
		String context = request.getContextPath();

		String changeUrl = "";
		if (request.getRequestURI().indexOf("discon_closed8811") > 0) {
			changeUrl = "discon_closed8811";
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

		DiscontinuedTemplate temp = templateService.getLang(baseLang, err);
		List<DiscontinuedSeries> list = null;
		if (kw != null && kw.isEmpty() == false) {
			list = seriesService.search(kw, baseLang, DiscontinuedModelState.PROD, true,  err);
			if (temp != null && list != null) {
				ret = getSearchResultHtml(baseLang, temp, list, context, changeUrl,  err);
				ret = ret.replace("name=\"kw\" value=\"\" id=\"k\"", "name=\"kw\" value=\""+kw+"\" id=\"k\"");
				ret = ret.replace("</head>", "<script>\r\n" +
						"$(function(){\r\n" +
						"highlightKw();\r\n" +
						"});\r\n" +
						"</script></head>");
			}
		}
		if (langObj.isVersion()) {
			String reqUrl = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(reqUrl);
			
			ModelState m = ModelState.PROD;
			if (isTestSite) m = ModelState.TEST;
			Boolean isActive = true;
			if (isTestSite) isActive = null; 
			
			// 変換処理
			Template toT = tService.getLangAndModelState(lang, m, isActive, err);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), true);

		}
		return ret;
	}
	// =================== private ===================
	private SimpleDateFormat formatJa = new SimpleDateFormat("yyyy年M月");
	private SimpleDateFormat formatEn = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH);
	private SimpleDateFormat formatZh = new SimpleDateFormat("yyyy/MM");
	private SimpleDateFormat baseSdf = new SimpleDateFormat("yyyy/MM/dd");
	private SimpleDateFormat baseSdf2 = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
	private String getSearchIndexHtml(String baseLang, DiscontinuedTemplate temp, String ind, List<DiscontinuedSeries> list, String context, String changeUrl, ErrorObject err) {
		String ret = null;
		String title = messagesource.getMessage("g.discontinued", null, getLocale(temp.getLang()));
		if (list != null && list.size() == 1) {
			title = list.get(0).getName();
		}
		if (temp != null) {
			ret = "";
			ret = temp.getTemplate();
			ret = ret.replace("$$$catpan$$$", temp.getCatpan().replace("$$$title$$$", title));
			ret = ret.replace("$$$h1box$$$", temp.getH1box().replace("$$$title$$$", title));
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				ret = ret.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			ret = ret.replace("$$$formbox$$$", temp.getFormbox());
			String formBox = temp.getFormbox();
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				formBox = formBox.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			ret = ret.replace("$$$sidebar$$$", getSidebar(temp.getLang(), context, changeUrl, err));
			// コンテンツ作成
			String content = "";
			if (list != null) {
				if (list.size() == 0 ) {
					content = "<br><p>" + messagesource.getMessage("msg.search.empty", null, getLocale(baseLang)) + "</p>";
				} else {
					DiscontinuedCategory c = null;
					content +=  DiscontinuedConfig.contentStart.replace("$$$title$$$", ind)
							.replace("$$$endtitle$$$", messagesource.getMessage("discon.title.products", null, getLocale(temp.getLang())))
							.replace("$$$replacetitle$$$", messagesource.getMessage("discon.title.replace.products", null, getLocale(temp.getLang())))
							.replace("$$$colname$$$", messagesource.getMessage("discon.discontinued.name", null, getLocale(temp.getLang())))
							.replace("$$$colseries$$$", messagesource.getMessage("discon.discontinued.series", null, getLocale(temp.getLang())))
							.replace("$$$coldate$$$", messagesource.getMessage("discon.time.of.discontinuation", null, getLocale(temp.getLang())))
							.replace("$$$colcompair$$$", messagesource.getMessage("discon.discontinued.compair", null, getLocale(temp.getLang())));
					for(DiscontinuedSeries s : list) {
						if (c == null || c.getId().equals( s.getCategoryId()) == false ) {
							c = categoryService.get(s.getCategoryId(), err);
						}
						content += "<tr>\r\n" +
									"<td class=\"tdl\">"+s.getName()+"</td>\r\n" +
									"<td>"+s.getSeries()+"</td>\r\n";
						try {
							if (temp.getLang().equals("ja-jp")) {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatJa.format(d)+"</td>\r\n" ;
							} else if (temp.getLang().equals("en-jp")) {
									Date d = baseSdf.parse(s.getDate());
									content += 	"<td>"+formatEn.format(d)+"</td>\r\n" ;
							} else if (temp.getLang().equals("zh-cn")) {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatZh.format(d)+"</td>\r\n" ;
							}
						}catch (Exception e) {
							try {
								if (temp.getLang().equals("ja-jp")) {
									Date d = baseSdf2.parse(s.getDate());
									content += 	"<td>"+formatJa.format(d)+"</td>\r\n" ;
								} else if (temp.getLang().equals("en-jp")) {
									Date d = baseSdf2.parse(s.getDate());
									content += 	"<td>"+formatEn.format(d)+"</td>\r\n" ;
								} else {
									Date d = baseSdf2.parse(s.getDate());
									content += 	"<td>"+formatZh.format(d)+"</td>\r\n" ;
								}
							} catch (Exception e2) {
								// 日付はカラの場合もあるので、debug
								log.debug("ERROR! getSearchIndexHtml() Date.parse() date="+s.getDate());
							}
						}

						content += "<td>"+s.getNewSeries()+"</td>\r\n" +
								"<td class=\"tdc\" width=\"80px\"><a href=\"";
						String tmp = "";
						if (c.getSlug() == null || c.getSlug().isEmpty()) {
							tmp = context+"/discontinued/detail/"+temp.getLang()+"/category/"+c.getId()+"/"+s.getSeriesId();
						}
						else {
							tmp = context+"/discontinued/detail/"+temp.getLang()+"/"+c.getSlug()+"/"+s.getSeriesId();
						};
						if (changeUrl != null && changeUrl.isEmpty() == false) {
							tmp = tmp.replace("/discontinued/", "/"+changeUrl+"/");
						}
						content+= tmp;
						content+= "\" class=\"actlink\">"+messagesource.getMessage("msg.head.table.detail", null, getLocale(temp.getLang()))+"</a></td>\r\n" +
						"</tr>";
					}
				}
				content+= DiscontinuedConfig.contentEnd;
				String search = DiscontinuedConfig.contentSearch;
				if (changeUrl != null && changeUrl.isEmpty() == false) {
					search = search.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
				}
				content+= search;
				ret = ret.replace("$$$content$$$", content);
			}
		}
		return ret;
	}

	private String getSearchResultHtml(String baseLang, DiscontinuedTemplate temp, List<DiscontinuedSeries> list, String context, String changeUrl, ErrorObject err) {
		String ret = null;
		String title = messagesource.getMessage("g.discontinued", null, getLocale(temp.getLang()));
		if (list != null && list.size() == 1) {
			title = list.get(0).getName();
		}
		if (temp != null) {
			ret = "";
			ret = temp.getTemplate();
			ret = ret.replace("$$$catpan$$$", temp.getCatpan().replace("$$$title$$$", title));
			ret = ret.replace("$$$h1box$$$", temp.getH1box().replace("$$$title$$$", title));
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				ret = ret.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			String formBox = temp.getFormbox();
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				formBox = formBox.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			ret = ret.replace("$$$formbox$$$", formBox);
			ret = ret.replace("$$$sidebar$$$", getSidebar(temp.getLang(), context, changeUrl, err));
			// コンテンツ作成
			String content = "";
			if (list != null) {
				if (list.size() == 0 ) {
					content = "<br><p>" + messagesource.getMessage("msg.search.empty", null, getLocale(baseLang)) + "</p>";
				} else {
					DiscontinuedCategory c = null;
					for(DiscontinuedSeries s : list) {
						if (c == null || c.getId().equals( s.getCategoryId()) == false ) {
							if (c != null) content+= DiscontinuedConfig.contentEnd;
							c = categoryService.get(s.getCategoryId(), err);
							content +=  DiscontinuedConfig.contentStart.replace("$$$title$$$", c.getName())
									.replace("$$$endtitle$$$", messagesource.getMessage("discon.title.products", null, getLocale(temp.getLang())))
									.replace("$$$replacetitle$$$", messagesource.getMessage("discon.title.replace.products", null, getLocale(temp.getLang())))
									.replace("$$$colname$$$", messagesource.getMessage("discon.discontinued.name", null, getLocale(temp.getLang())))
									.replace("$$$colseries$$$", messagesource.getMessage("discon.discontinued.series", null, getLocale(temp.getLang())))
									.replace("$$$coldate$$$", messagesource.getMessage("discon.time.of.discontinuation", null, getLocale(temp.getLang())))
									.replace("$$$colcompair$$$", messagesource.getMessage("discon.discontinued.compair", null, getLocale(temp.getLang())));
						}
						content += "<tr>\r\n" +
									"<td class=\"tdl\">"+s.getName()+"</td>\r\n" +
									"<td>"+s.getSeries()+"</td>\r\n";
						try {
							if (temp.getLang().equals("ja-jp")) {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatJa.format(d)+"</td>\r\n" ;
							} else if (temp.getLang().equals("en-jp")) {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatEn.format(d)+"</td>\r\n" ;
							} else {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatZh.format(d)+"</td>\r\n" ;
							}
						}catch (Exception e) {
							try {
								if (temp.getLang().equals("ja-jp")) {
									Date d = baseSdf2.parse(s.getDate());
									content += 	"<td>"+formatJa.format(d)+"</td>\r\n" ;
								} else if (temp.getLang().equals("en-jp")) {
									Date d = baseSdf2.parse(s.getDate());
									content += 	"<td>"+formatEn.format(d)+"</td>\r\n" ;
								} else {
									Date d = baseSdf2.parse(s.getDate());
									content += 	"<td>"+formatZh.format(d)+"</td>\r\n" ;
								}
							} catch (Exception e2) {
								// 日付はカラの場合もあるので、debug
								log.debug("ERROR! getSearchIndexHtml() Date.parse() date="+s.getDate());
							}
						}
						content += "<td>"+s.getNewSeries()+"</td>\r\n" +
								"<td class=\"tdc\" width=\"80px\"><a href=\"";
						String tmp = "";
						if (c.getSlug() == null || c.getSlug().isEmpty()) {
							tmp = context+"/discontinued/detail/"+temp.getLang()+"/category/"+c.getId()+"/"+s.getSeriesId();
						}
						else {
							tmp = context+"/discontinued/detail/"+temp.getLang()+"/"+c.getSlug()+"/"+s.getSeriesId();
						};
						if (changeUrl != null && changeUrl.isEmpty() == false) {
							tmp = tmp.replace("/discontinued/", "/"+changeUrl+"/");
						}
						content+= tmp;
						content+= "\" class=\"actlink\">"+messagesource.getMessage("msg.head.table.detail", null, getLocale(temp.getLang()))+"</a></td>\r\n" +
						"</tr>";
					}
				}
				content+= DiscontinuedConfig.contentEnd;
				String search = DiscontinuedConfig.contentSearch;
				if (changeUrl != null && changeUrl.isEmpty() == false) {
					search = search.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
				}
				content+= search;
				ret = ret.replace("$$$content$$$", content);
			}
		}
		return ret;
	}
	private String getHtml(DiscontinuedTemplate temp, List<DiscontinuedCategory> list, String context, String changeUrl, ErrorObject err) {
		String ret = null;
		String title = messagesource.getMessage("g.discontinued", null, getLocale(temp.getLang()));
		if (list != null && list.size() == 1) {
			title = list.get(0).getName();
		}
		if (temp != null) {
			ret = "";
			ret = temp.getTemplate();
			ret = ret.replace("$$$catpan$$$", temp.getCatpan().replace("$$$title$$$", title));
			ret = ret.replace("$$$h1box$$$", temp.getH1box().replace("$$$title$$$", title));
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				ret = ret.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			String formBox = temp.getFormbox();
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				formBox = formBox.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			ret = ret.replace("$$$formbox$$$", formBox);
			ret = ret.replace("$$$sidebar$$$", getSidebar(temp.getLang(), context, changeUrl, err));
			// コンテンツ作成
			if (list != null) {
				String content = "";
				for(DiscontinuedCategory c : list) {
					content +=  DiscontinuedConfig.contentStart.replace("$$$title$$$", c.getName())
							.replace("$$$endtitle$$$", messagesource.getMessage("discon.title.products", null, getLocale(temp.getLang())))
							.replace("$$$replacetitle$$$", messagesource.getMessage("discon.title.replace.products", null, getLocale(temp.getLang())))
							.replace("$$$colname$$$", messagesource.getMessage("discon.discontinued.name", null, getLocale(temp.getLang())))
							.replace("$$$colseries$$$", messagesource.getMessage("discon.discontinued.series", null, getLocale(temp.getLang())))
							.replace("$$$coldate$$$", messagesource.getMessage("discon.time.of.discontinuation", null, getLocale(temp.getLang())))
							.replace("$$$colcompair$$$", messagesource.getMessage("discon.discontinued.compair", null, getLocale(temp.getLang())));
					List<DiscontinuedSeries> sList = seriesService.listCategory(c.getId(), c.getState(), true, err);
					for(DiscontinuedSeries s : sList) {
						content += "<tr>\r\n" +
								"<td class=\"tdl\">"+s.getName()+"</td>\r\n" +
								"<td>"+s.getSeries()+"</td>\r\n";
						try {
							if (temp.getLang().equals("ja-jp")) {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatJa.format(d)+"</td>\r\n" ;
							} else if (temp.getLang().equals("en-jp")) {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatEn.format(d)+"</td>\r\n" ;
							} else {
								Date d = baseSdf.parse(s.getDate());
								content += 	"<td>"+formatZh.format(d)+"</td>\r\n" ;
							}
						} catch (Exception e) {
							content += "<td>"+s.getDate()+"</td>\r\n" ;
						}
					content += "<td>"+s.getNewSeries()+"</td>\r\n" +
					"<td class=\"tdc\" width=\"80px\"><a href=\"";

					String tmp = "";
					if (c.getSlug() == null || c.getSlug().isEmpty()) {
						tmp = context+"/discontinued/"+"detail/"+temp.getLang()+"/category/"+c.getId()+"/"+s.getSeriesId();
					}
					else {
						tmp = context+"/discontinued/"+"detail/"+temp.getLang()+"/"+c.getSlug()+"/"+s.getSeriesId();
					};
					if (changeUrl != null && changeUrl.isEmpty() == false) {
						tmp = tmp.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
					}
					content+= tmp;

					content+= "\" class=\"actlink\">"+messagesource.getMessage("msg.head.table.detail", null, getLocale(temp.getLang()))+"</a></td>\r\n" +
					"</tr>";
					}
					content+= DiscontinuedConfig.contentEnd;
				}
				String search = DiscontinuedConfig.contentSearch;
				if (changeUrl != null && changeUrl.isEmpty() == false) {
					search = search.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
				}
				content+= search;
				ret = ret.replace("$$$content$$$", content);
			}
		}
		return ret;
	}
	URLCodec codec = new URLCodec("UTF-8");
	private String getDetailHtml(DiscontinuedTemplate temp,DiscontinuedCategory c, DiscontinuedSeries s, boolean isTest, String context, String changeUrl, ErrorObject err) {
		String ret = null;
		String title = s.getSeriesName();
		if (temp != null) {
			ret = "";
			ret = temp.getTemplate();
			ret = ret.replace("$$$catpan$$$", temp.getCatpan().replace("$$$title$$$", title));
			ret = ret.replace("$$$h1box$$$", temp.getH1box().replace("$$$title$$$", title));
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				ret = ret.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			String formBox = temp.getFormbox();
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				formBox = formBox.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			ret = ret.replace("$$$formbox$$$", formBox);
			ret = ret.replace("$$$sidebar$$$", getSidebar(temp.getLang(), context, changeUrl, err));
			// コンテンツ作成
			String content = "";
			{
				content = DiscontinuedConfig.detailStart.replace("$$$title$$$", c.getName());
				content = content.replace("$$$name$$$", s.getName());
				content = content.replace("$$$series$$$", s.getSeriesName());
				content+="<tr>\r\n" +
						"<th colspan=\"3\" class=\"ul\">"+messagesource.getMessage("discon.discontinued.products", null, getLocale(temp.getLang()))+"</th>\r\n" +
						"<th class=\"ul\">"+messagesource.getMessage("discon.time.of.discontinuation", null, getLocale(temp.getLang()))+"</th>\r\n" +
						"<th colspan=\"3\" class=\"ul last\">"+messagesource.getMessage("discon.replacement.products", null, getLocale(temp.getLang()))+"</th>\r\n" +
						"</tr>\r\n";
				{
					int ln = 1;
					List<String[]> list = s.getDetailList();
					for(String[] arr : list) {
						try {

							content += "<tr>\r\n";
							if (ln == 1) {
								content +=	"<td class=\"tdl\" rowspan=\""+list.size()+"\">";
								if (s.getImage() != null && s.getImage().isEmpty() == false) content += "<img src=\""+s.getImage()+"\"/>";
								else content+="-&nbsp;";

								content+="</td>\r\n" +
									"<td>"+arr[0]+"</td>\r\n";
								// 生産終了のカタログを行内に表示。// 2023/03/13
								// １番右に個別の終了カタログPDF列追加。 // 2023/03/18
								content+="<td>";
								if (arr.length > 4 && arr[4] != null && arr[4].isEmpty() == false && arr[4].equals("null") == false ) {
									content += "<p class=\"dls\"><a href=\""+arr[4]+"\" target=\"_blank\">"+messagesource.getMessage("discon.button.catalog", null, getLocale(temp.getLang()))+"</a></p>";
								}
								else if (s.getCatalogLink() != null && s.getCatalogLink().isEmpty() == false) {
									content += "<p class=\"dls\"><a href=\""+s.getCatalogLink()+"\" target=\"_blank\">"+messagesource.getMessage("discon.button.catalog", null, getLocale(temp.getLang()))+"</a></p>";
								} else {
									content+="-&nbsp;";
								}
								content+="</td>";

								try {
									if (arr[1].isEmpty() == false) {
										Date d = baseSdf.parse(arr[1]);
										if (temp.getLang().equals("ja-jp")) {
											content += 	"<td>"+formatJa.format(d)+"</td>\r\n" ;
										} else if (temp.getLang().equals("en-jp")) {
											content += 	"<td>"+formatEn.format(d)+"</td>\r\n" ;
										} else {
											content += 	"<td>"+formatZh.format(d)+"</td>\r\n" ;
										}
									} else {
										content += "<td></td>\r\n" ;
									}
								} catch (Exception e) {
									content += "<td>"+s.getDate()+"</td>\r\n" ;
								}
								content += "<td class=\"tdl\" rowspan=\""+list.size()+"\">";
								if (s.getNewImage() != null && s.getNewImage().isEmpty() == false) content+= "<img src=\""+s.getNewImage()+"\"/>";
								else content+="-&nbsp;";
								content += "</td>\r\n" +
									"<td>"+arr[2]+"</td>\r\n" +
									"<td class=\"tdc\" width=\"80px\">";
							} else {
								if (arr[1].isEmpty() == false) {
									Date d = baseSdf.parse(arr[1]);
									content += "<td style=\"border-left: 1px solid #ccc;\">"+arr[0]+"</td>\r\n" ;

									// 生産終了のカタログを行内に表示。// 2023/03/13
									content+="<td>";
									if (arr.length > 4 && arr[4] != null && arr[4].isEmpty() == false && arr[4].equals("null") == false ) {
										content += "<p class=\"dls\"><a href=\""+arr[4]+"\" target=\"_blank\">"+messagesource.getMessage("discon.button.catalog", null, getLocale(temp.getLang()))+"</a></p>";
									}
									else if (s.getCatalogLink() != null && s.getCatalogLink().isEmpty() == false) {
										content+="<p class=\"dls\"><a target=\"_blank\" href=\""+s.getCatalogLink()+"\">"+messagesource.getMessage("discon.button.catalog", null, getLocale(temp.getLang()))+"</a></p>";
									} else {
										content+="-&nbsp;";
									}
									content+="</td>";

									if (temp.getLang().equals("ja-jp")) content += "<td>"+formatJa.format(d)+"</td>\r\n" ;
									else if (temp.getLang().equals("en-jp")) content += "<td>"+formatEn.format(d)+"</td>\r\n" ;
									else  content += "<td>"+formatZh.format(d)+"</td>\r\n" ;
									content += "<td>"+arr[2]+"</td>\r\n" +
									           "<td class=\"tdc\" width=\"80px\">";
								} else {
									content += "<td></td>\r\n";
								}
							}
							if (arr[3] == null || arr[3].isEmpty() || arr[3].equals("null") ) {
								if (s.getNewCatalogLink() != null && s.getNewCatalogLink().isEmpty() == false) {
									content+="<a target=\"_blank\" href=\""+s.getNewCatalogLink()+"\" class=\"actlink\">"+messagesource.getMessage("discon.discontinued.detail", null, getLocale(temp.getLang()))+"</a>";
								} else {
									content+="-&nbsp;";
								}
							}
							else {
								String url = DiscontinuedConfig.webCatalogUrl;
								String[] arr3 = arr[3].split("-");
								if (arr3 != null && arr3[arr3.length-1].equals("E") && temp.getLang().equals("en-jp") == false) {
									url = url.replace("ja/", "en/");
									url = url.replace("ja-jp/", "en-jp/");
								} else if (arr3 != null && arr3[arr3.length-1].equals("ZH") && temp.getLang().equals("zh-cn") == false) {
									url = url.replace("ja/", "zh/");
									url = url.replace("ja-jp/", "zh-cn/");
								} else if (arr3 != null && arr3[arr3.length-1].equals("ZHTW") && temp.getLang().equals("zh-tw") == false) {
									url = url.replace("ja/", "zhtw/");
									url = url.replace("ja-jp/", "zh-tw/");
								} else if (arr[3].equals("EX260-EN") && temp.getLang().equals("en-jp") == false) {
									url = url.replace("ja/", "en/");
									url = url.replace("ja-jp/", "en-jp/");
								} else {
									if (temp.getLang().equals("en-jp")) {
										url = url.replace("ja/", "en/");
										url = url.replace("ja-jp/", "en-jp/");
									}
									else if (temp.getLang().equals("zh-cn")) {
										url = url.replace("ja/", "zh/");
										url = url.replace("ja-jp/", "zh-cn/");
									}
									else if (temp.getLang().equals("zh-tw")) {
										url = url.replace("ja/", "zhtw/");
										url = url.replace("ja-jp/", "zh-tw/");
									}
								}
								String[] arr2 = arr[3].split("/");
								if (arr2 != null ) {
									int cnt = 0;
									for(String tmp : arr2) {
										if (cnt == 0) url+="?id=" + tmp;
										else url+="&id=" +tmp;
										cnt++;
									}
									content+="<a target=\"_blank\" href=\""+url+"\" class=\"actlink\">"+messagesource.getMessage("discon.discontinued.detail", null, getLocale(temp.getLang()))+"</a>";
								} else if (s.getNewCatalogLink() != null && s.getNewCatalogLink().isEmpty() == false) {
									content+="<a target=\"_blank\" href=\""+s.getNewCatalogLink()+"\" class=\"actlink\">"+messagesource.getMessage("discon.discontinued.detail", null, getLocale(temp.getLang()))+"</a>";
								} else {
									content+="-&nbsp;";
								}
							}
							content+="</td>\r\n" +
							"</tr>";
						} catch (Exception e) {
							log.debug(e.getMessage());
						}
						ln++;
					}
				}
				content+= DiscontinuedConfig.contentEnd;
				{
					String tmp =s.getCompatibility();
					String tmp2 = s.getCaution();
					if ( (tmp != null && tmp.isEmpty() == false) || (tmp2 != null && tmp2.isEmpty() == false) ) {
						content += "<p class=\"dls\"><b>"+messagesource.getMessage("discon.compatibility.title", null, getLocale(temp.getLang()))+"</b><br/>";
						if (tmp != null && tmp.isEmpty() == false) content += "・"+messagesource.getMessage("discon.compatibility.interchangeability", null, getLocale(temp.getLang())) + s.getCompatibility()+"<br/>";
						if (tmp2 != null && tmp2.isEmpty() == false) content += "・"+messagesource.getMessage("discon.compatibility.precautions", null, getLocale(temp.getLang())) + s.getCaution()+"<br/>";
						content += "</p><div class=\"clear\"></div>";
					}
//					tmp =s.getCatalogLink();
					tmp = null; // 生産終了のカタログを行内に表示。// 2023/03/13
					tmp2 = s.getManualLink();
					if ( (tmp != null && tmp.isEmpty() == false) || (tmp2 != null && tmp2.isEmpty() == false) ) {
						String cate = "";
						if (tmp != null && tmp.isEmpty() == false) {
							cate += "<p class=\"dls\"><b>"+messagesource.getMessage("discon.discontinued.catalog", null, getLocale(temp.getLang()))
								+"</b>&nbsp;<a href=\""+tmp+"\" target=\"_blank\">"+messagesource.getMessage("act.download", null, getLocale(temp.getLang()))+"</a>";
						}
						if (tmp2 != null && tmp2.isEmpty() == false)  {
							if (cate.isEmpty() == false) cate+=" / " + "<b>"+messagesource.getMessage("discon.manual.title", null, getLocale(temp.getLang()))+"</b>";
							else  cate += "<p class=\"dls\"><b>"+messagesource.getMessage("discon.manual.title", null, getLocale(temp.getLang()))+"</b>";
							cate += "&nbsp;<a href=\""+tmp2+"\" target=\"_blank\">"+messagesource.getMessage("act.download", null, getLocale(temp.getLang()))+"</a>";
						}
						content += cate +"</p><div class=\"clear\"></div>";
					}
				}
				if (isTest && s.getComparison() != null && s.getComparison().isEmpty() == false) {
					// test. URLのみ比較PDF表示
					content += "<p class=\"dls intraonly\"><a href=\""+s.getComparison()+"\" target=\"_blank\">"+messagesource.getMessage("discon.discontinued.compair.pdf", null, getLocale(temp.getLang()))+"</a></p>";
				}
			}
			String search = DiscontinuedConfig.contentSearch;
			if (changeUrl != null && changeUrl.isEmpty() == false) {
				search = search.replace(context+"/discontinued/", context+"/"+changeUrl+"/");
			}
			content+= search;
			ret = ret.replace("$$$content$$$", content);
		}
		return ret;
	}
	private String getSidebar(String lang, String context, String changeUrl, ErrorObject err) {
		String ret = "";
		DiscontinuedTemplate temp = templateService.getLang(lang, err);
		List<DiscontinuedCategory> list = categoryService.listAllActive(lang, DiscontinuedModelState.PROD, err);
		String strCate = "";
		String url = "/discontinued/";
		if (changeUrl != null && changeUrl.isEmpty() == false) {
			url = "/"+changeUrl+"/";
		}
		for(DiscontinuedCategory c : list) {
			if (c.getSlug() != null && c.getSlug().isEmpty() == false)  {
				strCate+= "<li><span><a href=\""+context+url+"series/{lang}/{slug}/\" class=\"dr_link\">{title}</a></span></li>"
						.replace("{lang}", lang).replace("{slug}", c.getSlug()).replace("{title}", c.getName());
			} else {
				strCate+= "<li><span><a href=\""+context+url+"series/{lang}/category/{categoryId}/\" class=\"dr_link\">{title}</a></span></li>"
					.replace("{lang}", lang).replace("{categoryId}", c.getId()).replace("{title}", c.getName());
			}
		}
		ret = temp.getSidebar();
		ret = ret.replace("$$$category$$$", strCate);
		return ret;
	}

	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.equals("zh-tw")) loc = Locale.TRADITIONAL_CHINESE;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}


}
