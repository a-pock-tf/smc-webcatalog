package com.smc.webcatalog.api;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.smc.cad3d.model.Cad3d;
import com.smc.cad3d.service.Cad3dService;
import com.smc.exception.DataAccessException;
import com.smc.omlist.service.OmlistServiceImpl;
import com.smc.psitem.model.PsItem;
import com.smc.psitem.service.PsItemService;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.model.Category;
import com.smc.webcatalog.model.CategorySeries;
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
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.NarrowDownService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.JpServiceResult;
import com.smc.webcatalog.util.JpServiceUtil;
import com.smc.webcatalog.util.LibHtml;
import com.smc.webcatalog.util.LibSynonyms;
import com.smc.webcatalog.web.ScreenStatusHolder;

import lombok.extern.slf4j.Slf4j;

@RestController
@ResponseBody
@RequestMapping("/")
@Slf4j
public class ProductRestController {

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
	PsItemService psItemService;

	@Autowired
	Cad3dService cad3dService;
	
	@Autowired
	OmlistServiceImpl omlistService;
	
	@Autowired
	NarrowDownService narrowDownService;
	
	@Autowired
	SeriesFaqRepository faqRepo;


//	@Autowired
//	ImpPsItemService psItemService;

	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml html;

	// 3Sを検索し、一致しなければPsItemを表示
	// 2023/3/18 cdは検索条件、1:前方一致 2:部分一致
	// 2023/3/22 デフォルト、前方一致（日本語のみ選択可能）
	// 2023/9/25 日本語は「関連語」を取得する。
	// 2024/5/15 末尾が-の場合は削除
	// 2024/12/10 デフォルト、英語のみ前頭一致。他国、部分一致。前方一致は日本語、英語が選択可能
	@GetMapping(value={"/{lang}/search3S/"}, produces="text/html;charset=UTF-8")
	public String getSearch3SandPsItem(@PathVariable(name = "lang", required = true) String lang,
			@RequestParam(name = "kw", required = false) String kw,
			@RequestParam(name = "cd", required = false) String cd,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "category", required = false, defaultValue = "") String category,
			@RequestParam(name = "series", required = false, defaultValue = "") String series,
			HttpServletRequest request) {

		String ret = null;
		ErrorObject err = new ErrorObject();
		boolean isNoHit = false; // 検索結果0件

		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		if (err.isError()) {
			log.error("ErrorObject:msg="+err.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "ErrorObject!");
		}
		
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		// 3S
		JpServiceUtil util = new JpServiceUtil();

		JpServiceResult res = null;
		if (kw != null && kw.isEmpty() == false) {
			kw = kw.trim(); // 全件ヒット。2023/5/25
		}
		String kwOriginal = kw; // 3Sヒットを見るためにNormalizerを利用。
		if (kw != null && kw.isEmpty() == false && kw.getBytes().length != kw.length()) {
			kw = Normalizer.normalize(kw, Normalizer.Form.NFKC); // 全角英数を半角に
			if (kw.indexOf("ー") > -1) kw = kw.replace("ー", "-");
			if (kw.indexOf("‐") > -1) kw = kw.replace("‐", "-");
			if (kw.indexOf("―") > -1) kw = kw.replace("―", "-");
		}
		if (kw != null && kw.matches("^[A-z0-9-_/+#]{1,30}$")) { // 全部半角の場合のみ3S検索
			res = util.get(request.getServletContext(), kw, baseLang);
		} else {
			kw = kwOriginal; // すべて英数で無ければ戻す 2023/10/26 5ポートが5ポ-ト
		}

		if (res != null && res.getCode().isEmpty() == false && (res.getCode().equals("21") || res.getCode().equals("22")) ){
			if (kw.indexOf('#') > -1) kw = kw.replace("#", "%23");
			// 3Sの結果を表示
			StringBuilder r = new StringBuilder("<html><head><meta http-equiv=\"refresh\" content=\"0;URL=");
			//  /webcatalog/s3s/ja-jp/detail/CRB-CDRB/?partNumber=CRBJ10-90
			if (lang.indexOf("zh") > -1) {
				r.append("/webcatalog/s3s/").append(lang).append("/detail/?partNumber=").append(kw);
			} else if (lang.indexOf("en") > -1) {
				r.append("/webcatalog/s3s/").append(lang).append("/detail/?partNumber=").append(kw);
			} else {
				r.append("/webcatalog/s3s/").append(lang).append("/detail/?partNumber=").append(kw);
			}
			r.append("\"></head></html>");
			return r.toString();
		}
		// 3Sに一致しなければNormalizer対応を戻す。
//		kw = kwOriginal; // 2023/10/16 元に戻さない

		// デフォルト英語は前方一致、他国は部分一致に変更。2024/12/10
		if (cd == null || cd.isEmpty()) {
			if (baseLang.equals("en-jp")) cd = "1"; // カラの場合、英語なら前方一致
			else cd = "2"; // 他国なら部分一致
		} else if (cd.equals("1") == false && cd.equals("2") == false) {
			if (baseLang.equals("en-jp")) cd = "1"; // 1,2以外の場合、英語なら前方一致
			else cd = "2"; // 他国なら部分一致
		}
		LibSynonyms synonyms = new LibSynonyms();
		String[] arr = kw.split("[ 　]");
		// 末尾が - の場合は削除 2024/5/15
		if (arr != null) {
			for(int i = 0; i < arr.length; i++) {
				if (arr[i].lastIndexOf("-") == arr[i].length()-1) {
					arr[i] = arr[i].substring(0, arr[i].length()-1); 
				}
			}
		}
		List<String> synonymsList = synonyms.getSynonyms(arr, lang);
		if (synonymsList == null) synonymsList = Arrays.asList(arr);
		List<PsItem> items = psItemService.searchKeyword(synonymsList, cd, category, series, baseLang);
		List<PsItem> list = items;

		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		ModelState m = ModelState.PROD;
		if (isTestSite) m = ModelState.TEST;
		Boolean isActive = true;
		if (isTestSite) isActive = null; 
		
		Template t = templateService.getTemplateFromBean(baseLang, m); // メモリ上のTemplateから読み込み
		if (err.isError()) {
			log.error("ErrorObject:msg="+err.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "ErrorObject!");
		}
		if (lang.equals(baseLang) == false) { 
			Template tL = templateService.getTemplateFromBean(lang, m);
			if (err.isError()) {
				log.error("ErrorObject:msg="+err.getMessage());
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ErrorObject!");
			}
	
			if (tL.getHeader() != null && tL.getHeader().isEmpty() == false) {
				t = tL; // 言語のテンプレートが設定されていればそれを有効にする。
			}
		}

		// listの先頭のカテゴリテンプレートを取得
		TemplateCategory tc = null;
		tc = getTemplateCategoryFromSearchResult(list, langObj, m, isActive, err);
		if (tc == null) {
			// 失敗した場合はcategoryの先頭
			Category root = service.getRoot(baseLang, m, CategoryType.CATALOG, err);
			root = service.getWithChildren(root.getId(), null, err);
			Category topC = root.getChildren().get(0);
			if (topC == null || topC.isActive() == false) {
				topC = root.getChildren().get(1);
			}
			tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, topC.getId());
		}

		Category c = service.getLang(baseLang, m, CategoryType.CATALOG, true, err);

		boolean is2026 = tc.is2026();
		String temp = tc.getTemplate();

		String title = "検索結果";
		if (lang.indexOf("en-") > -1) {
			title = "Search result";
		} else if (lang.indexOf("zh-") > -1) {
			title = "搜索结果";
		}
		String strResult = "{0}件";
		if (lang.indexOf("en-") > -1) {
			strResult = "{0} hits";
		} else if (lang.indexOf("zh-") > -1) {
			strResult = "{0}项结果";
		}

		temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", title));
		String sidebar = tc.getSidebar();
		if (is2026) {
			sidebar = sidebar.replaceFirst("is-current\"", "\""); // メニューを閉じる
		} else {
			sidebar = sidebar.replaceFirst("class=\"child open\"", "class=\"child\""); // メニューを閉じる
		}
		List<Category> cList = service.listAll(c.getLang(), c.getState(), c.getType(), err);
		if (err.isError()) {
			log.error("ErrorObject:msg="+err.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "ErrorObject!");
		}
		
		List<Category> setCategoryList = new LinkedList<>();
		for(Category cate :  cList) {
			Category setC = service.getWithSeries(cate.getId(), null, err);
			if (setC != null) setCategoryList.add( setC );
		}
		List<String> cate = null;
		if (is2026) {
			cate = html.getCategoryMenu2026(lang, null, null, setCategoryList);
		} else {
			cate = html.getCategoryMenu(lang, null, null, setCategoryList);
		}
		sidebar = StringUtils.replace(sidebar,"$$$category$$$",cate.get(0));
		sidebar = StringUtils.replace(sidebar,"$$$category2$$$",cate.get(1));
		temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);
		if (is2026) {
			String catpan = "<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"></li>"
					+ "<li><a class=\"breadcrumb-item\" href=\"/webcatalog/"+baseLang+"/\">CATPAN</a>\r\n"
					+ "              </li>";
			temp = StringUtils.replace(temp,"$$$catpan_title$$$", catpan.replace("CATPAN", title));
			temp = StringUtils.replace(temp,"$$$formbox$$$", "");
			String h1box = "<div class=\"mb48 m-mb36 s-mb36\">\r\n"
					+ "              <div class=\"text-5xl leading-tight fw5\">KEYWORD</div>\r\n"
					+ "            </div>";
			temp = StringUtils.replace(temp,"$$$h1box$$$", h1box.replace("KEYWORD", kw) 
					+ tc.getFormbox().replace("pt24 px96 pb36 s-p24 s-gap-32 m-p24 m-gap-32", "p24 border s-px16 m-px16"));
		} else {
			temp = StringUtils.replace(temp,"$$$h1box$$$", tc.getH1box().replace("$$$title$$$", kw));
			temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox());
		}
		
		temp = StringUtils.replace(temp,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない

		if (temp != null) {
			// 検索キーワードと条件を設定 
			// 1:で始まる 2:を含む。
			// 2024/12/10 2がデフォルト。
			if (is2026) {
				temp = temp.replaceFirst("value=\"\" id=\"kwItem\"", "value=\""+kw+"\" id=\"kwItem\"");
				if (cd == null || cd.isEmpty() || cd.equals("2")) {
					temp = temp.replace("id=\"kwCond1\" value=\"1\" checked", "id=\"kwCond1\" value=\"1\"");
					temp = temp.replace("id=\"kwCond2\" value=\"2\"", "id=\"kwCond2\" value=\"2\" checked");
					temp = temp.replace("id=\"kwCondSP1\" value=\"1\" checked", "id=\"kwCondSP1\" value=\"1\"");
					temp = temp.replace("id=\"kwCondSP2\" value=\"2\"", "id=\"kwCondSP2\" value=\"2\" checked");
				} else if (cd != null && cd.equals("1")) {
					temp = temp.replace("id=\"kwCond2\" value=\"2\" checked", "id=\"kwCond2\" value=\"2\"");
					temp = temp.replace("id=\"kwCond1\" value=\"1\"", "id=\"kwCond1\" value=\"1\" checked");
					temp = temp.replace("id=\"kwCondSP2\" value=\"2\" checked", "id=\"kwCondSP2\" value=\"2\"");
					temp = temp.replace("id=\"kwCondSP1\" value=\"1\"", "id=\"kwCondSP1\" value=\"1\" checked");
				}
			} else {
				temp = temp.replaceFirst("value=\"\" id=\"kwItem\"", "value=\""+kw+"\" id=\"kwItem\"");
				if (cd == null || cd.isEmpty() || cd.equals("2")) {
					temp = temp.replaceFirst("class=\"kwCond\" value=\"1\" checked", "class=\"kwCond\" value=\"1\"");
					temp = temp.replaceFirst("class=\"kwCond\" value=\"2\"", "class=\"kwCond\" value=\"2\" checked");
				} else if (cd != null && cd.equals("1")) {
					temp = temp.replaceFirst("class=\"kwCond\" value=\"2\" checked", "class=\"kwCond\" value=\"2\"");
					temp = temp.replaceFirst("class=\"kwCond\" value=\"1\"", "class=\"kwCond\" value=\"1\" checked");
				}
			}
		}

		StringBuilder content = new StringBuilder();
		int listCount = 0;
		if (list != null && list.size() > 0) listCount =list.size(); // 型式のCount
		List<String> catList = new ArrayList<String>(); // 絞り込み
		List<String> serList = new ArrayList<String>();

		int cnt = 0;
		if (list != null && list.size() > 0) {
			int max = list.size();
			String[] th = AppConfig.PsItemSearchResultTableTh[0];
			if (lang.indexOf("en-") > -1) {
				th = AppConfig.PsItemSearchResultTableTh[1];
			} else if (lang.indexOf("zh-") > -1) {
				th = AppConfig.PsItemSearchResultTableTh[2];;
			}
			if (is2026) {
				content.append("<div class=\"w-full overflow-auto mb48\">\r\n")
						.append( "                              <table class=\"table\">\r\n")
						.append( "                                            <thead>\r\n")
						.append( "                                              <tr>");
				content.append("<th class=\"th w284\" colspan=\"1\">").append( th[0] ).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( th[1] ).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( th[2] ).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( th[3] ).append("</th>");
				content.append("<th class=\"th w120\" colspan=\"1\">&nbsp;</th>");
				content.append("</tr></thead>\r\n")
						.append( "<tbody><tr>");
			} else {
				content.append("<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n")
						.append("<tbody><tr>");
				content.append("<th style=\"width:280px;\">" + th[0] +"</th>");
				content.append("<th>").append( th[1] ).append("</th>");
				content.append("<th>").append( th[2] ).append("</th>");
				content.append("<th>").append( th[3] ).append("</th>");
				content.append("<th style=\"width:80px;\">&nbsp;</th>");
				content.append("</tr><tr>");
			}

			String detail = "詳細";
			if (lang.indexOf("en-") > -1) {
				detail = "Detail";
			} else if (lang.indexOf("zh-") > -1) {
				detail = "详情";
			}
			for(PsItem s : list) {
				StringBuilder c1c2 = new StringBuilder( s.getC1()).append("/").append(s.getC2());
				if (catList.contains(c1c2.toString()) == false) catList.add(c1c2.toString());
				if (serList.contains(s.getSeries()) == false) serList.add(s.getSeries());

				if (is2026) {
					content.append("<td class=\"td text-sm\" colspan=\"1\">" ).append( c1c2 ).append("</td>");
					content.append("<td class=\"td text-sm\" colspan=\"1\">" ).append( s.getName() ).append("</td>");
					content.append("<td class=\"td text-sm\" colspan=\"1\">" ).append( s.getSeries() ).append("</td>");
					content.append("<td class=\"td text-sm\" colspan=\"1\">" ).append( s.getItem() ).append("</td>");
					// queryが複数あれば複数対応
					String seriesUrl = s.getQuery();
					String[] arr2 = null;
					if (seriesUrl != null) arr2 = seriesUrl.split("id=");
					if (arr2 != null) {
						content.append("<td class=\"td text-center\" colspan=\"1\">");
						content.append("  <a class=\"button secondary solid medium\" href=\"").append(AppConfig.ProdRelativeUrl).append( baseLang ).append( "/seriesList/?").append(seriesUrl).append("\">" ).append(detail).append("</a>");
						content.append("</td>");
					} else {
						content.append("<td>&nbsp;</td>");
					}
				} else {
					content.append("<td>" ).append( c1c2 ).append("</td>");
					content.append("<td>" ).append( s.getName() ).append("</td>");
					content.append("<td>" ).append( s.getSeries() ).append("</td>");
					content.append("<td>" ).append( s.getItem() ).append("</td>");
					// queryが複数あれば複数対応
					String seriesUrl = s.getQuery();
					String[] arr2 = null;
					if (seriesUrl != null) arr2 = seriesUrl.split("id=");
					if (arr2 != null) {
						content.append("<td><a href=\"").append(AppConfig.ProdRelativeUrl).append( baseLang ).append( "/seriesList/?").append(seriesUrl).append("\">" ).append(detail ).append("</a></td>");
					} else {
						content.append("<td>&nbsp;</td>");
					}
				}
				cnt++;
				if (cnt < max) content.append("</tr>\r\n<tr>");
			}
			content.append("</tr>\r\n</tbody></table>");
			if (is2026) content.append("</div>\r\n");
		}

		if (is2026) {
			StringBuilder str = new StringBuilder();
			str.append( "<div class=\"mt48 mb24 s-mt36 s-mb8 s-mt36 m-mb8\">\r\n")
				.append("  <div class=\"f fm gap-16\">\r\n")
				.append("    <div class=\"text-2xl fw6 leading-tight\">").append(title).append("</div>\r\n")
				.append("    <div class=\"badge large filled\">").append(strResult.replace("{0}", String.valueOf(listCount))).append("</div>\r\n")
				.append("  </div>\r\n")
				.append("</div>\r\n");
			// 絞り込み
			if (list != null && list.size() > 0) {
				str.append("<div class=\"mb24\">\r\n")
					.append( "    <div class=\"mb16 s-mb8 m-mb8\">");
				
				if (lang.indexOf("en-") > -1) {
					str.append( "There were no hits with the full part number, but there were {0} hits in the keyword search.".replace("{0}", String.valueOf(listCount)));
				} else if (lang.indexOf("zh-") > -1) {
					str.append( "全型号搜索内未找到结果，但在关键词搜索内找到{0}项结果。\r\n".replace("{0}", String.valueOf(listCount)));
				} else {
					str.append( "フル品番ではヒットしませんでしたが、<u>キーワード検索に{0}件</u>のヒットがありました。".replace("{0}", String.valueOf(listCount)));
				}
				str.append( "    </div>");
				if (html.isDisconHit(baseLang, kw)) {
					String strDiscon = "";
					if (lang.indexOf("en-") > -1) {
						strDiscon = "Your search matched discontinued products. Please click here for details.";
					} else if (lang.indexOf("zh-") > -1) {
						strDiscon = "结果中包含了停产产品，点击此处了解更多信息。";
					} else {
						strDiscon = "生産終了製品のご案内にもヒットしています。詳細はこちらをクリックしてください。";
					}
					str.append( "  <a class=\"text-sm leading-tight text-primary\" href=\"").append(AppConfig.PageProdDisconUrl ).append( lang ).append( "/?kw=" ).append( kw ).append("\">")
						.append("    <span class=\"fw5 hover-link-underline\">").append(strDiscon).append("</span>")
						.append("    <img class=\"inline-block vertical-align-text-bottom s16 ml4 object-fit-contain\" src=\"/assets/smcimage/common/blank-primary.svg\" alt=\"\" title=\"\">")
						.append("  </a>\r\n");
				}
				str.append( "</div>");
				String search = getNarrowingHtml2026(kw, baseLang, category, catList, series, serList, true);
				content.insert(0, search);
				content.insert(0, str);
			} else {
				str.append( "<div class=\"f fh border boder-base-stroke-subtle mb24 h160 w-full bg-base-container-accent\">")
					.append( "  <span class=\"fw5 s-px16 s-text-center m-px16 m-text-center\">");
				if (lang.indexOf("en-") > -1) {
					str.append( "Products meeting the search conditions could not be found.");
				} else if (lang.indexOf("zh-") > -1) {
					str.append( "找不到要命中搜索条件的产品。");
				} else {
					str.append( "検索条件にヒットする製品が見つかりませんでした。");
				}
				str.append( "  </span>\r\n")
					.append( "</div>\r\n");
				content.insert(0, str);
				content.append(tc.getProductsSupport());
				isNoHit = true;
			}
		} else {
			// 絞り込み
			if (list != null && list.size() > 0) {
				StringBuilder str = new StringBuilder("<p>");
				if (lang.indexOf("en-") > -1) {
					str.append( "There were no hits with the full part number, but there were {0} hits in the keyword search.".replace("{0}", String.valueOf(listCount)));
				} else if (lang.indexOf("zh-") > -1) {
					str.append( "全型号搜索内未找到结果，但在关键词搜索内找到{0}项结果。\r\n".replace("{0}", String.valueOf(listCount)));
				} else {
					str.append("フル品番ではヒットしませんでしたが、<u>キーワード検索に{0}件</u>のヒットがありました。".replace("{0}", String.valueOf(listCount)));
				}
				str.append("</p><br>");
				String strDiscon = "生産終了製品のご案内にもヒットしています。詳細は<u>こちら</u>をクリックしてください。";
				if (lang.indexOf("en-") > -1) {
					strDiscon = "Your search matched discontinued products. Please click <u>here</u> for details.";
				} else if (lang.indexOf("zh-") > -1) {
					strDiscon = "结果中包含了停产产品，点击<u>此处</u>了解更多信息。";
				}
				if (html.isDisconHit(baseLang, kw)) {
					str.append("<br><br><p class=\"search_result_discon\"><a href=\"").append(AppConfig.PageProdDisconUrl).append(lang).append("/?kw=").append( kw ).append("\">").append( strDiscon ).append("</a></p><br>");
				}
	
				String search = getNarrowingHtml(kw, baseLang, category, catList, series, serList, true);
				content.insert(0, search);
				content.insert(0, str);
			} else {
				String str = "検索条件にヒットする製品が見つかりませんでした。";
				if (lang.indexOf("en-") > -1) {
					str = "Products meeting the search conditions could not be found.";
				} else if (lang.indexOf("zh-") > -1) {
					str = "找不到要命中搜索条件的产品。";
				}
				content.append("<br><p>" + str + "</p>");
				isNoHit = true;
			}
		}

		temp = StringUtils.replace(temp, "$$$content$$$", content.toString());
		
		// 中国はJingSocial対応。
		String head = t.getHeader();
		String foot = t.getFooter();
		if (isNoHit) {
			if (baseLang.equals("zh-cn")) {
				foot = foot.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", kw).replace("$YYY$", "0")+"\r\n<footer>");
			}
			head = head.replaceFirst("<script src=\"/assets/js/switching.js\"></script>", ""); // 言語切り替えを外す
			ret = head + temp + foot;

		} else {
			if (baseLang.equals("zh-cn")) {
				foot = foot.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", kw).replace("$YYY$", "1")+"\r\n<footer>");
			}
			ret = head + temp + foot;
		}

		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}

		return ret;
	}

	/// Webカタログサイト内検索
	@GetMapping(value={"/{lang}/searchSite/"}, produces="text/html;charset=UTF-8")
	public String getSearchGuide(@PathVariable(name = "lang", required = false) String lang,
			@RequestParam(name = "kw", required = false) String kw,
			@RequestParam(name = "page", required = false) String page,
			HttpServletRequest request) {
		String ret = null;
		ErrorObject err = new ErrorObject();
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		Boolean isActive = true;
		if (isTestSite) {
			m = ModelState.TEST;
			isActive = null;
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
		String kwOriginal = kw;
		if (kw == null || kw.isEmpty() || kw.trim().isEmpty() ) {
//			log.error("Keyword is  Empty! ");
//			throw new ResponseStatusException(
//					  HttpStatus.NOT_FOUND, "Keyword is Empty!");
			kw = "";
		} else if ( kw.getBytes().length != kw.length()) {
			kw = Normalizer.normalize(kw, Normalizer.Form.NFKC); // 全角英数を半角に
			kw = kw.trim();
			if (kw.indexOf("ー") > -1) kw = kw.replace("ー", "-");
			if (kw.indexOf("‐") > -1) kw = kw.replace("‐", "-");
			if (kw.indexOf("―") > -1) kw = kw.replace("―", "-");
			if (kw != null && kw.matches("^[A-z0-9-_/+]{1,30}$")) { // 全部半角の場合
				// そのまま
			} else {
				kw = kwOriginal; // すべて英数で無ければ戻す 2023/10/26 5ポートが5ポ-ト
			}
		} else {
			kw = kw.replace("　", " ");
			kw = kw.trim();
		}
		String[] arr = kw.split("[ 　]");
		
		LibSynonyms synonyms = new LibSynonyms();
		List<String> synonymsList = synonyms.getSynonyms(arr, baseLang);
		if (synonymsList == null && arr != null && arr.length > 0) {
			synonymsList = Arrays.asList(arr);
		}

		int p = 1;
		if (page != null) {
			try {
				p = Integer.parseInt(page);
			} catch (Exception e) {
				p = 1;
			}
		}
		int limit = 10;
		int cnt = 0;
		List<Series> list = null;
		if (synonymsList != null) {
			cnt = seriesService.searchCount(synonymsList.toArray(new String[synonymsList.size()]), baseLang, m, err);
			list = seriesService.getPage(synonymsList.toArray(new String[synonymsList.size()]), baseLang, m, p, limit, err);
		}

		Category c = service.getLang(baseLang, m, CategoryType.CATALOG, isActive, err); 
		Template t = templateService.getTemplateFromBean(baseLang, m);
		Template tL = templateService.getTemplateFromBean(lang, m);
		if (tL != null && tL.getHeader() != null) {
			t = tL;
		}
		
		SeriesHtml sHtml = new SeriesHtml(LibHtml.getLocale(baseLang), messagesource, omlistService, faqRepo);

		if (list != null && list.size() > 0) {
			// listの先頭のカテゴリテンプレートを取得
			TemplateCategory tc = getTemplateCategoryFromSeries(list, langObj, m, err);
			if (tc == null) {
				log.info("ProductRestController.getSearchGuide() tc == null. list[0].seriesId="+list.get(0).getId() + " list[0].modelNumber="+list.get(0).getModelNumber());
				tc = templateCategoryService.getCategory(c, err);
			}
			int i = 0;
			for( i = 0; i < list.size(); i++) {
				list.set(i,  seriesService.getWithLink(list.get(i).getId(), isActive, err));
			}
			ret = html.getSearchResultFromSeriesFile(kwOriginal, baseLang, t, tc, service, sHtml, list, AppConfig.ContextPath + "/" + lang+"/searchSite/?kw="+kw, p, cnt, isTestSite); // isTest=trueなら毎回生成
			
		} else if (c != null){
			TemplateCategory tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, c.getId());
			ret = html.getSearchResultFromSeriesFile(kwOriginal, baseLang, t, tc, service, sHtml, list, AppConfig.ContextPath + "/" + lang+"/searchSite/?kw="+kw, p, cnt, isTestSite); // isTest=trueなら毎回生成
		}

		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		
		// 中国JingSocial対応。
		if (baseLang.equals("zh-cn")) {
			if (list == null || list.size() == 0) { 
				ret = ret.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", kw).replace("$YYY$", "0")+"\r\n<footer>");
			} else {
				ret = ret.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", kw).replace("$YYY$", "1")+"\r\n<footer>");
			}
		}
		return ret;
	}

	// 頭文字検索
	@GetMapping(value={"/{lang}/indexSearch/{h}", "/{lang}/indexSearch/{h}/"}, produces="text/html;charset=UTF-8")
	public String getIndexPsItem(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "h", required = true) String h,
			@RequestParam(name = "category", required = false, defaultValue = "") String category,
			@RequestParam(name = "series", required = false, defaultValue = "") String series,
			HttpServletRequest request) {

		String ret = null;
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
		Locale baseLocale = LibHtml.getLocale(baseLang);

		if (h != null && h.isEmpty() == false) {
			h = h.trim();
		}
		if (h != null && h.isEmpty() == false && h.getBytes().length != h.length()) {
			h = Normalizer.normalize(h, Normalizer.Form.NFKC); // 全角英数を半角に
		}

		if (h != null) h = h.toUpperCase();
		String head = "^"+h;
		List<PsItem> items = psItemService.searchIndex(head, category, series, baseLang);
		List<PsItem> list = items;
		// psItemから非表示は除外 2022/7/5
/*		List<PsItem> list = new LinkedList<PsItem>();
		for(PsItem i : items) {
			Series s = seriesService.getFromModelNumber(i.getSid(), ModelState.PROD, err);
			if (s != null && s.isActive()) {
				list.add(i);
			}
		}
*/
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		if (isTestSite) m = ModelState.TEST;
		Boolean isActive = true;
		if (isTestSite) isActive = null; 

		Category c = service.getLang(baseLang, m, CategoryType.CATALOG, true, err);
		Template t = templateService.getTemplateFromBean(baseLang, m);
		Template tL = templateService.getTemplateFromBean(lang, m);
		if (tL != null && tL.getHeader() != null) {
			t = tL;
		}
		Category prodRoot = service.getRoot(baseLang, m, CategoryType.CATALOG, err);
		prodRoot = service.getWithChildren(prodRoot.getId(), true, err);

		// 先頭が作成中で「テンプレートがない場合がある。2022/10
		// listの先頭のカテゴリテンプレートを取得
		TemplateCategory tc = null;
		tc = getTemplateCategoryFromSearchResult(list, langObj, m, isActive, err);
		if (tc == null) {
			Category prodC = prodRoot.getChildren().get(0);
			if (prodC != null && prodC.isActive()) {
				tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, prodC.getId());
				if (tc == null) {
					if (prodRoot.getChildren().size() > 1) {
						prodC = prodRoot.getChildren().get(1);
						if (prodC == null || prodC.isActive() == false) {
							prodC = prodRoot.getChildren().get(1);
							tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, prodC.getId());
						}
					}
				}
			}
		}

		boolean is2026 = tc.is2026();
		String temp = tc.getTemplate();
		
		String title = "検索結果";
		if (lang.indexOf("en-") > -1) {
			title = "Search result";
		} else if (lang.indexOf("zh-") > -1) {
			title = "搜索结果";
		}
		String strResult = "{0}件";
		if (lang.indexOf("en-") > -1) {
			strResult = "{0} hits";
		} else if (lang.indexOf("zh-") > -1) {
			strResult = "{0}项结果";
		}

		temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", messagesource.getMessage("msg.search.title", null, baseLocale)));
		String sidebar = null;
		if (is2026) {
			sidebar = tc.getSidebar().replaceFirst("is-current\"", "\""); // メニューを閉じる
		} else {
			sidebar = tc.getSidebar().replaceFirst("class=\"child open\"", "class=\"child\""); // メニューを閉じる
		}
		List<Category> cList = service.listAll(c.getLang(), c.getState(), c.getType(), err);
		List<Category> setCategoryList = new LinkedList<>();
		for(Category cate :  cList) {
			Category setC = service.getWithSeries(cate.getId(), null, err);
			if (setC != null) setCategoryList.add( setC );
		}
		List<String> cate = null;
		if (is2026) {
			cate = html.getCategoryMenu2026(lang, null, null, setCategoryList);
		} else {
			cate = html.getCategoryMenu(lang, null, null, setCategoryList);
		}
		sidebar = StringUtils.replace(sidebar,"$$$category$$$",cate.get(0));
		sidebar = StringUtils.replace(sidebar,"$$$category2$$$",cate.get(1));
		temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);
		if (is2026) {
			StringBuilder catpan = new StringBuilder();
			catpan.append("<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"></li>")
					.append("<li><a class=\"breadcrumb-item\" href=\"/webcatalog/").append(baseLang).append("/\">CATPAN</a></li>\r\n");
			temp = StringUtils.replace(temp,"$$$catpan_title$$$", catpan.toString().replace("CATPAN", title));
			temp = StringUtils.replace(temp,"$$$formbox$$$", "");
			StringBuilder h1box =  new StringBuilder();
			h1box.append("<div class=\"mb48 m-mb36 s-mb36\">\r\n")
					.append("      <div class=\"text-5xl leading-tight fw5\">").append(h).append("</div>\r\n")
					.append("</div>");
			temp = StringUtils.replace(temp,"$$$h1box$$$", h1box.toString() 
					+ tc.getFormbox().replace("pt24 px96 pb36 s-p24 s-gap-32 m-p24 m-gap-32", "p24 border s-px16 m-px16"));
		} else {
			temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox());
			temp = StringUtils.replace(temp,"$$$h1box$$$", tc.getH1box().replace("$$$title$$$", h));
		}
		
		temp = StringUtils.replace(temp,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない
		
		StringBuilder content = new StringBuilder();
		int listCount = list.size(); // 型式のCount
		List<String> catList = new ArrayList<String>();
		List<String> serList = new ArrayList<String>();
		if (list.size() > 0) {
			if (is2026) {
				content.append("<div class=\"w-full overflow-auto mb48\">\r\n")
					.append( "                <div class=\"min-w-900\">\r\n")
					.append( "                              <table class=\"table\">\r\n")
					.append( "                                            <thead>\r\n")
					.append( "                                              <tr>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( messagesource.getMessage("msg.head.table.th1", null,  baseLocale)).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( messagesource.getMessage("msg.head.table.th2", null,  baseLocale)).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( messagesource.getMessage("msg.head.table.th3", null,  baseLocale)).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">").append( messagesource.getMessage("msg.head.table.th4", null,  baseLocale)).append("</th>");
				content.append("<th class=\"th w234\" colspan=\"1\">&nbsp;</th>");
				content.append("</tr></thead>\r\n")
						.append( "<tbody><tr>");
			} else {
				content.append("<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n")
					.append("<tbody><tr>");
				content.append("<th style=\"width:280px;\">").append( messagesource.getMessage("msg.head.table.th1", null,  baseLocale)).append("</th>");
				content.append("<th>").append( messagesource.getMessage("msg.head.table.th2", null,  baseLocale)).append("</th>");
				content.append("<th>").append( messagesource.getMessage("msg.head.table.th3", null,  baseLocale)).append("</th>");
				content.append("<th>").append( messagesource.getMessage("msg.head.table.th4", null,  baseLocale)).append("</th>");
				content.append("<th style=\"width:80px;\">&nbsp;</th>");
				content.append("</tr><tr>");
			}
			int cnt = 0;
			int max = list.size();
			for(PsItem s : list) {
				String c1c2 = s.getC1()+"/"+s.getC2();
				if (catList.contains(c1c2) == false) catList.add(c1c2);
				if (serList.contains(s.getSeries()) == false) serList.add(s.getSeries());
				
				if (is2026) {
					content.append("<td class=\"td text-sm\" colspan=\"1\">").append( c1c2 ).append("</td>");
					content.append("<td class=\"td text-sm\" colspan=\"1\">").append( s.getName() ).append("</td>");
					content.append("<td class=\"td text-sm\" colspan=\"1\">").append( s.getSeries() ).append("</td>");
					content.append("<td class=\"td text-sm\" colspan=\"1\">").append( s.getItem() ).append("</td>");
					// queryが複数あれば複数対応
					String seriesUrl = s.getQuery();
					String[] arr2 = null;
					if (seriesUrl != null) arr2 = seriesUrl.split("id=");
					if (arr2 != null) {
						content.append( "<td class=\"td text-center\" colspan=\"1\">");
						content.append( "  <a class=\"button secondary solid medium\" href=\"").append(AppConfig.ProdRelativeUrl).append( baseLang).append( "/seriesList/?").append(seriesUrl).append("\">").append( messagesource.getMessage("msg.head.table.detail", null,  baseLocale) ).append("</a>")
								.append( "</td>");
					} else {
						content.append("<td>&nbsp;</td>");
					}
				} else {
					content.append("<td>").append( c1c2 ).append("</td>");
					content.append("<td>").append( s.getName() ).append("</td>");
					content.append("<td>").append( s.getSeries() ).append("</td>");
					content.append("<td>").append( s.getItem() ).append("</td>");
					String seriesUrl = s.getQuery();
					content.append("<td><a href=\"").append(AppConfig.ProdRelativeUrl).append( baseLang ).append( "/seriesList/?").append(seriesUrl).append("\">").append( messagesource.getMessage("msg.head.table.detail", null,  baseLocale) ).append("</a></td>");
				}
				cnt++;
				if (cnt < max) content.append("</tr>\r\n<tr>");
			}
			content.append("</tr>\r\n</tbody></table>");
			if (is2026) content.append("</div></div>\r\n");
		} else {
			// is2026 == trueは下部で処理
			if (is2026 == false) content.append("<br>\r\n<p>" + messagesource.getMessage("msg.search.empty", null,  baseLocale) + "</p>\r\n");
		}
		if (is2026) {
			String str = "<div class=\"mt48 mb24 s-mt36 s-mb8 s-mt36 m-mb8\">\r\n"
					+ "                            <div class=\"f fm gap-16\">\r\n"
					+ "                              <div class=\"text-2xl fw6 leading-tight\">"+title+"</div>\r\n"
					+ "                              <div class=\"badge large filled\">"+strResult.replace("{0}", String.valueOf(listCount))+"</div>\r\n"
					+ "                            </div>\r\n"
					+ "              </div>";
			// 絞り込み
			if (list != null && list.size() > 0) {
				str += "<div class=\"mb24\">\r\n";
				
				if (html.isDisconHit(baseLang, h)) {
					String strDiscon = "";
					if (lang.indexOf("en-") > -1) {
						strDiscon = "Your search matched discontinued products. Please click here for details.";
					} else if (lang.indexOf("zh-") > -1) {
						strDiscon = "结果中包含了停产产品，点击此处了解更多信息。";
					} else {
						strDiscon = "生産終了製品のご案内にもヒットしています。詳細はこちらをクリックしてください。";
					}
					str += "  <a class=\"text-sm leading-tight text-primary\" href=\""+AppConfig.PageProdDisconHeadUrl + lang + "/" + h +"\"><span class=\"fw5 hover-link-underline\">"+strDiscon+"</span><img class=\"inline-block vertical-align-text-bottom s16 ml4 object-fit-contain\" src=\"/assets/smcimage/common/blank-primary.svg\" alt=\"\" title=\"\"></a>\r\n";
				}
				str += "</div>";
				String search = getNarrowingHtml2026(h, baseLang, category, catList, series, serList, true);
				content.insert(0,  search);
				content.insert(0,  str);
			} else {
				str += "<div class=\"f fh border boder-base-stroke-subtle mb24 h160 w-full bg-base-container-accent\">"
						+ "<span class=\"fw5 s-px16 s-text-center m-px16 m-text-center\">";
				if (lang.indexOf("en-") > -1) {
					str += "Products meeting the search conditions could not be found.";
				} else if (lang.indexOf("zh-") > -1) {
					str += "找不到要命中搜索条件的产品。";
				} else {
					str += "検索条件にヒットする製品が見つかりませんでした。";
				}
				str += "</span>"
					+ "</div>";
				content.insert(0,  str);
				content.append(tc.getProductsSupport());
			}
		} else {
			String strDiscon = "";
			if (html.isDisconHeadHit(baseLang, h)) {
				strDiscon ="<br>\r\n<p class=\"search_result_discon\">\r\n<a href=\""+AppConfig.PageProdDisconHeadUrl
						+ lang + "/" + h +"\">"+messagesource.getMessage("msg.search.discon.hit", null, baseLocale) + "</a>\r\n</p>\r\n<br>\r\n";
			}
			// 絞り込み
			String search = getNarrowingHtml(h, baseLang, category, catList, series, serList, false);
			content.insert(0,  search + strDiscon);
			content.insert(0,  "<p>"+ messagesource.getMessage("msg.head.hit", new Object[] {h, listCount},  baseLocale) + "</p>\r\n<br>");
		}

		temp = StringUtils.replace(temp, "$$$content$$$", content.toString());
		ret = t.getHeader() + temp + t.getFooter();

		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);

		}

		return ret;
	}

	private String getNarrowingHtml(String h, String lang, String category, List<String> catList, String series, List<String> serList, boolean isKeyword) {
		String func = "submitProductsIndexSearch";
		if (isKeyword) func = "submitProductsKeywordSearch";
		StringBuilder search = new StringBuilder("<div class=\"additional\">\r\n")
				.append("        <p class=\"bold\">").append( messagesource.getMessage("msg.head.search.title", null,  LibHtml.getLocale(lang)) ).append("</p>\r\n")
				.append("        <div class=\"searchform ps\">\r\n")
				.append("        <label>").append( messagesource.getMessage("msg.head.search.category", null,  LibHtml.getLocale(lang)) ).append(":</label>\r\n")
				.append("        <select name=\"c1c2\" onchange=\"").append(func).append("('").append(h).append("','category','").append(lang).append("');\" id=\"indexCategory\"><option value=\"\">").append( messagesource.getMessage("msg.head.search.none", null,  LibHtml.getLocale(lang))).append( "</option>");
		for(String cat : catList) {
			if (category.isEmpty() == false && cat.equals(category)) {
				search.append("<option value=\"").append(cat).append("\" selected>").append(cat).append("</option>\r\n");
			} else {
				search.append("<option value=\"").append(cat).append("\">").append(cat).append("</option>\r\n");
			}
		}
		search.append( "          </select>\r\n")
				.append("        </div>\r\n")
				.append("        <p class=\"idt\">").append( messagesource.getMessage("msg.head.search.or", null,  LibHtml.getLocale(lang)) ).append( "</p>\r\n")
				.append("        <div class=\"searchform ps\">\r\n")
				.append("        <label>").append( messagesource.getMessage("msg.head.search.series", null,  LibHtml.getLocale(lang)) ).append( ":</label>\r\n" )
				.append("        <select name=\"series\" onchange=\"").append(func).append("('").append(h).append("','series','").append(lang).append("');\" id=\"indexSeries\"><option value=\"\">").append( messagesource.getMessage("msg.head.search.none", null,  LibHtml.getLocale(lang))).append( "</option>");
		for(String ser : serList) {
			if (series.isEmpty() == false && ser.equals(series)) {
				search.append("<option value=\"").append(ser).append("\" selected>").append(ser).append("</option>\r\n") ;
			} else {
				search.append("<option value=\"").append(ser).append("\">").append(ser).append("</option>\r\n" );
			}
		}
		search.append( "         </select>\r\n")
			.append("        </div>\r\n")
			.append("        <input type=\"hidden\" name=\"h\" value=\"N\">\r\n")
			.append("        <input class=\"bt\" type=\"button\" value=\"").append(messagesource.getMessage("msg.head.search.reset", null,  LibHtml.getLocale(lang))).append("\" onclick=\"").append(func).append("('").append(h).append("','','").append(lang).append("');\">\r\n" )
			.append("        </div>");
		return search.toString();
	}
	private String getNarrowingHtml2026(String h, String lang, String category, List<String> catList, String series, List<String> serList, boolean isKeyword) {
		String func = "submitProductsIndexSearch";
		if (isKeyword) func = "submitProductsKeywordSearch";
		boolean isClearEnabled = (category != null || series != null);
		
		StringBuilder search = new StringBuilder("<div class=\"w-full p16 bg-base-container-accent border border-base-stroke-default mb24\">\r\n")
				.append("        <div class=\"f fbw mb16\">\r\n")
				.append("          <div class=\"f fm flex-fixed gap-4\"><img class=\"s20 object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\" title=\"\">\r\n")
				.append("            <div class=\"leading-none fw5\">"+ messagesource.getMessage("msg.head.search.title", null,  LibHtml.getLocale(lang)) +"</div>\r\n")
				.append("          </div>\r\n");
		String disabled = "disabled";
		if (isClearEnabled) disabled = "";
		search.append("          <!-- 絞り込み時：enabled、絞り込み未実施：disabled(デフォルトでdisabled)-->\r\n")
			.append("          <button class=\"button medium solid secondary text-primary\" type=\"button\" "+disabled+" onclick=\""+func+"('"+h+"','','"+lang+"');\">"+messagesource.getMessage("msg.head.search.reset", null,  LibHtml.getLocale(lang))+"</button>\r\n");

		search.append("        </div>\r\n")
				.append( "        <div class=\"f p24 bg-base-foreground-on-fill border border-base-stroke-subtle s-fclm s-py24 s-px16 m-fclm m-py24 m-px16\">\r\n")
				.append( "          <div class=\"f fclm w-full\">\r\n")
				.append( "            <div class=\"text-sm fw5 mb8\">"+ messagesource.getMessage("msg.head.search.category", null,  LibHtml.getLocale(lang)) +"</div>\r\n")
				.append( "                        <div class=\"select\">\r\n")
				.append( "                          <select name=\"c1c2\" onchange=\""+func+"('"+h+"','category','"+lang+"');\" id=\"indexCategory\"><option value=\"\">" + messagesource.getMessage("msg.head.search.none", null,  LibHtml.getLocale(lang)) + "</option>");
		for(String cat : catList) {
			if (category.isEmpty() == false && cat.equals(category)) {
				search.append("<option value=\""+cat+"\" selected>"+cat+"</option>\r\n");
			} else {
				search.append("<option value=\""+cat+"\">"+cat+"</option>\r\n");
			}
		}
		search.append("                          </select>\r\n")
			.append( "                        </div>\r\n")
			.append( "          </div>\r\n")
			.append( "          <div class=\"flex-fixed f fb pb8 px16 s-py16 s-pb0 m-py16 m-pb0 s-fh m-fh\">"+ messagesource.getMessage("msg.head.search.or", null,  LibHtml.getLocale(lang)) +"</div>\r\n")
			.append( "          <div class=\"f fclm w-full\">\r\n")
			.append( "            <div class=\"text-sm fw5 mb8\">"+ messagesource.getMessage("msg.head.search.series", null,  LibHtml.getLocale(lang)) +"</div>\r\n")
			.append( "                        <div class=\"select\">\r\n")
			.append( "                          <select name=\"series\" onchange=\""+func+"('"+h+"','series','"+lang+"');\" id=\"indexSeries\"><option value=\"\">" + messagesource.getMessage("msg.head.search.none", null,  LibHtml.getLocale(lang)) + "</option>");
				for(String ser : serList) {
					if (series.isEmpty() == false && ser.equals(series)) {
						search.append( "<option value=\""+ser+"\" selected>"+ser+"</option>\r\n") ;
					} else {
						search.append( "<option value=\""+ser+"\">"+ser+"</option>\r\n" );
					}
				}
		search.append("                          </select>\r\n")
			.append( "                        </div>\r\n")
			.append( "          </div>\r\n")
			.append( "        </div>\r\n")
			.append( "      </div>");
		return search.toString();
	}

	// 旧カテゴリID
	// ModelState.PROD, CategoryType.CATALOG 専用
	@GetMapping(value={"/{lang}/categoryID/{oldId}", "/{lang}/categoryID/{oldId}/"}, produces="text/html;charset=UTF-8")
	public String getOldCategory(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "oldId", required = true) String oldId,
			HttpServletRequest request) {
		String ret = null;
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

		int old = 0;
		if (oldId != null && oldId.isEmpty() == false) {
			old = Integer.parseInt(oldId);
		}
		if (old > 0) {
			Category c = service.getFromOldId(oldId, ModelState.PROD, CategoryType.CATALOG, null, err);
			if (c != null) {
				Category root = service.getRoot(baseLang, c.getState(), c.getType(), err);
				if (c.getParentId().equals(root.getId())) {
					// 1階層目
					ret = html.getFileFromHtml(baseLang+"/" + c.getSlug() +"/index.html");
				} else {
					// 2階層目
					Category parentCategory = service.get(c.getParentId(), err);
					String tmp = html.getFileFromHtml( baseLang+"/" + parentCategory.getSlug() + "/" + c.getSlug() + "/index.html");
					if (tmp == null || tmp.isEmpty()) {
						tmp = html.getFileFromHtml( baseLang+"/" +  parentCategory.getSlug() + "/" + c.getSlug() + "/index.html");
					}
					if (tmp != null && tmp.isEmpty() == false) {
						ret = tmp;
					}
					if (ret == null || ret.isEmpty()) {
						log.error("ERROR! ret is null. oldId=" + oldId);
						throw new ResponseStatusException(
								  HttpStatus.NOT_FOUND, "oldId not found"
								);
					}
				}
			} else {
				log.debug("oldId is Bad or Empty! oldId=" + oldId);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "OldId is Empty!");
			}

		} else {
			log.debug("oldId is Bad or Empty! oldId=" + oldId);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "OldId is Empty!");
		}
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, ModelState.PROD);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		return ret;
	}

	// シリーズIDが複数の場合
	@GetMapping(value={"/{lang}/seriesList/", "/{lang}/seriesList/{page}"}, produces="text/html;charset=UTF-8")
	public String getSeries(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "page", required = false) String page,
			@RequestParam(name = "id", required = true) String[] ids,
			HttpServletRequest request) {
		String ret = null;
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
		Locale baseLocale = LibHtml.getLocale(baseLang);

		boolean isTestSite = LibHtml.isTestSite(request.getRequestURL().toString());
		
		ModelState m = ModelState.PROD;
		Boolean isActive = true;
		if (isTestSite) {
			m = ModelState.TEST;
			isActive = null;
		}

		if (page == null || page.equals("index.html")) {
			List<Series> list = new LinkedList<Series>();
			for(String seid : ids ) {
				Series s = seriesService.getFromModelNumber(seid, m, err);
				if (s != null && s.isActive()) {
					if (baseLang.equals(s.getLang()) == false) {
						log.debug("Series.lang is not Same! lang=" + lang + " seriesLang="+s.getLang());
						throw new ResponseStatusException(
								  HttpStatus.NOT_FOUND, "Series.lang is not Same! lang=" + lang + " seriesLang="+s.getLang());
					}
					list.add(s);
				}
			}
			if (list == null || list.size() == 0) {
				log.error("ERROR! list is null. sid=" + ids.toString());
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ids not found"
						);
			}
			Template t = templateService.getTemplateFromBean(baseLang, m);
			Template tL = templateService.getTemplateFromBean(lang, m);
			if (tL != null && tL.getHeader() != null) {
				t = tL;
			}
			// listの先頭のカテゴリテンプレートを取得
			TemplateCategory tc = getTemplateCategoryFromSeries(list, langObj, m, err);
			if (tc == null) {
				log.info("ProductRestController.getSeries() tc == null. list[0].seriesId="+list.get(0).getId() + " list[0].modelNumber="+list.get(0).getModelNumber());
				tc = templateCategoryService.getLangAndStateFromBean(baseLang, m);
			}
			if (tc.is2026()) {
				SeriesHtml sHtml = new SeriesHtml(LibHtml.getLocale(baseLang), messagesource, omlistService, faqRepo);
				ret = html.getSearchResult2026("", baseLang,  t, tc, service, seriesService, sHtml, list, 0, -1, null, isTestSite);
			} else {
				ret = html.getSearchResult("", baseLang,  t, tc, service,  list, 0, -1, null);
			}

			String backUrl = request.getHeader("REFERER");
			String mes = messagesource.getMessage("button.back.to.list", null,  baseLocale);
			if (backUrl != null && backUrl.isEmpty() == false ) {
				if (tc.is2026()) {
					ret = StringUtils.replace(ret, "$$$backUrl$$$", 
							SeriesHtml._BackToList.replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
				} else {
					ret = StringUtils.replace(ret, "$$$backUrl$$$", "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
				}
			} else {
				ret = StringUtils.replace(ret, "$$$backUrl$$$", ""); // ボタン自体消す
				ret = StringUtils.replace(ret, "<a href=\"\" class=\"backToList\">"+mes+"</a>", "");
			}
		} else {
			for(String seid : ids ) {
				ret = html.getFileFromHtml(baseLang + "/series/" + seid + "/s.html");
			}
		}
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}

		return ret;

	}

	// シリーズ単体
	@GetMapping(value={"/series/{seid}", "/series/{seid}/", "/series/{seid}/{page}",
		"/{lang}/series/{seid}", "/{lang}/series/{seid}/", "/{lang}/series/{seid}/{page}",
		"/{lang}/series/{seid}/{seid2}", "/{lang}/series/{seid}/{seid2}/", "/{lang}/series/{seid}/{seid2}/{page}",}, produces="text/html;charset=UTF-8")
	public String getSeries(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "seid") String seid,
			@PathVariable(name = "seid2", required = false ) String seid2,
			@PathVariable(name = "page", required = false) String page,
			HttpServletRequest request) {

		String ret = null;
		ErrorObject err = new ErrorObject();

		Series s = null;
		if (lang.equals("zh-tw")) {
			// 3Sが簡体語のため、seidが-ZHで終わっているものが来る場合がある
			if (seid.lastIndexOf("-ZH") >= seid.length()-3) {
				seid = seid.replace("-ZH", "-ZHTW");
			}
			s = seriesService.getFromModelNumber(seid, ModelState.PROD, err);
		} else {
			s = seriesService.getFromModelNumber(seid, ModelState.PROD, err);
		}
		if (s == null) {
			if (seid2 != null) {
				s = seriesService.getFromModelNumber(seid+"/"+seid2, ModelState.PROD, err);
				if (s != null ) {
					seid = seid + "/" + seid2;
					page = null;
				} else {
					// / 区切りSeriesIDのため、pageを連結してみる
					if (page != null) {
						s = seriesService.getFromModelNumber(seid + "/" +seid2+"/" + page, ModelState.PROD, err);
						if (s != null) {
							seid = seid + "/" + seid2 + "/" + page;
							page = null;
						}
					}
				}
			}
			if (s == null) {
				log.debug("ERROR! series is null. seid=" + seid);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ret is null."
						);
			}
		} else if (err.isError() == true) {
			log.debug("ERROR! ErrorObject isError(). seid=" + seid + " ErrorObject=" + err.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, " ErrorObject isError()."
					);
		} else if (s.isActive() == false) {
			log.error("ERROR! series is not active. seid=" + seid);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "series not found."
					);
		}
		if (lang == null || lang.isEmpty()) {
			lang = s.getLang();
		} 
		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.debug("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		if (baseLang.equals(s.getLang()) == false) {
			log.debug("Series.lang is not Same! lang=" + lang + " seriesLang="+s.getLang());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Series.lang is not Same! lang=" + lang + " seriesLang="+s.getLang());
		}
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		Boolean isActive = true;
		if (isTestSite) {
			m = ModelState.TEST;
			isActive = null;
		}

		Locale baseLocale = LibHtml.getLocale(baseLang);
		
		if (page == null || page.equals("index.html")) {
			ret = html.getFileFromHtml(baseLang + "/series/" + seid + "/index.html" ); // テンプレート組み込み済み
			String backUrl = request.getHeader("REFERER");
			String mes = messagesource.getMessage("button.back.to.list", null,  baseLocale);
			if (backUrl != null && backUrl.isEmpty() == false ) {
				if (ret.indexOf("<main ") > -1) { // 2026
					ret = StringUtils.replace(ret, "$$$backUrl$$$",
							SeriesHtml._BackToList.replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
				} else {
					ret = StringUtils.replace(ret, "$$$backUrl$$$", "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
				}
			} else {
				ret = StringUtils.replace(ret, "$$$backUrl$$$", ""); // ボタン自体消す
				ret = StringUtils.replace(ret, "<a href=\"\" class=\"backToList\">"+mes+"</a>", "");
			}
		} else {
			ret = html.getFileFromHtml(baseLang + "/series/" + seid + "/s.html");
		}
		if (ret == null || ret.isEmpty()) {
			log.debug("ERROR! ret is null. seid=" + seid);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "slug not found"
					);
		}
		// すでにモーダルが入っているか確認。入っていなければモーダル追加。2023/11/27 変更前はif無しで全部に追加。すると２つ入る場合があった。
		if (ret.indexOf(".js-modal-open") == -1) { 
			ret += SeriesHtml._seriesCadModal;
		}
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		return ret;
	}
	@GetMapping(value={"", "/", "index.html","index.jsp"}, produces="text/html;charset=UTF-8")
	public String redirect() {
		String ret = "<html><head><meta http-equiv=\"refresh\" content=\"0;URL=/webcatalog/ja-jp/\"></head><body></body><html>";

		return ret;
	}

	@GetMapping(value={"/ja-jp", "/ja-jp/", "/ja-jp/{slug}", "/ja-jp/{slug}/{slug2}", "/ja-jp/{slug}/{slug2}/{se_id}", "/ja-jp/{slug}/", "/ja-jp/{slug}/{slug2}/", "/ja-jp/{slug}/{slug2}/{se_id}/", "/ja-jp/{slug}/{slug2}/{se_id}/{se_id2}",
		"/en-*/", "/en-*/{slug}", "/en-*/{slug}/{slug2}", "/en-*/{slug}/{slug2}/{se_id}", "/en-*/{slug}/", "/en-*/{slug}/{slug2}/", "/en-*/{slug}/{slug2}/{se_id}/", "/en-*/{slug}/{slug2}/{se_id}/{se_id2}",
		"/zh-*/", "/zh-*/{slug}", "/zh-*/{slug}/{slug2}", "/zh-*/{slug}/{slug2}/{se_id}", "/zh-*/{slug}/", "/zh-*/{slug}/{slug2}/", "/zh-*/{slug}/{slug2}/{se_id}/", "/zh-*/{slug}/{slug2}/{se_id}/{se_id2}"}, produces="text/html;charset=UTF-8")
	/**
	 * カテゴリ一覧。
	 * 2024/10/8 写真一覧追加のため、listとcookie,resが追加。
	 * @param slug
	 * @param slug2
	 * @param se_id
	 * @param view デフォルトがpicture。今までの表示がlist
	 * @param action narrowdownで絞り込み表示
	 * @param key 下のCountを作成。全部アップ後、削除! 2025/11/25
	 * @param narrowDownCount narrowKeyでパラメータの変更検出をヤメて項目数で確認することに。2025/11/25
	 * @param cookie
	 * @param request
	 * @param res
	 * @return
	 */
	public String get(
			@PathVariable(name = "slug", required = false) String slug,
			@PathVariable(name = "slug2", required = false) String slug2,
			@PathVariable(name = "se_id", required = false) String se_id,
			@PathVariable(name = "se_id2", required = false) String se_id2,
			@RequestParam(name = "view", required = false) String view,
			@RequestParam(name = "action", required = false) String action,
			@RequestParam(name = "narrowKey", required = false) String[] key,
			@RequestParam(name = "narrowCnt", required = false) String narrowDownCount,
			@RequestParam(name = "nCnt", required = false) String nCnt,
			@CookieValue(value="WebCatalogListState", required = false) Cookie cookie,
			HttpServletRequest request,
			HttpServletResponse res
			) {
		String ret = null;
		String lang="ja-jp";
		ErrorObject err = new ErrorObject();
		log.debug("get === lang=== uri="+request.getRequestURI());
		
		if (view != null && view.isEmpty() == false) {
			if (view.equals("guide") == false && view.equals("list") == false && view.equals("picture") == false && view.equals("compare") == false) {
				view = "picture";
			}
/*			if (cookie == null) cookie = new Cookie(AppConfig.CookieListState, view);
			else cookie.setValue(view);
			cookie.setMaxAge(30 * 24 * 60 * 60);
			cookie.setPath("/");
			cookie.setSecure(true);
			//cookie.setHttpOnly(true);
			cookie.setValue("SameSite=Strict;");
			res.addCookie(cookie);*/
			String strCookie = String.format("%s=%s; max-age=%s; Path=/; HttpOnly; Secure; SameSite=Strict;",
					AppConfig.CookieListState, view, 30 * 24 * 60 * 60);
			res.addHeader("Set-Cookie", strCookie);
		} else {
			String strCookie = "picture";
			if (cookie != null) strCookie = cookie.getValue();
			if (strCookie == null || strCookie.isEmpty()) {
				view = "picture";
/*				if (cookie == null) cookie = new Cookie(AppConfig.CookieListState, view);
				else cookie.setValue(view);
				cookie.setMaxAge(30 * 24 * 60 * 60);
				cookie.setPath("/");
				cookie.setSecure(true);
				//cookie.setHttpOnly(true);
				cookie.setValue("SameSite=Strict;");
				res.addCookie(cookie);*/
				String strC = String.format("%s=%s; max-age=%s; Path=/; HttpOnly; Secure; SameSite=Strict;",
						AppConfig.CookieListState, view, 30 * 24 * 60 * 60);
				res.addHeader("Set-Cookie", strC);
			} else {
				view = strCookie;
			}
		}

		String[] tmp = request.getRequestURI().split("/");
		if (tmp != null) {
			int cnt = 0;
			boolean isFind = false;
			for(String str: tmp) {
				if (str.equals("webcatalog")) {
					lang = tmp[cnt+1];
					isFind = true;
					break;
				}
				cnt++;
			}
			if (isFind == false) {
				for(String str: tmp) {
					if (str != null && str.isEmpty() == false) {
						lang = str;
						isFind = true;
						break;
					}
				}
			}
		}
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		Boolean isActive = true;
		if (isTestSite) {
			m = ModelState.TEST;
			isActive = null;
		}

		log.debug("get === lang=== lang="+lang);
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
		Locale baseLocale = LibHtml.getLocale(baseLang);

		if (se_id != null && se_id.isEmpty() == false && seriesService.isModelNumberExists(se_id, m, isActive, err) == false) {
			if (se_id2 != null && se_id.isEmpty() == false) {
				if (seriesService.isModelNumberExists(se_id+"/"+se_id2, m, isActive, err) == false) {
					log.debug("ModelNumber Empty! seid=" + se_id+"/"+se_id2);
					throw new ResponseStatusException(
							  HttpStatus.NOT_FOUND, "ModelNumber not found"
							);
				} else {
					se_id += "/"+se_id2; // ex)IP8_00/IP8_01
				}
			} else {
				log.debug("ModelNumber Empty! seid=" + se_id);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ModelNumber not found"
						);
			}
		}
		if (slug != null && slug2 != null && se_id != null) {
			Series tmpS = seriesService.getFromModelNumber(se_id, ModelState.PROD, err);
			if (err.isError()) {
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ModelNumber not found"
						);
			} else if (tmpS == null) {
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ModelNumber not found."
						);
			} else if (tmpS.isActive() == false) {
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ModelNumber not found"
						);
			} else {
				if (baseLang.equals(tmpS.getLang()) == false) {
					throw new ResponseStatusException(
							  HttpStatus.NOT_FOUND, "ModelNumber not found. Different Lang."
							);
				}
				if (tmpS.isActive() == false) {
					throw new ResponseStatusException(
							  HttpStatus.NOT_FOUND, "ModelNumber not found. Not Active."
							);
				}
			}
			ret = html.getFileFromHtml( baseLang+"/" + slug + "/" + slug2 + "/" +se_id+ "/index.html");
			if (ret == null || ret.isEmpty()) {
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "ModelNumber not found"
						);
			}
			String backUrl = request.getHeader("REFERER");
			if (backUrl != null && backUrl.isEmpty() == false ) {
				String mes = messagesource.getMessage("button.back.to.list", null,  baseLocale);
				if (ret.indexOf("<main ") > -1) { // 2026
					ret = StringUtils.replace(ret, "$$$backUrl$$$",
							SeriesHtml._BackToList.replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
				} else {
					ret = StringUtils.replace(ret, "$$$backUrl$$$", "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes));
				}
			} else {
				ret = StringUtils.replace(ret, "$$$backUrl$$$", ""); // ボタン自体消す
			}
		} else if (slug != null && slug2 != null) {
			Category c2 = service.getFromSlug(slug2, baseLang, m, CategoryType.CATALOG, 2, isActive, err);
			Category c = service.getFromSlug(slug, baseLang, m, CategoryType.CATALOG, 1, isActive, err);
			if (action != null && action.isEmpty() == false) {
				// narrowdown 検索結果
				Template t = templateService.getTemplateFromBean(baseLang, m); 
				if (err.isError()) {
					log.error("ErrorObject:msg="+err.getMessage());
					throw new ResponseStatusException(
							  HttpStatus.NOT_FOUND, "ErrorObject!");
				}
				TemplateCategory tc = null;
				tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, c.getId());
				if (tc == null) {
					throw new ResponseStatusException(
							  HttpStatus.NOT_FOUND, "Category not found. slug = "+slug
							);
				}
				boolean is2026 = tc.is2026();
				ret = t.getHeader();
				String temp = tc.getTemplate();
				
				String title = AppConfig.SearchResultTitleList[0]; // 検索結果タイトル文字
				if (lang.indexOf("en-") > -1) {
					title = AppConfig.SearchResultTitleList[1];
				} else if (lang.indexOf("zh-") > -1) {
					title = AppConfig.SearchResultTitleList[2];
				}
				String strHitTitle = AppConfig.SearchResultHitCountTitleList[0]; // 123件
				if (lang.indexOf("en-") > -1) {
					strHitTitle = AppConfig.SearchResultHitCountTitleList[1];
				} else if (lang.indexOf("zh-") > -1) {
					strHitTitle = AppConfig.SearchResultHitCountTitleList[2];
				}
				
				// 検索結果取得
				List<Series> list = null;
				if (action == null || action.isEmpty()) {
					list = seriesService.listSlug(c2, true, err); // カテゴリ一覧では「非公開」は抜く。シリーズ単体なら表示可能
				} else {
					// 絞り込み検索結果
					list = narrowDownService.getNarrowDown(c2.getId(), request, err);
					if (list == null && err.getCode().equals(ErrorCode.E10005)) {// 絞り込み条件がカラの場合
						list = seriesService.listSlug(c2, true, err); // 全表示
					}
				}

				if (is2026) {
					StringBuilder catpan = new StringBuilder();
					catpan.append("<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\" title=\"\"></li>");
					catpan.append("<a class=\"breadcrumb-item\" href='").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(slug).append("'>").append(c.getName()).append("</a>");
					catpan.append("<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\" title=\"\"></li>");
					catpan.append("<a class=\"breadcrumb-item\" href='").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(slug).append("/").append(slug2).append("'>").append(c2.getName()).append("</a>");
					catpan.append("<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\" title=\"\"></li>");
					catpan.append(title);

					temp = temp.replace("$$$catpan$$$", tc.getCatpan().replace("$$$catpan_title$$$", catpan));
				} else {
					StringBuilder catpan = new StringBuilder();
					catpan.append("<a href='").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(slug).append("'>").append(c.getName()).append("</a>")
					.append("&nbsp;»&nbsp;")
					.append("<a href='").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(slug).append("/").append(slug2).append("'>").append(c2.getName()).append("</a>");
					temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", catpan));
				}
				String sidebar = tc.getSidebar();
				List<Category> cList = service.listAll(c.getLang(), c.getState(), c.getType(), err);
				List<Category> setCategoryList = new LinkedList<>();
				for(Category cate :  cList) {
					Category setC = service.getWithSeries(cate.getId(), null, err);
					if (setC != null) setCategoryList.add( setC );
				}
				// 2024/10/24 絞り込み検索の検索部分
				// 2025/11/25 GETパラメータを減らすためnarrowKeyからnarrowCntへ
				if (is2026) {
					String narrowDown = html.getNarrowDown2026(c.getLang(), c, c2, request);
					sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", narrowDown);
				} else {
					String narrowDown = html.getNarrowDown(c.getLang(), c, c2, request);
					sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", narrowDown);
				}
				
				if (is2026) {
					List<String> category = html.getCategoryMenu2026(lang, c.getId(), c2.getId(), setCategoryList);
					sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
					if (category.size() > 1) {
						sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
					} else {
						sidebar = StringUtils.replace(sidebar,"$$$category2$$$","");
					}
					temp = StringUtils.replace(temp,"$$$sidebar$$$", sidebar);
					
					int ndCnt = 0;
					if (key != null && key.length > 0) ndCnt = key.length; 
					else if (narrowDownCount != null) ndCnt = Integer.parseInt(narrowDownCount);
					else if (nCnt != null && ndCnt == 0) ndCnt = Integer.parseInt(nCnt);

					String viewStr = "";
					if (list != null && list.size() > 0) {
						viewStr = html.getListDisplaySelection2026(lang, c2, view, action, ndCnt, request); // 検索窓下のリスト表示種別選択（一覧、画像、仕様比較）
					}
					
					String t1 = c2.getName().substring(0, 1);
					String t2 = c2.getName().substring(1);
					temp = StringUtils.replace(temp,"$$$h1box$$$", tc.getH1box().replace("$$$title21$$$",t1).replace("$$$title22$$$",t2) + viewStr);
				} else {
					List<String> category = html.getCategoryMenu(lang, c.getId(), c2.getId(), setCategoryList);
					sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
					if (category.size() > 1) {
						sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
					} else {
						sidebar = StringUtils.replace(sidebar,"$$$category2$$$","");
					}
					temp = StringUtils.replace(temp, "$$$sidebar$$$", sidebar);
					temp = StringUtils.replace(temp, "$$$formbox$$$", tc.getH1box().replace("$$$title$$$",c2.getName())); // ↓小カテゴリは逆
				}
				// 2024/11/05 表示形式 一覧、写真、比較
				if (view == null || view.isEmpty()) {
					if (cookie == null) view = "picture";
					else {
						view = cookie.getValue();
						if (view == null) view = "picture";
						else if (view.equals("picture") == false && view.equals("list") == false && view.equals("compare")) view = "picture";
						else if (c2.isCompare() == false && view.equals("compare")) view = "picture";
					}
				} else {
					if (c2.isCompare() == false && view.equals("compare")) view = "picture";
				}
				// 
				if (is2026) {
					// viewStrはh1boxの下
					temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox());
				} else {
					int ndCnt = 0;
					if (key != null && key.length > 0) ndCnt = key.length; // narrowKeyが無くなれば削除！2025/11/25
					else if (narrowDownCount != null) ndCnt = Integer.parseInt(narrowDownCount);
					String viewStr = html.getListDisplaySelection(lang, c2, view, action, ndCnt, request); // 検索窓下のリスト表示種別選択
					temp = StringUtils.replace(temp,"$$$h1box$$$", tc.getFormbox() + viewStr); // ↑小カテゴリは逆
				}
				
				StringBuilder content = new StringBuilder();
				SeriesHtml sHtml = new SeriesHtml(LibHtml.getLocale(baseLang), messagesource, omlistService, faqRepo);
				if (list != null && list.size() > 0) {
					content.append("<div class=\"p_block\">");
					if (view.equals("list")) {
						for(Series s: list) {
							s.setLink(seriesService.getLink(s.getId(), err));
							if (is2026) {
								content.append( sHtml.getGuide2026(s, c, c2, request.getRequestURI(), c.getLang(), false, false));
								content.append( "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>");
							} else {
								content.append( sHtml.get(s, c, c2, request.getRequestURI(), c.getLang(), false, false));
							}
						}
					} else if (view.equals("compare")) {
						// narrow_down_compare削除後不要。2025/11/25
//						List<NarrowDownCompare> compareList = narrowDownService.getCategoryCompare(c2.getId(), true, err);
						HashMap<String, List<NarrowDownValue>> map = new HashMap<>();
						for (Series s : list) {
							List<NarrowDownValue> valList = narrowDownService.getCategorySeriesValue(c2.getId(), s.getId(), true, err);
							map.put(s.getId(), valList);
						}
						List<NarrowDownColumn> colList = narrowDownService.getCategoryColumn(c2.getId(), true, err);
						if (is2026) {
							content.append( sHtml.getCompareHtml2026(lang, baseLang, c, c2, colList, list, map, request));
						} else {
							content.append( sHtml.getCompareHtml(lang, baseLang, c, c2, colList, list, map, request));
						}
					} else {
						if (is2026) {
							content.append( sHtml.getPictureList2026(c, c2, list));
						} else {
							content.append( sHtml.getPictureList(c, c2, list));
						}
					}
					content.append( "</div>");
				} else {
					
					content.append("<div class=\"f fclm gap-24 mb48\">\r\n")
							.append( "                            <div class=\"f fm gap-16\">\r\n")
							.append( "                              <div class=\"text-2xl fw6 leading-tight\">").append(title).append("</div>\r\n")
							.append( "                              <div class=\"badge large filled\">0").append(strHitTitle).append("</div>\r\n")
							.append( "                            </div>\r\n")
							.append( "                            <div class=\"f fh border boder-base-stroke-subtle h160 w-full bg-base-container-accent\"><span class=\"fw5 s-px16 s-text-center m-px16 m-text-center\">");
					if (baseLang.indexOf("en-") > -1) {
						content.append("There were no series that matched the criteria.");
					} else if(baseLang.equals("zh-tw")){
						content.append("沒有符合標準的系列。");
					} else if (baseLang.indexOf("zh-") > -1) {
						content.append( "没有符合标准的系列。");
					} else {
						content.append( "条件に一致したシリーズがありませんでした。");
					}
					content.append("  </span></div>\r\n");
					content.append( "</div>\r\n");
				}
				temp = StringUtils.replace(temp, "$$$content$$$", content.toString());
				ret+=temp;
				ret+= SeriesHtml._seriesCadModal;
				ret+=t.getFooter();
			} else {
				//  narrowdown 未検索
				// viewがカラはPicture。
				if (view == null || view.isEmpty()) {
					ret = html.getFileFromHtml( baseLang+"/" + slug + "/" + slug2 + "/picture.html"); // picture.htmlが写真一覧
				} else if (view.equals("picture")) {
					ret = html.getFileFromHtml( baseLang+"/" + slug + "/" + slug2 + "/picture.html"); // picture.htmlが写真一覧
				} else if (view.equals("list")) {
					ret = html.getFileFromHtml( baseLang+"/" + slug + "/" + slug2 + "/index.html"); // index.htmlはguide一覧
				} else if (c2 != null && c2.isCompare() && view.equals("compare")) {
					ret = html.getFileFromHtml( baseLang+"/" + slug + "/" + slug2 + "/compare.html"); // compare.htmlは仕様比較
				} else {
					ret = html.getFileFromHtml( baseLang+"/" + slug + "/" + slug2 + "/picture.html"); // picture.htmlが写真一覧
				}
			}
			if (ret == null || ret.isEmpty()) {
				log.error("Bad slug or slug2 is Empty! slug=" + slug + " slug2=" + slug2);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "html.getFileFromHtml() not found. path="+baseLang+"/" + slug + "/" + slug2 + "/index.html"
						);
			}
		} else if (slug != null && slug2 == null) {
			ret = html.getFileFromHtml(baseLang+"/" + slug +"/index.html");
			if (ret == null || ret.isEmpty()) {
				log.error("Bad slug is Empty! slug=" + slug);
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "html.getFileFromHtml() not found. path="+baseLang+"/" + slug +"/index.html"
						);
			}
		} else {
			ret = html.getFileFromHtml(baseLang+"/index.html");
			if (ret == null || ret.isEmpty()) {
				log.error("getFileFromHtml() is Empty! path=" + baseLang+"/index.html");
				throw new ResponseStatusException(
						  HttpStatus.NOT_FOUND, "html.getFileFromHtml() not found. url="+baseLang+"/index.html"
						);
			}
		}
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		log.debug("end ===== uri="+request.getRequestURI());
		return ret;
	}

	// 3DCADのCadenasからの戻り
	@GetMapping(value={"/3dcad/cadenas/{lang}/"}, produces="text/html;charset=UTF-8")
	public String getCadenas3DCAD(@PathVariable(name = "lang", required = true) String lang,
			@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "ppath", required = false) String ppath,
			@RequestParam(name = "series", required = false) String seriesName,
			@RequestParam(name = "cat", required = false) String cat,
			@RequestParam(name = "mode", required = false) String mode,
			HttpServletRequest request) {
		String ret = "";
		ErrorObject err = new ErrorObject();
		Lang langObj = langService.getFromContext(lang);

		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		if (lang ==  null || lang.isEmpty()) {
			lang = "ja-jp";
		} else if (lang.equals("ja")) {
			lang = "ja-jp";
		} else if (lang.equals("en")) {
			lang = "en-jp";
		}
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		Boolean isActive = true;
		if (isTestSite) {
			m = ModelState.TEST;
			isActive = null;
		}

		Template t = templateService.getTemplateFromBean(baseLang, m);
		if (err.isError()) {
			log.error("ErrorObject:msg="+err.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "ErrorObject!");
		}

		// ppath url1の部分一致
		List<Cad3d> list = cad3dService.searchUrl(ppath, lang);

		// newmsgがあれば代替品表示 newidsを全部表示
		String message = "";
		String kw = "";
		List<Series> newList = new LinkedList<Series>();
		List<Series> sList = new LinkedList<Series>();
		for(Cad3d c : list) {
			if (c.getNewmsg() != null && c.getNewmsg().isEmpty() == false) {
				message = c.getNewmsg();
				kw = c.getSeries();
				String[] arr = c.getNewids().replace("【", "").split("】");
				for(String str : arr) {
					if (str.trim().isEmpty() == false) {
						Series s = seriesService.getFromModelNumber(str.trim(), ModelState.PROD, err);
						if (s != null) {
							newList.add(s);
						}
					}
				}
			} else {
				kw = c.getSeries();
				String[] arr = c.getIds().replace("【", "").split("】");
				for(String str : arr) {
					if (str.trim().isEmpty() == false) {
						Series s = seriesService.getFromModelNumber(str.trim(), ModelState.PROD, err);
						if (s != null) {
							sList.add(s);
						}
					}
				}
			}
 		}
		if (newList.size() > 0) {
			TemplateCategory tc = getTemplateCategoryFromSeries(newList, langObj, ModelState.PROD, err);

			if (tc.is2026()) {
				SeriesHtml sHtml = new SeriesHtml(LibHtml.getLocale(baseLang), messagesource, omlistService, faqRepo);
				ret = html.getSearchResult2026(kw, baseLang,  t, tc, service, seriesService, sHtml, newList, 0, -1, message, isTestSite);
			} else {
				ret = html.getSearchResult(kw, baseLang,  t, tc, service, newList, 0, -1, message);
			}
			ret = StringUtils.replace(ret, "$$$backUrl$$$", ""); // ボタン自体消す
		} else {
			// newmsgが無ければ通常表示。idsを全部表示
			TemplateCategory tc = null;
			if (sList == null || sList.size() == 0) {
				Category cate = service.getLang(baseLang, ModelState.PROD, CategoryType.CATALOG, true, err);
				if (cate != null) {
					tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, cate.getId());
				}
				if (tc == null) {
					Category root = service.getWithChildren(cate.getParentId(), true, err);
					for(Category c : root.getChildren()) {
						tc = templateCategoryService.findByCategoryIdFromBean(baseLang, m, c.getId());
						if (tc != null) {
							break;
						}
					}

				}
			} else {
				tc = getTemplateCategoryFromSeries(sList, langObj, ModelState.PROD, err);
			}
			if (tc.is2026()) {
				SeriesHtml sHtml = new SeriesHtml(LibHtml.getLocale(baseLang), messagesource, omlistService, faqRepo);
				ret = html.getSearchResult2026(kw, baseLang,  t, tc, service, seriesService, sHtml, sList, 0, -1, message, isTestSite);
			} else {
				ret = html.getSearchResult(kw, baseLang,  t, tc, service, sList, 0, -1, message);
			}
		}

		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		return ret;
	}

	// 3DCADのポップアップ表示
	@GetMapping(value={"/3dcad/{lang}/"}, produces="text/html;charset=UTF-8")
	public String get3DCAD(@PathVariable(name = "lang", required = true) String lang,
			@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "series", required = false) String seriesName,
			@RequestParam(name = "cat", required = false) String cat,
			@RequestParam(name = "mode", required = false) String mode,
			@RequestParam(name = "version", required = false) String version,
			HttpServletRequest request) {
		StringBuilder ret = new StringBuilder();
		ErrorObject err = new ErrorObject();
		Lang langObj = langService.getFromContext(lang);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}
		String shortLang = "ja";
		if (lang.indexOf("en-") > -1) {
			shortLang = "en";
		} else if (lang.indexOf("zh-") > -1) {
			shortLang = "zh";
		}
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		if (isTestSite && version == null) version = "2026";

		List<Cad3d> list = null;
		Map<String,Cad3d> catmap = new LinkedHashMap<String,Cad3d>();

		try {
			Series series =seriesService.getFromModelNumber(id, ModelState.PROD, err);
			if (series == null) {
				if (version == null || version.equals("2026") == false) {
					ret.append(SeriesHtml._3dCadFrameHtml);
					ret.append( "<span class=\"error\">").append(messagesource.getMessage("web.manual.empty", null, LibHtml.getLocale(lang))).append("</span>");
					ret.append( SeriesHtml._3dCadFrameHtmlEND);
				} else {
					// 2026デザイン
					ret.append( SeriesHtml._3dCadFrameHtml_2026.replace("$$$TITLE$$$", "2D/3D CAD"));
					ret.append( "<span class=\"text-red\">").append(messagesource.getMessage("web.manual.empty", null, LibHtml.getLocale(lang))).append("</span>");
					ret.append( SeriesHtml._3dCadFrameHtmlEND_2026);
				}
			} else {
				String escape = id;
				if (id.indexOf('(') > -1) {
					escape = id.replace("(", "\\(").replace(")", "\\)");
				}
				if(mode != null && mode.equals("bycat")){
					list = cad3dService.search("【"+escape+"】", seriesName, lang, cat);

	            } else {
	            	list = cad3dService.search("【"+escape+"】", null, lang, null);
	            	//分類があるかどうか
		            for(Cad3d _c3d:list){
		            	if(StringUtils.isNotEmpty(_c3d.getCat())){
		            		catmap.put(_c3d.getSeries()+"\t"+_c3d.getCat(), _c3d);
		            	}
		            }
	            }
				if (list == null || list.size() == 0) {
					if (version == null || version.equals("2026") == false) {
						ret.append( SeriesHtml._3dCadFrameHtml);
						ret.append( "<span class=\"error\">").append(messagesource.getMessage("web.manual.empty", null, LibHtml.getLocale(lang))).append("</span>");
						ret.append( SeriesHtml._3dCadFrameHtmlEND);
					} else {
						// 2026デザイン
						ret.append( SeriesHtml._3dCadFrameHtml_2026.replace("$$$TITLE$$$", "2D/3D CAD"));
						ret.append( "<span class=\"text-red\">").append(messagesource.getMessage("web.manual.empty", null, LibHtml.getLocale(lang))).append("</span>");
						ret.append( SeriesHtml._3dCadFrameHtmlEND_2026);
					}
				} else {

		            //分類
		            String result_type = "RESULT";
		            if(catmap.size()>0){
		            	result_type = "CATMAP";
		            }
		            //mode=bycatのときは通常検索
		            if(mode != null && mode.equals("bycat")){
		            	result_type ="RESULT";
		            }
		            /* cad3d.title.series=シリーズ
		            cad3d.title.category=分類
		            cad3d.title.model=型式
		            cad3d.title.name=名称
		            cad3d.title.download=ダウンロード
		            cad3d.list.detail=CADライブラリへ
					cad2d.list.detail=検索結果を表示
					cad3d.back.link=↑分類一覧に戻る */
		            String detail = messagesource.getMessage("cad3d.list.detail", null, LibHtml.getLocale(lang));

		            if (version == null || version.equals("2026") == false) {
			            ret.append( SeriesHtml._3dCadFrameHtml);
		            	ret.append( "<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl resulttbl2\">\r\n" +
								"  <tbody>\r\n");
		            } else {
		            	// 2026デザイン
			            ret.append( SeriesHtml._3dCadFrameHtml_2026.replace("$$$TITLE$$$", "2D/3D CAD"));
		            	ret.append( "<div class=\"w-full overflow-x-auto\">\r\n")
		            		.append( "             <table class=\"table-hover s-full border-bottom border-right border-base-stroke-default border-collapse-collapse\">\r\n")
		            		.append( "               <thead>");
		            }

					if (result_type.equals("CATMAP")) {

			            String strSeries = messagesource.getMessage("cad3d.title.series", null, LibHtml.getLocale(lang));
			            String strCategory = messagesource.getMessage("cad3d.title.category", null, LibHtml.getLocale(lang));
			            
			            if (version == null || version.equals("2026") == false) {
			            	ret.append( "<tr>\r\n")
			            		.append("    <th>").append(strSeries).append("</th>\r\n")
			            		.append("    <th>").append(strCategory).append("</th>\r\n")
			            		.append("    <th>&nbsp;</th>\r\n")
			            		.append("</tr>");
			            } else {
			            	// 2026デザイン
			            	ret.append( "<tr>\r\n")
				            	.append("     <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">").append(strSeries).append("</th>\r\n")
				            	.append("     <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">").append(strCategory).append("</th>\r\n")
				            	.append("     <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">&nbsp;</th>\r\n")
				            	.append("</tr></thead><tbody>\r\n");
			            }
						Set<String> keys = catmap.keySet();
						for(String k : keys) {
							String[] arr = k.split("\t");
							if (arr.length >= 2) {
								if (version == null || version.equals("2026") == false) {
									ret.append("<tr>\r\n");
									ret.append("<td>"+arr[0]+"</td>");
									ret.append("<td>"+arr[1]+"</td>");
									ret.append("<td class=\"tdc\"><a href=\"./?mode=bycat&id=").append(id).append("&series=").append(arr[0]).append("&cat=").append(arr[1]).append("\">").append(detail).append("</a></td>");
									ret.append("</tr>\r\n");
								} else {
					            	// 2026デザイン
									ret.append("<tr>\r\n");
									ret.append("<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">").append(arr[0]).append("</td>");
									ret.append("<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">").append(arr[1]).append("</td>");
									ret.append("<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">")
										.append( "<div class=\"f fc\">")
										.append( "  <a class=\"f fm gap-4\" target=\"_blank\" href=\"./?mode=bycat&id="+id+"&series=").append(arr[0]).append("&cat=").append(arr[1]).append("\">")
										.append( "    <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">").append(detail).append("</span>")
										.append( "  </a>\r\n")
										.append( "</div>\r\n")
										.append( "</td>\r\n");
									ret.append("</tr>\r\n");
								}
							}
						}
					} else {
						String strSeries = messagesource.getMessage("cad3d.title.series", null, LibHtml.getLocale(lang));
			            String strModel = messagesource.getMessage("cad3d.title.model", null, LibHtml.getLocale(lang));
			            String strName = messagesource.getMessage("cad3d.title.name", null, LibHtml.getLocale(lang));
			            String strDownload = messagesource.getMessage("cad3d.title.download", null, LibHtml.getLocale(lang));
			            String strBack = messagesource.getMessage("cad3d.back.link", null, LibHtml.getLocale(lang));

			            if(mode != null && mode.equals("bycat")){
			            	ret.append("<p align=\"right\"><a href=\"./?id=").append(id).append("\">").append(strBack).append("</a></p>\r\n" );
			            }
			            if (version == null || version.equals("2026") == false) {
			            	ret.append( "<tr>\r\n" )
				            	.append("    <th>").append(strSeries).append("</th>\r\n" )
				            	.append("    <th>").append(strModel).append("</th>\r\n" )
				            	.append("    <th>").append(strName).append("</th>\r\n" )
				            	.append("    <th>").append(strDownload).append("</th>\r\n")
				            	.append("</tr>\r\n");
			            } else {
			            	// 2026デザイン
			            	ret.append( "<tr>\r\n")
				            	.append("    <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">"+strSeries+"</th>\r\n" )
				            	.append("    <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">"+strModel+"</th>\r\n" )
				            	.append("    <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">"+strName+"</th>\r\n" )
				            	.append("    <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">"+strDownload+"</th>\r\n")
				            	.append("</tr></thead><tbody>\r\n");
			            }
			            for(Cad3d cad : list) {
			            	if (version == null || version.equals("2026") == false) {
				            	ret.append("<tr>\r\n");
								ret.append("<td>").append(cad.getSeries()).append("</td>");
								ret.append("<td>").append(cad.getItem()).append("</td>");
								ret.append("<td>").append(cad.getName()).append("</td>");
								ret.append("<td class=\"tdc\"><a href=\"").append(cad.getUrl1()).append("\" target=\"_blank\"><img src=\"").append(AppConfig.ProdRelativeUrl).append("images/").append(shortLang).append("/bt_to_3dcad.jpg\"></a></td>");
								ret.append("</tr>\r\n");
			            	} else {
				            	// 2026デザイン
			            		ret.append("<tr>\r\n");
								ret.append("<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">"+cad.getSeries()+"</td>");
								ret.append("<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">"+cad.getItem()+"</td>");
								ret.append("<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">"+cad.getName()+"</td>");
								ret.append("<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">")
									.append( "  <div class=\"f fc\">")
									.append( "    <a class=\"f fm gap-4\" target=\"_blank\" href=\"").append(cad.getUrl1()).append("\" >")
									.append( "      <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">").append(detail).append("</span><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\"\">")
									.append( "    </a>")
									.append("  </div>")
									.append( "</td>");
								ret.append("</tr>\r\n");
			            	}
			            }
					}
					if (version == null || version.equals("2026") == false) {
						ret.append("</tbody></table>");
						ret.append( SeriesHtml._3dCadFrameHtmlEND);
					} else {
						// 2026デザイン
						ret.append("</tbody></table>");
						ret.append( SeriesHtml._3dCadFrameHtmlEND_2026);
					}
	            }
			}
		} catch (Exception e) {
			throw new DataAccessException(messagesource.getMessage("web.page.empty", null, LibHtml.getLocale(lang)));
		}

		return ret.toString();
	}

	// 2DCADのポップアップ表示
	@GetMapping(value={"/2dcad/{lang}/{seid}", "/2dcad/{lang}/{seid}/{seid2}"}, produces="text/html;charset=UTF-8")
	public String get2DCAD(@PathVariable(name = "lang", required = false) String lang,
			@PathVariable(name = "seid") String seid,
			@PathVariable(name = "seid2", required = false) String seid2,
			@PathVariable(name = "page", required = false) String page,
			@RequestParam(name = "version", required = false) String version,
			HttpServletRequest request) {

		StringBuilder ret = new StringBuilder();
		String detail = messagesource.getMessage("cad2d.list.detail", null, LibHtml.getLocale(lang)); // ボタン

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
		Locale baseLocale = LibHtml.getLocale(baseLang);
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		if (isTestSite) m = ModelState.TEST;
		Boolean isActive = true;
		if (isTestSite) {
			isActive = null;
			version = "2026";
		}

		SeriesHtml sHtml = new SeriesHtml(baseLocale, messagesource, null, null);
		try {
			Series series = seriesService.getFromModelNumber(seid, m, err);
			if (series == null && seid2 != null && seid2.isEmpty() == false) {
				series = seriesService.getFromModelNumber(seid+"/"+seid2, m, err);
			}

			List<String[]> links = sHtml.getGuideIDLinks(series.getModelNumber(), series.getSpec(), 0);

			if (links != null && links.size() > 1) {
				if (version == null || version.equals("2026") == false) {
					ret.append( SeriesHtml._2dCadFrameHtml);
				} else {
					ret.append( SeriesHtml._3dCadFrameHtml_2026.replace("$$$TITLE$$$", "2D CAD"));
					ret.append( "<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n")
						.append( "<thead>\r\n");
				}
				ret.append( "<tr>\r\n" );
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					if (version == null || version.equals("2026") == false) {
						if (cnt == titles.length-1) {
							ret.append(  "<th class=\"last\">").append( title ).append("</th>\r\n" );
						} else {
							ret.append(  "<th scope=\"col\">").append( title ).append( "</th>\r\n" );
						}
					} else {
						ret.append( "    <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">").append(title).append("</th>\r\n");
					}
					cnt++;
				}
				
				if (version == null || version.equals("2026") == false) {
					ret.append( "</tr>\r\n");
				} else  {
					ret.append( "</tr></thead><tbody>\r\n");
				}
				for (int i = 1; i < links.size(); i++) {
					ret.append( "<tr>\r\n");
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							if (val == null || StringUtils.isEmpty(val)) {
								if (version == null || version.equals("2026") == false) {
									ret.append( "<td class=\"last\">&nbsp;</td>\r\n");
								} else  {
									ret.append( "<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">&nbsp;</td>");
								}
							} else {
								if (version == null || version.equals("2026") == false) {
									ret.append(  "<td class=\"last\">"+
											"<div class=\"win_dlbt_area\">\r\n" +
											"<a href=\"" ).append( val ).append( "\" target=\"_blank\" class=\"plink link_2dcad p_2dcad\">");
									String tmp = "<img src=\""+AppConfig.ProdRelativeUrl+"images/ja/bt_to_2dcad.jpg\" alt=\"2DCAD\"/></a>\r\n" ;
									if (lang.indexOf("en-") > -1) {
										ret.append( tmp.replace("/ja/", "/en/"));
									} else if (lang.equals("zh-cn")) {
										ret.append( tmp.replace("/ja/", "/zh/"));
									} else if (lang.equals("zh-tw")) {
										ret.append( tmp.replace("/ja/", "/en/"));
									} else {
										ret.append( tmp);
									}
									ret.append( "</div>\r\n")
									.append("</td>\r\n");
								} else {
									ret.append("<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">")
										.append( "  <div class=\"f fc\"><a class=\"f fm gap-4\" target=\"_blank\" href=\"").append( val ).append( "\" >")
										.append( "    <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">").append(detail).append("</span><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\"\">")
										.append( "  </a></div>")
										.append( "</td>");
								}
							}
						} else {
							if (version == null || version.equals("2026") == false) {
								ret.append(  "<td>").append( val ).append("</td>\r\n");
							} else {
								ret.append( "<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">").append(val).append("</td>\r\n");
							}
						}
						cnt++;
					}
					ret.append( "</tr>\r\n");
				}
				if (version == null || version.equals("2026") == false) {
					ret.append( SeriesHtml._2dCadFrameHtmlEND);
				} else {
					ret.append( "</tbody></table>\r\n");
					ret.append( SeriesHtml._3dCadFrameHtmlEND_2026);
				}
			}
			if (langObj.isVersion()) {
				// 変換処理
				Template toT = templateService.getTemplateFromBean(lang, m);
				String tmp = html.changeLang(ret.toString(), baseLang, lang, toT.getHeader(), toT.getFooter(), false);
				ret = new StringBuilder(tmp);
			}
		} catch (Exception e) {
			throw new DataAccessException(messagesource.getMessage("web.page.empty", null, LibHtml.getLocale(lang)));
		}

		return ret.toString();
	}
    @GetMapping("/ja-jp/test/headers")
    public ModelAndView getHeaders(
			ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {
		mav.setViewName("/login/admin/headers");
 		return mav;
    }
	// ========== private ==========
    // 2026/3 www以外はtestを取得。ModelStateを追加
	private TemplateCategory getTemplateCategoryFromSearchResult(List<PsItem> items, Lang langObj, ModelState m, Boolean active, ErrorObject err) {
		TemplateCategory ret = null;
		TemplateCategory tc = null;

		List<Series> list = new ArrayList<Series>();
		if (items != null && items.size() > 0) {
			for(PsItem i : items) {
				Series s = seriesService.getFromModelNumber(i.getSid(), m, err);
				if (s != null && s.isActive()) {
					list.add(s);
					if (list.size() > 2) break; // 重くなると困るので。2022/10/24
				}
			}
		}
		if (list.size() > 0) {
			tc = getTemplateCategoryFromSeries(list, langObj, m, err);
			if (tc != null) ret = tc;
		}
		return ret;
	}
	private TemplateCategory getTemplateCategoryFromSeries(List<Series> list, Lang langObj, ModelState m, ErrorObject err) {
		TemplateCategory ret = null;
		TemplateCategory tc = null;
		try {
			for(Series se : list) {
				Series series = seriesService.getWithCategory(se.getId(), true, err); // TODO 全部取る必要はない
				if (series != null) {
					List<CategorySeries> cList = series.getCategorySeries();
					for(CategorySeries cs : cList) {
						tc = templateCategoryService.findByCategoryIdFromBean(series.getLang(), series.getState(), cs.getCategoryId());
						if (tc == null) {
							List<Category> cateList = service.listAll(langObj.getLang(), m, CategoryType.CATALOG, err);
							for(Category c : cateList) {
								if (c.getId().equals(cs.getCategoryId()) && c.getParentId() != null) {
									tc = templateCategoryService.findByCategoryIdFromBean(langObj.getLang(), m, c.getParentId());
									if (tc != null) break;
								}
							}
						}
						if (tc != null) break;
					}
					if (tc != null) break;
				}
			}
		} catch (Exception e) {
			log.error("getTemplateCategoryFromSeries() Exception!",e);
		}
		if (tc != null) ret = tc;
		else {
			// 最初のtemplateCategoryを取得
			ret = templateCategoryService.findByLangFromBean(langObj.getLang(), m);
		}
		return ret;
	}



}
