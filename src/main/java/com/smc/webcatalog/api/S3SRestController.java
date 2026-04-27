package com.smc.webcatalog.api;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smc.psitem.service.PsItemService;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateCategoryService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.AccessTokenResult;
import com.smc.webcatalog.util.Candidate;
import com.smc.webcatalog.util.JpServiceResult;
import com.smc.webcatalog.util.JpServiceUtil;
import com.smc.webcatalog.util.LibHtml;
import com.smc.webcatalog.util.LibSynonyms;
import com.smc.webcatalog.util.S3SClient;
import com.smc.webcatalog.util.S3SDetailResult;
import com.smc.webcatalog.util.S3SDetailResult.Accessories;
import com.smc.webcatalog.util.S3SDetailResult.Prop;
import com.smc.webcatalog.util.S3SDetailResult.SuitableSeries;
import com.smc.webcatalog.util.S3SPartialMatchResult;
import com.smc.webcatalog.util.S3SPartialMatchResultData;
import com.smc.webcatalog.util.S3SResult;
import com.smc.webcatalog.util.S3SSearchResult;
import com.smc.webcatalog.util.S3SSeriesInfo;

import lombok.extern.slf4j.Slf4j;
import net.arnx.jsonic.JSON;

@RestController
@RequestMapping("/s3s")
@Slf4j
public class S3SRestController {

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
    MessageSource messagesource;

	@Autowired
	LibHtml html;

	static int PAGE_MAX = 100; // 1ページあたりの最大数
	
	// 部分一致検索
	// gsearchからの一覧へのリンク
	@GetMapping(value={"/{lang}/search/", "/{lang}/search"})
	public String getSearch3S(@PathVariable(name = "lang", required = true) String lang,
			@RequestParam(name = "kw", required = true) String kw,
			@RequestParam(name = "cd", required = false, defaultValue = "1") String cd,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "limit", required = false) String limit,
			HttpServletRequest request) {
		String ret = null;
		ErrorObject err = new ErrorObject();

		boolean isNoHit = false; // 検索結果0件
		
		int intLimit = PAGE_MAX; // デフォルト
		int intPage = 1; 
		if (limit != null) {
			try {
				intLimit = Integer.parseInt(limit);
				if (intLimit < 100) intLimit = 100;
			}catch (Exception e) {
				log.error("getSearch3S() limit.parse exception. e="+e.getMessage() );
			}
		}
		if (page != null) {
			try {
				intPage = Integer.parseInt(page);
				if (intPage < 1) intPage = 1;
			} catch (Exception e) {
				log.error("getSearch3S() page.parse exception. e="+e.getMessage() );
			}
		}
		Lang langObj = langService.getLang(lang, err);
		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		Locale baseLocale = getLocale(baseLang);
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		Boolean isActive = true;
		if (isTestSite) {
			m = ModelState.TEST;
			isActive = null;
		}

		// 3S
		JpServiceUtil util = new JpServiceUtil();

		S3SPartialMatchResult res = null;
		if (kw != null && kw.isEmpty() == false) {
			kw = kw.trim(); // 全件ヒット。2023/5/25
		}
		String kwOriginal = kw;
		if (kw != null && kw.isEmpty() == false && kw.getBytes().length != kw.length()) {
			kw = Normalizer.normalize(kw, Normalizer.Form.NFKC); // 全角英数を半角に
			if (kw.indexOf("ー") > -1) kw = kw.replace("ー", "-");
			if (kw.indexOf("‐") > -1) kw = kw.replace("‐", "-");
			if (kw.indexOf("―") > -1) kw = kw.replace("―", "-");
			if (kw != null && kw.matches("^[A-z0-9-_/+]{1,50}$")) {
				// そのまま
			} else {
				kw = kwOriginal;
			}
		}
		// スペースがあれば区切って、関連語を取得
		// 2023/10/20 当面、日本語のみ
		String header = "";
		String footer = "";
		boolean is2026 = false;
		try {
			List<String> kwList = null;
			if (lang != null && lang.equals("ja-jp")) {
				LibSynonyms synonyms = new LibSynonyms();
				String[] arr = kw.split("[ 　]");
				kwList = synonyms.getSynonyms(arr, lang);
				if (kwList == null) {
					kwList = Arrays.asList(arr);
				}
			} else {
				String[] arr = kw.split("[ 　]");
				kwList = new LinkedList<>();
				kwList.addAll(Arrays.asList(arr));
			}
				
			if (kwList != null) {
				String searchType = "fore";
				if (cd == null || cd.equals("1") == false) {
					searchType = "in";
				}
				res = util.search(request.getServletContext(), kwList, baseLang, intPage, intLimit, searchType);
			} else {
				isNoHit = true;
			}
			if (res != null && res.getHitCount().isEmpty() == false && res.getHitCount().equals("0") == false) {
				int hit = Integer.parseInt(res.getHitCount());
				if (intPage > 1 && hit > 0 && (intPage-1)*intLimit > hit) {
					isNoHit = true;
				}
			}
			Template t = templateService.getTemplateFromBean(baseLang, m);
			header = t.getHeader();
			footer = t.getFooter();

			String numberCheck = "品番確認";
			String strSearch = "フル品番 検索";
			String strSearchResult = "フル品番 検索結果";
			String catpan = "<div class=\"catpan\" style=\"width:1120px;margin:auto;\">\r\n" +
					"製品情報&nbsp;»&nbsp;\r\n" +
					"<a href=\"/webcatalog/{lang}/\">WEBカタログ</a>\r\n" +
					"</div>";
			
			is2026 = t.is2026();
			if (is2026) {
				header += "<main class=\"relative bg-motif s-bg-image-none m-bg-image-none\">\r\n";
				catpan = " <div class=\"container-1600\">\r\n"
						+ "\r\n"
						+ "        <div class=\"px72 py8 s-px16 s-py8 m-px16 m-py8\">\r\n"
						+ "          <nav class=\"breadcrumb leading-normal text-sm\">\r\n"
						+ "            <ol class=\"f fm gap-8 s-text-xs m-text-xs\">\r\n"
						+ "              <li>製品情報\r\n"
						+ "              </li>\r\n"
						+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
						+ "              <li><a class=\"breadcrumb-item\" href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n"
						+ "              </li>\r\n"
						+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
						+ "              <li><a class=\"breadcrumb-item\" href=\"/\">品番確認</a>\r\n"
						+ "              </li>\r\n"
						+ "            </ol>\r\n"
						+ "          </nav>\r\n"
						+ "        </div>\r\n"
						+ "";
			} else {
				catpan = catpan.replace("{lang}", lang);
			}
			if (baseLang.indexOf("ja-") > -1) {
			} else if (baseLang.indexOf("en-") > -1) {
				catpan = catpan.replace("製品情報", "Product Information");
				catpan = catpan.replace("WEBカタログ", "WEB Catalog");
				numberCheck = "Product Number Check";
				catpan = catpan.replace("品番確認", numberCheck);
				strSearch = "Part Number Search";
				strSearchResult = "Search Results";
			} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
				catpan = catpan.replace("製品情報", "产品信息");
				catpan = catpan.replace("WEBカタログ", "产品目录");
				numberCheck = "型号确认";
				catpan = catpan.replace("品番確認", numberCheck);
				strSearch = "全型号搜索";
				
				strSearchResult = "搜索结果";
			}  else if (baseLang.equals("zh-tw")) {
				catpan = catpan.replace("製品情報", "產品信息");
				catpan = catpan.replace("WEBカタログ",  "產品目錄");
				numberCheck = "型號確認";
				catpan = catpan.replace("品番確認", numberCheck);
				strSearch = "全型号搜尋";
				strSearchResult = "搜尋結果";
			}
			header += catpan;
			if (is2026) {
				header += "<div class=\"px72 py8 s-px16 s-py8 m-px16 m-py8\">"
						+ "  <div class=\"container-1600 f fclm w-full gap-24 p24 border s-px16 m-px16\">\r\n"
						+ "    <div class=\"bg-base-container-accent\">\r\n"
						+ "<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/s3s/ja-jp/search/\">\r\n"
						+ "\r\n"
						+ "    <div class=\"row gap-8-24 w-full s-fclm s-gap-16 m-fclm m-gap-16 l-fm\">\r\n"
						+ "        <div class=\"flex-200 fw5 leading-none s-flex-auto s-leading-tight s-fw6 m-flex-auto m-leading-tight m-fw6\">"+strSearch+"</div>\r\n"
						+ "        <div class=\"f fm gap-12 flex-auto-600 s-flex-auto m-flex-auto\"> \r\n"
						+ "            <input type=\"text\" name=\"kw\" value=\""+kw+"\" id=\"kwItem\" class=\"k input h44 ellipsis\" placeholder=\""+strSearch+"\">\r\n"
						+ "            <div class=\"f gap-12 flex-fixed\">\r\n"
						+ "                  <button class=\"button large secondary solid gap-8 m-is-square\" onclick=\"SearchPsItemForm.submit();\">"
						+ "                    <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/search.svg\" alt=\"\" title=\"\">\r\n"
						+ "                    <div class=\"s-hide m-hide\">検索</div>\r\n"
						+ "                  </button>\r\n"
						+ "            </div>\r\n"
						+ "        </div>"
						+ "    </div>"
						+ "</form>"
						+ "    </div>"
						+ "  </div>"
						+ "</div>";
			} else {
				header += "<div class=\"clear\"></div>";
				header += "<div id=\"content\">";
				header += "<div class=\"form_box\">\r\n"
				+ "<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/s3s/ja-jp/search/\">\r\n"
				+ "\r\n"
				+ "    <div class=\"sform search\">\r\n"
				+ "    <label class=\"title\" for=\"k\">"+strSearch+"</label>\r\n"
				+ "      <div class=\"input\">\r\n"
				+ "      <input type=\"text\" name=\"kw\" value=\""+kw+"\" id=\"kwItem\" class=\"k\" style=\"width:600px;\" placeholder=\""+strSearch+"\">\r\n"
				+ "      <input type=\"submit\" value=\"\" class=\"sbt\" onclick=\"SearchPsItemForm.submit();\" >\r\n"
				+ "      </div>"
				+ "    </div>"
				+ "</form>"
				+ "</div>";
			}

			// ページタイトル
			if (is2026) {
				String title = "<div class=\"w-full mb24 px72 py8 s-px16 s-py8 m-px16 m-py8\">\r\n"
						+ "            <div class=\"mb36 m-mb24 s-mb24\">\r\n"
						+ "              <h2 class=\"text-6xl leading-tight fw5 s-fw6 s-text-3xl m-fw6 m-text-3xl\"><span class=\"text-primary\">$$$title21$$$</span><span class=\"text-base-foreground-default\">$$$title22$$$</span></h2>\r\n"
						+ "            </div>";
				title = StringUtils.replace(title, "$$$title21$$$", strSearchResult.substring(0, 1));
				title = StringUtils.replace(title, "$$$title22$$$", strSearchResult.substring(1));
				header += title;
			} else {
				header += "<div style=\"font-weight: bold; font-size: 18px; line-height: 22px;\">"+strSearchResult+"</div>";
			}
			
			if (isNoHit == false && res != null && res.getHitCount().isEmpty() == false && res.getHitCount().equals("0") == false) {
				NumberFormat comFormat = NumberFormat.getNumberInstance();
				int start = (intPage-1) * intLimit + 1;
				int end = intPage * intLimit;
				int hit = Integer.parseInt(res.getHitCount());
				if(end > hit) end = hit;
				
				String strCount = "件";
				String strTotal = "全";
				if (baseLang.indexOf("en-") > -1) {
					strCount = "";
					strTotal = "Total";
				} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
					strCount = "";
					strTotal = "总数";
				} else if (baseLang.equals("zh-tw")) {
					strCount = "";
					strTotal = "總數";
				}
				String strType = "型式・名称";
				String strNumber = "品番確認";
				String strDetail = "詳細";
				if (baseLang.indexOf("en-") > -1) {
					strType = "Product name";
					strNumber = "Download";
					strDetail = "Details";
				} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
					strType = "型号、名称";
					strNumber = "型号确认";
					strDetail = "详情";
				} else if (baseLang.equals("zh-tw")) {
					strType = "型號、名稱";
					strNumber = "型號確認";
					strDetail = "詳情";
				}
				header += "<div style=\"text-align:right;\">"+ start + "-" + end + strCount +" ( "+strTotal+"："+comFormat.format(hit)+ strCount +" )"+"</div>";
				if (is2026) {
					ret = "<table class=\"table\">\r\n"
							+ "                                            <thead>\r\n"
							+ "                                              <tr>\r\n" +
							"			<th class=\"th\" colspan=\"2\">"+strType+"</th>\r\n" +
							"			<th class=\"th w120 text-center\">"+strNumber+"</th>\r\n" +
							"		</tr></thead><tbody>";
					for(S3SPartialMatchResultData d : res.getSearchData()) {
						String path = "/webcatalog/s3s/"+lang + "/frame/"+d.getTypeID()+"/";
						ret += "<tr>\r\n" ;
						if (d.getPicFile() != null && d.getPicFile().isEmpty() == false) {
							ret += "			<td class=\"td text-center\" colspan=\"1\"><span class=\"w100\"><img src=\""+d.getPicFile().replace("{size}", "S")+"\"/></span></td>\r\n" ;
						} else {
							ret += "			<td class=\"td text-center\" colspan=\"1\"></td>\r\n" ;
						}
						ret += "			<td class=\"td text-sm\" colspan=\"1\">"+d.getDescription()+"</td>\r\n" +
							"			<td class=\"td w120 text-center\" colspan=\"1\">\r\n" +
							"					<a class=\"button secondary solid medium\" href=\""+path+"\" target=\"_blank\">"+strDetail+"</a>\r\n" +
							"			</td>\r\n" +
							"		</tr>";
					}
					ret+="</tbody>\r\n"
							+ "</table>\r\n";
				} else {
					ret = "<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
							"		<tr>\r\n" +
							"			<th colspan=\"2\" class=\"\">"+strType+"</th>\r\n" +
							"			<th class=\"last\" style=\"width:80px\">"+strNumber+"</th>\r\n" +
							"		</tr>";
					for(S3SPartialMatchResultData d : res.getSearchData()) {
						String path = "/webcatalog/s3s/"+lang + "/frame/"+d.getTypeID()+"/";
						ret += "<tr>\r\n" ;
						if (d.getPicFile() != null && d.getPicFile().isEmpty() == false) {
							ret += "			<td class=\"\" style=\"padding: 5px;border-right: none;width:55px;height:60px;\"><img src=\""+d.getPicFile().replace("{size}", "S")+"\"/></td>\r\n" ;
						} else {
							ret += "			<td class=\"\" style=\"padding: 5px;border-right: none;width:55px;height:60px;\"></td>\r\n" ;
						}
						ret += "			<td class=\"\" style=\"text-align: left;padding: 5px;\">"+d.getDescription()+"</td>\r\n" +
							"			<td class=\"last\" style=\"text-align: center;\">\r\n" +
							"					<a href=\""+path+"\" target=\"_blank\">"+strDetail+"</a>\r\n" +
							"			</td>\r\n" +
							"		</tr>";
					}
					ret+="</table>";
					if (hit > PAGE_MAX) {
						ret += "<div class=\"navi\" style=\"margin:15px 0 10px 0;text-align:center;\">";
						double db =  Math.ceil((double)hit / PAGE_MAX);
						int s = intPage-5;
						int e = intPage+5;
						if (s <= 0) {e -= s; s=1;}
						if (e > db) e = (int)db;
						if (s < intPage) ret += "<a href=\""+AppConfig.ProdRelativeUrl+"s3s/"+lang+"/search/?kw="+kw+"&page="+(intPage-1)+"\" class=\"back\">&lt;</a>&nbsp;";
						for(; s <= e; s++) {
							if (intPage != s) ret += "<a href=\""+AppConfig.ProdRelativeUrl+"s3s/"+lang+"/search/?kw="+kw+"&page="+ s +"\" class=\"pn\">"+s+"</a>&nbsp;";
							else ret += "<span class=\"pn current\">" + intPage + "</span>&nbsp;";
						}
						if (e > intPage) ret += "<a href=\""+AppConfig.ProdRelativeUrl+"s3s/"+lang+"/search/?kw="+kw+"&page="+(intPage+1)+"\" class=\"fw\">&gt;</a>&nbsp;";
						ret += "</div>\r\n";
					}
				}
				if (is2026) ret += "</div>\r\n"; // <div class=\"w-full mb24 px72 py8 s-px16 s-py8 m-px16 m-py8\">
			} else {
				// noHit
				ret = "検索条件にヒットする製品が見つかりませんでした。";
				if (lang.indexOf("en-") > -1) {
					ret = "Products meeting the search conditions could not be found.";
				} else if (lang.equals("zh-tw")) {
					ret = "找不到要命中搜尋條件的產品。";
				} else if (lang.indexOf("zh-") > -1) {
					ret = "找不到要命中搜索条件的产品。";
				}
				if (is2026) {
					ret = "<div class=\"f fclm gap-24 mb48\">\r\n"
							+ "    <div class=\"f fh border boder-base-stroke-subtle h160 w-full bg-base-container-accent\">"
							+ "        <span class=\"fw5 s-px16 s-text-center m-px16 m-text-center\">"+ ret +"</span>"
							+ "    </div>\r\n"
							+ "</div>";
				} else {
					ret = "<br><p>" + ret + "</p>";
				}
				isNoHit = true;
			}
		} catch (Exception e) {
			log.error("getSearch3S() page.parse exception. e="+e.getMessage() );
			ret = "ERROR! 検索条件にヒットする製品が見つかりませんでした。";
			if (lang.indexOf("en-") > -1) {
				ret = "ERROR! Products meeting the search conditions could not be found.";
			} else if (lang.equals("zh-tw")) {
				ret = "ERROR! 找不到要命中搜尋條件的產品。";
			} else if (lang.indexOf("zh-") > -1) {
				ret = "ERROR! 找不到要命中搜索条件的产品。";
			}
			if (is2026) {
				ret = "<div class=\"f fclm gap-24 mb48\">\r\n"
						+ "    <div class=\"f fh border boder-base-stroke-subtle h160 w-full bg-base-container-accent\">"
						+ "        <span class=\"fw5 s-px16 s-text-center m-px16 m-text-center\">"+ ret +"</span>"
						+ "    </div>\r\n"
						+ "</div>";
			} else {
				ret = "<br><p>" + ret + "</p>";
			}
			isNoHit = true;
		}
		ret = header + ret ;
		String backUrl = request.getHeader("REFERER");
		String mes = messagesource.getMessage("act.back", null,  baseLocale);
		String tmp = "";
		if (backUrl != null && backUrl.isEmpty() == false ) {
			if (is2026) {
				tmp =	 "<div class=\"f fc mt16 mb48\">\r\n"
						+ "      <a class=\"button large primary solid w264 gap-8 s-w-full m-w-full\" href=\"$$$url$$$\">\r\n"
						+ "        <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left-white.svg\" alt=\"\" title=\"\">\r\n"
						+ "        <span>$$$message$$$</span>\r\n"
						+ "      </a>\r\n"
						+ "</div>".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes);
			} else {
				tmp = "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes);
			}
		} 
		ret += tmp;
		if (is2026) {
			ret += "</div>\r\n</main>\r\n";
		} else {
			ret += "</div>"; // id=content
		}
		
		if (baseLang.equals("zh-cn")) {
			if (isNoHit == false) {
				footer = footer.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", kw).replace("$YYY$", "1")+"\r\n<footer>");
			} else {
				footer = footer.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", kw).replace("$YYY$", "0")+"\r\n<footer>");
			}
		}
		ret +=footer;
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			ret = html.changeLang(ret, baseLang, lang, toT.getHeader(), toT.getFooter(), false);
		}
		
		return ret;
	}
	// 旧デザイン用  -2026
	String _jsonHeader =
			"<link href=\"/css/common.css\" rel=\"stylesheet\" type=\"text/css\">\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/products/js/products.js?200609\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/products/js/s3s.js\"></script>\r\n" +
			"<link href=\"/products/css/style.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/assets/webcatalog/css/s3s.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"\r\n" +
			"<script src=\"https://code.jquery.com/jquery-3.3.1.min.js\"></script>\r\n" +
			"<script src=\"/products/js/json-viewer/jquery.json-viewer.js\"></script>\r\n" +
			"<link href=\"/products/js/json-viewer/jquery.json-viewer.css\" type=\"text/css\" rel=\"stylesheet\">\r\n" +
			"";
	String _3sHeader =
			"<link href=\"/css/common.css\" rel=\"stylesheet\" type=\"text/css\">\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/assets/js/micromodal.min.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/products/js/s3s.js\"></script>\r\n" +
			"<link href=\"/products/css/style.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/assets/webcatalog/css/s3s.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n"
			+ "<link href=\"/smc_css/micromodal.css\" rel=\"stylesheet\" type=\"text/css\">\r\n" +
			"\r\n" +
			"\r\n" +
			"<style>\r\n" +
			".modal {\r\n" +
			"  display: none;\r\n" +
			"}\r\n" +
			".modal.is-open {\r\n" +
			"  display: block;\r\n" +
			"}\r\n" +
			"</style>\r\n" +
			"\r\n" +
			"<script>\r\n" +
			"$(function(){\r\n" +
			"        MicroModal.init();\r\n" +
			"});\r\n" +
			"\r\n" +
			"</script>" ;

	@GetMapping(value={"/{lang}/detail/", "/{lang}/detail", "/{lang}/detail/{typeid}", "/{lang}/detail/{typeid}/",
			"/{lang}/detail/{typeid}/{sid}", "/{lang}/detail/{typeid}/{sid}/",
			 "/{lang}/detail/{typeid}/{sid}/{sid2}", "/{lang}/detail/{typeid}/{sid}/{sid2}/",
			 "/{lang}/detail/{typeid}/{sid}/{sid2}/{sid3}", "/{lang}/detail/{typeid}/{sid}/{sid2}/{sid3}/"}, produces="text/html;charset=UTF-8")
	@CrossOrigin(origins= {"http://192.168.0.36","http://192.168.0.34","http://153.120.135.17","https://153.120.135.17",
			"http://localhost:8080","http://dev1.smcworld.com","http://ap1.smcworld.com","http://ap2.smcworld.com","https://ap1admin.smcworld.com",
			"http://www.smc3s.com", "https://www.smc3s.com", "http://133.242.52.163", "http://153.120.139.192",
			"http://3sapi.smcworld.com","https://3sapi.smcworld.com", "https://test.smcworld.com","https://cdn.smcworld.com", "https://www.smcworld.com", "https://www.smc.com.cn"},allowCredentials="true")
	public String getDetail3S(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "typeid", required = false) String typeid,
			@PathVariable(name = "sid", required = false) String sid,
			@PathVariable(name = "sid2", required = false) String sid2,
			@PathVariable(name = "sid3", required = false) String sid3,
			@RequestParam(name = "partNumber", required = false) String partNumber,
			@RequestParam(name = "result", required = false) String result,
			@RequestParam(name="userID", required = false) String userID,
			HttpServletRequest request) {
		String ret = null;
		ErrorObject err = new ErrorObject();

		//コンテキストの取得
		ServletContext context = request.getServletContext();

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
		Locale baseLocale = getLocale(baseLang);

		if (userID == null || userID.equals("")) {
			userID = "JP_test";
		}

		if (result == null || StringUtils.isEmpty(result)) result = "detail";

		//lang for cadenas
		String ca_lang = "japanese";
		if (lang.indexOf("en-") > -1) {
			ca_lang = "english";
		} else if (lang.equals("zh-cn") || lang.equals("zh-hk")) {
			ca_lang = "chinese";
		} else if (lang.equals("zh-tw")) {
			//ca_lang = "chinese_traditional"; // 2024/6/11 繁体語は未対応。日本語のまま表示
		}
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);
		
		ModelState m = ModelState.PROD;
		if (isTestSite) m = ModelState.TEST;
		Boolean isActive = true;
		if (isTestSite) isActive = null; 
		
		Template t = templateService.getTemplateFromBean(baseLang, m);
		String header = t.getHeader();
		String footer = t.getFooter();
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			header = toT.getHeader();
			footer = toT.getFooter();
		}
		boolean is2026 = t.is2026();

		String catpan = "<div class=\"catpan\" style=\"width:1120px;margin:auto;\">\r\n" +
				"製品情報&nbsp;»&nbsp;\r\n" +
				"<a href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n" +
				"</div>";
		if (is2026) {
			header += "<main class=\"relative bg-motif s-bg-image-none m-bg-image-none\">\r\n";
			catpan = " <div class=\"container-1600\">\r\n"
					+ "\r\n"
					+ "        <div class=\"px72 py8 s-px16 s-py8 m-px16 m-py8\">\r\n"
					+ "          <nav class=\"breadcrumb leading-normal text-sm\">\r\n"
					+ "            <ol class=\"f fm gap-8 s-text-xs m-text-xs\">\r\n"
					+ "              <li>製品情報\r\n"
					+ "              </li>\r\n"
					+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
					+ "              <li><a class=\"breadcrumb-item\" href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n"
					+ "              </li>\r\n"
					+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
					+ "              <li><a class=\"breadcrumb-item\" href=\"/\">品番確認</a>\r\n"
					+ "              </li>\r\n"
					+ "            </ol>\r\n"
					+ "          </nav>\r\n"
					+ "        </div>\r\n"
					+ "";
		}
		String lang3S = "ja-JP";
		String button = "詳細表示";
		String sButton = "詳細";
		String numberCheck = "品番確認";
		String returnLink = "型式表示へ戻る";
		String returnToItem = "製品情報へ戻る";
		if (baseLang.indexOf("ja-") > -1) {
		} else if (baseLang.indexOf("en-") > -1) {
			catpan = catpan.replace("製品情報", "Product Information");
			returnLink = "Return to option configuration";
			returnToItem = "Return to Product Information";
			numberCheck = "Product Number Check";
			catpan = catpan.replace("WEBカタログ", "WEB Catalog");
			catpan = catpan.replace("品番確認", numberCheck);
			lang3S = "en-US";
			button = "Details";
			sButton = "Details";
		} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
			catpan = catpan.replace("製品情報", "产品信息");
			returnLink = "返回至选项配置";
			returnToItem = "返回至产品信息";
			numberCheck = "型号确认";
			catpan = catpan.replace("WEBカタログ", "产品目录");
			catpan = catpan.replace("品番確認", numberCheck);
			lang3S = "zh-CHS";
			button = "显示详情";
			sButton = "详情";
		} else if (baseLang.equals("zh-tw")) {
			catpan = catpan.replace("製品情報", "產品信息");
			returnLink = "返回至選項配置";
			returnToItem = "返回產品情報";
			numberCheck = "型號確認";
			catpan = catpan.replace("WEBカタログ",  "產品目錄");
			catpan = catpan.replace("品番確認", numberCheck);
			lang3S = "zh-CHS";
			button = "顯示詳情";
			sButton = "詳情";
		}

		if (typeid != null && typeid.isEmpty() == false) {
			if (sid != null && sid.isEmpty() == false) {
				if (sid2 != null && sid2.isEmpty() == false) sid = sid + "/" + sid2;
				if (sid3 != null && sid3.isEmpty() == false) sid = sid + "/" + sid3;
				if (is2026) {
					String tmp = "<div class=\"catpan px72 py8 s-px16 s-py8 m-px16 m-py8 text-sm\">\r\n"
							+ "<a class=\"breadcrumb-item\" href=\"/webcatalog/s3s/{lang}/frame/{typeid}/{sid}/\">\r\n"
							+ "    <img class=\"s16 pt6 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left.svg\" alt=\"\" title=\"\">\r\n"
							+ "    <span>"+returnLink+"</span>\r\n"
							+ "</a></div>";
					catpan += tmp;
				} else {
					catpan+= "<div class=\"catpan catpan_back\" style=\"width:1120px;margin:auto;\"><a href=\"/webcatalog/s3s/{lang}/frame/{typeid}/{sid}/\" >"+returnLink+"</a></div>\r\n" +
						"        <div class=\"clear\"></div>";
				}
				catpan = catpan.replace("{sid}", sid);
				catpan = catpan.replace("{typeid}", typeid);
				catpan = catpan.replace("{lang}", lang);
			} else {
				if (is2026) {
					String tmp = "<div class=\"catpan catpan_back\"><a href=\"/webcatalog/s3s/{lang}/frame/{typeid}\" >"+returnLink+"</a></div>\r\n" 
							/*+"        <div class=\"clear\"></div>\r\n"*/;
					tmp = StringUtils.replace(tmp, "\"catpan", "\"catpan px72 py8 s-px16 s-py8 m-px16 m-py8");
					tmp = StringUtils.replace(tmp, "<a href=\"", "<a class=\"breadcrumb-item\" href=\"");
					catpan += tmp;
				} else {
					catpan+= "<div class=\"catpan catpan_back\" style=\"width:1120px;margin:auto;\"><a href=\"/webcatalog/s3s/{lang}/frame/{typeid}\" >"+returnLink+"</a></div>\r\n" +
						"        <div class=\"clear\"></div>\r\n";
				}
				catpan = catpan.replace("{typeid}", typeid);
				catpan = catpan.replace("{lang}", lang);
			}
		}
		// 戻る
		if (sid != null && sid.isEmpty() == false) {
			if (is2026) {
				String tmp = "<div class=\"catpan px72 py8 s-px16 s-py8 m-px16 m-py8 text-sm\">\r\n"
						+ "<a class=\"breadcrumb-item\" href=\"/webcatalog/{lang}/series/{sid}\">\r\n"
						+ "    <img class=\"s16 pt6 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left.svg\" alt=\"\" title=\"\">\r\n"
						+ "    <span>"+returnToItem+"</span>\r\n"
						+ "</a></div>";
				catpan += tmp;
				catpan = catpan.replace("{sid}", sid);
				catpan = catpan.replace("{lang}", lang);
			} else {
				catpan += "<div class=\"catpan catpan_back\" style=\"width:1120px;margin:auto;\"><a href=\"/webcatalog/"+lang+"/series/"+sid+"\" >"+returnToItem+"</a></div>\r\n<div class=\"clear\"></div>";
			}
		}
		header+=catpan;
		
		Client client = S3SClient.getInstance();
		try {

			AccessTokenResult access_token = JpServiceUtil.getAccessToken(context, client, Calendar.getInstance());
//	InetAddress addr = InetAddress.getByName("www.smc3s.com");
//  	log.error(addr.getHostAddress());

			//get TypeId(s) by Japanese SeriesID
			MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
			headers.putSingle("Authorization", "Bearer " + access_token.getAccess_token());

			if (partNumber == null || partNumber.isEmpty()) partNumber = typeid;
			WebTarget target = client.target(JpServiceUtil.S3SAPI_SERVER_URL)
					.path("/3SApi/check/v1")
					.property(ClientProperties.CONNECT_TIMEOUT, 2000)// 接続タイムアウト
					.property(ClientProperties.READ_TIMEOUT, 5000)// 読み込みタイムアウト
					.queryParam("modelNo", URLEncoder.encode(partNumber, "UTF-8"))
					.queryParam("fields", "-1")
					.queryParam("language", lang3S);

			String json_result = target.request().headers(headers).get(String.class);
//	addr = InetAddress.getByName("www.smc3s.com");
//  	log.error(addr.getHostAddress());

			if (result.equals("json")) {
				ret = header.replace("</head>", _jsonHeader+"</head>");
				ret+="<div id=\"main\">\r\n" +
						"    <div id=\"json-renderer\"></div>\r\n" +
						"    <script>\r\n" +
						"var data = " + json_result +
						"\r\n" +
						"$('#json-renderer').jsonViewer(data, {collapsed: false, withQuotes: true, withLinks: false});\r\n" +
						"</script>\r\n" +
						"\r\n" +
						"\r\n" +
						"<%--EDIT end(content)--%>\r\n" +
						"<div class=\"clear\"></div>\r\n" +
						"</div><!--main-->";
				if (is2026) ret = StringUtils.replace(ret, "<div id=\"main\">", "<div class=\"w-full mb24 px72 py8 s-px16 s-py8 m-px16 m-py8\">");
				ret+=footer;
			} else if (is2026) {
				S3SResult o_result = JSON.decode(json_result, S3SResult.class);

				if (o_result.getCode().equals("21") || o_result.getCode().equals("22")) {
					result = "detail";
					S3SDetailResult o = JSON.decode(json_result, S3SDetailResult.class);

					// for CADENASLINK
					HashMap<String, String> cadenas = new LinkedHashMap<String, String>();
					forCadenasLink(cadenas, o, ca_lang);

					// for guide link
					List<Series> seriesList = new ArrayList<Series>();
					for (String _sid : o.getTypeIdJapan()) {
						Series se = seriesService.getFromModelNumber(_sid, ModelState.PROD, err);
						if (se != null) {
							seriesList.add(se);
						}
					}
//					request.setAttribute("seriesList", seriesList);
					ret =header.replace("</head>", _3sHeader+"</head>");
					String webcatalog = "WEBカタログ";
					if (baseLang.indexOf("en-") > -1) {
						webcatalog = "WEB Catalog";
					} else if (baseLang.indexOf("zh-tw") > -1) {
						webcatalog = "產品目錄";
					} else if (baseLang.indexOf("zh-") > -1) {
						webcatalog = "产品目录";
					}
					String modelNo = "品番";
					String cadPreview = "CADプレビュー";
					String pdfDatasheet = "簡易図面　　";
					String pdfDatasheetLogin = "簡易図面をご利用の際はログインが必要となります。";
					if (baseLang.indexOf("en-") > -1) {
						modelNo = "Product Part Number";
						cadPreview = "CAD Preview";
						pdfDatasheet = "PDF datasheet";
						pdfDatasheetLogin = "You will need to login to use the PDF datasheet.";
					} else if (baseLang.indexOf("zh-tw") > -1) {
						modelNo = "型號";
						cadPreview = "CAD预览";
						pdfDatasheet = "PDF 文件";
						pdfDatasheetLogin = "您需要登入才能使用 PDF 文件。";
					} else if (baseLang.indexOf("zh-") > -1) {
						modelNo = "型号";
						cadPreview = "CAD預覽";
						pdfDatasheet = "PDF 文件";
						pdfDatasheetLogin = "您需要登录才能使用 PDF 文件。";
					}
					// 枠
					ret += "<div class=\"w-full mb24 px72 py8 s-px16 s-py8 m-px16 m-py8\">"
							+ "<div class=\"f w-full s-fclm m-fclm gap-36 mb48\">";
					
					// 画像
					String img = o.getPicture();
					if (img != null) {
						img = StringUtils.replace(img, "{size}", "L");
						ret += "<div class=\"f fclm fc\">\r\n"
							+ "    <div class=\"relative f fc\">\r\n"
							+ "        <img src=\""+img+"\" class=\"s3s_picture_M flex-fixed object-fit-contain w264\">"
							+ "    </div>\r\n"
							+ "</div>\r\n";
					}
					
					// タイトル・ボタン
					ret += "<div class=\"f fclm w-full\">"; // 右側スタート
					
					String title1 = modelNo.substring(0,1);
					String title2 = modelNo.substring(1);
					ret += "<h2 class=\"text-6xl mb32 leading-tight fw5 s-fw6 s-text-3xl m-fw6 m-text-3xl\">"
							+ "<span class=\"text-primary text-6xl\">"+title1+"</span><span class=\"text-base-foreground-default text-6xl\">"+title2+": "+o.getModelNo()+"</span>"
							+ "</h2>";

					ret += "<div class=\"g grid-autofit-180 gap-8 w-full s-gcol1 m-gcol1\">"; // ボタンスタート

					if (o.getCadParameter() != null && o.getCadParameter().isEmpty() == false) {
						String param = o.getCadParameter();
						if (param.indexOf("\\\\") > 0) param = param.replace("\\\\", "/");
//						String btn = "<button class=\"bt_3s_preview button secondary solid large gap-8 w-full medium\" onclick=\"location.href='https://webapi.partcommunity.com/cgi-bin/plogger.asp?part="+param+"&firm=SMC&language="+ca_lang+"&external=1'\" target=\"_blank\">"
//									+ "  <span class=\"flex-fixed text-sm leading-tight\">"+cadPreview+"</span>"
//									+ "</button>";
						String btn = "<button class=\"bt_3s_preview button secondary solid large gap-8 w-full medium\" onclick=\"document.getElementById('3s_link').click();\">"
								+ "<a href=\"https://webapi.partcommunity.com/cgi-bin/plogger.asp?part="+param
								+ "&firm=SMC&language="+ca_lang+"&external=1\" class=\"bt_3s_preview\" target=\"_blank\"><span>"+cadPreview+"</span></a>"
								 		+ "</button>";
						ret+= btn;
					}
					if (baseLang.indexOf("zh-") > -1) { // 中国
						String ca_info = cadenas.get("ca_info");
						String ca_varset = cadenas.get("ca_varset");
						if (ca_info != null && ca_info.isEmpty() == false) {
							String link = "https://webassistants.partcommunity.com/23d-libs/smc_jp_assistant/gui/pcom_emb.html";
							link += "?info="+ca_info+"&varset="+ca_varset+"&language="+cadenas.get("ca_language")+"&name="+cadenas.get("ca_name");
							String btn = "<button class=\"bt_3s_23dcad button secondary solid large gap-8 w-full medium\" onclick=\"location.href='"+link+"'\" target=\"_blank\">"
									+ "  <span class=\"flex-fixed text-sm leading-tight\">2D/3D CAD</span>"
									+ "</button>";
							ret+= btn;
						}
					}
					// for guide link
					// TODO modal
					if (seriesList != null && seriesList.size() > 0) {
// original						ret+="<a class=\"bt_3s digi_cat\" data-micromodal-trigger=\"modal-a\"><span>"+webcatalog+"</span></a>";
						String btn = "<button class=\"bt_3s_webcatalog button secondary solid large gap-8 w-full medium\"  data-micromodal-trigger=\"modal-a\">"
								+ "  <span class=\"flex-fixed text-sm leading-tight\">"+webcatalog+"</span>"
								+ "</button>";
						ret+= btn;
					}
					// ログイン必須で一旦区切り
					ret += "</div>\r\n"; //ボタンend
					
					// ログイン前 2026/3/1
					if (baseLang.equals("ja-jp")) {
						ret+="<div class=\"isLoginFalse\" style=\"display:block; margin-top:10px;\">\r\n"
							+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
							+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">簡易図面、2D/3D CADは\r\n"
							+ "ユーザ登録者限定サービスです。\r\n"
							+ "ログインしてご利用ください"
							+ "</div>\r\n"
							+ "<a href=\"javascript:void(0)\" \r\n"
							+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">ログイン</span></a>\r\n"
							+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
							+ "<button class=\"button solid gap-8 w-full primary  medium\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">簡易図面</span></button>\r\n"
							+ "<button class=\"button solid gap-8 w-full secondary  medium\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">2D/3D CAD</span></button>\r\n"
							+ "</div>\r\n"
							+ "</div>\r\n"
							+ "</div>";
					} else if (baseLang.indexOf("en-") > -1) {
						ret+="<div class=\"isLoginFalse\" style=\"display:block; margin-top:10px;\">\r\n"
								+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
								+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">PDF datasheet and 2D/3D CAD\r\n"
								+ "are services available only to registered users.\r\n"
								+ "Please log in to use these services.</div>\r\n"
								+ "<a href=\"javascript:void(0)\" \r\n"
								+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Login</span></a>\r\n"
								+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
								+ "<button class=\"button solid gap-8 w-full primary  medium\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">PDF datasheet</span></button>\r\n"
								+ "<button class=\"button solid gap-8 w-full secondary  medium\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">2D/3D CAD</span></button>\r\n"
								+ "</div>\r\n"
								+ "</div>\r\n"
								+ "</div>";
					}
					
					// 簡易図面、PDFデータシート
					URL u = new URL(request.getRequestURL().toString());
					String host = u.getHost();
					if (baseLang.equals("ja-jp") || baseLang.indexOf("en-") > -1) {
						if (o.getCadParameter() != null && o.getCadParameter().isEmpty() == false) {
							String param = o.getCadParameter();
							if (param.indexOf("\\\\") > 0) param = param.replace("\\\\", "/");
							else if (param.indexOf("\\") > 0) param = param.replace("\\", "/");
//							ret+="<script src='https://service.web2cad.co.jp/maker/techdemo/webcomponents/8.1.0/api/js/thirdparty.min.js'></script>";
//							ret+="<script src='https://service.web2cad.co.jp/maker/techdemo/webcomponents/8.1.0/api/js/psol.components.min.js'></script>";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/thirdparty.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/psol.components.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/axios.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/crypto-js.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/js.cookie.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/pdf_datasheet.js'></script>\r\n";
							ret+="<div id=\"pdf_datasheet_login\" class=\"isLoginTrue\" style=\"display:none;\">\r\n";
							ret += "<div class=\"g grid-autofit-180 gap-8 w-full s-gcol1 m-gcol1\">"; // ボタン枠スタート
							ret += "<button class=\"button solid gap-8 w-full primary  medium bt_3s_datasheet\" type=\"button\" onclick=\"downloadCAD('"+param+"', 'PDFDATASHEET', '"+cadenas.get("ca_language")+"');\">"
									+ "<img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check-white.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">"+pdfDatasheet+"</span>"
									+ "</button>";
							// 3D CAD  on 簡易図面、PDFデータシートの下へ margin-top:10px margin-right: 15px;
							String ca_info2 = cadenas.get("ca_info");
							String ca_varset2 = cadenas.get("ca_varset");
							if (ca_info2 != null && ca_info2.isEmpty() == false) {
								String link = "/products/cad/"+lang+"/?info="+ca_info2+"&varset="+ca_varset2+"&language="+cadenas.get("ca_language")+"&name="+cadenas.get("ca_name");
								ret += "<button class=\"button solid gap-8 w-full secondary  medium bt_3s_cad23d\" type=\"button\" onclick=\"location.href='"+link+"'\" target=\"_blank\">"
										+ "  <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\"2D/3D CAD\" title=\"2D/3D CAD\">"
										+ "  <span class=\"text-sm s-leading-tight m-leading-tight\">2D/3D CAD</span>"
										+ "</button>";
							}
							ret+="</div>"; //ボタン枠end
							ret += "</div>\r\n"; // isLoginTrue end
						}
					}
					
					ret += "</div>\r\n"; // 右側end
					
					// 代替品番
					if (o.getConsolidation() != null) {
						ret+= "<div class=\"clear\"></div>\r\n" +
								"                    <div class=\"s3s_consolidation\">\r\n" +
								"                        <p style=\"\">\r\n" +
								o.getConsolidation().description+"\r\n" +
								"                        </p>\r\n" ;
						// 代替品番がある場合 品番詳細表示
						if (o.getConsolidation().newModel != null && o.getConsolidation().newModel.isEmpty() == false) {
							String newModel = "新品番を表示";
							if (baseLang.indexOf("en-") > -1) {
								newModel = "New Product Number";
								ret+= "                        →<a href=\"/webcatalog/s3s/"+lang+"/detail/"+sid+"/?partNumber="+o.getConsolidation().newModel+"\" target=\"_parent\">"+newModel+"</a>\r\n\r\n";
							} else if (baseLang.indexOf("zh-") > -1) {
								newModel = "新型号";
								ret+= "                        →<a href=\"/webcatalog/s3s/"+lang+"/detail/"+sid+"?partNumber="+o.getConsolidation().newModel+"\" target=\"_parent\">"+newModel+"</a>\r\n\r\n";
							} else {
								ret+= "                        →<a href=\"/webcatalog/s3s/ja-jp/detail/"+sid+"/?partNumber="+o.getConsolidation().newModel+"\" target=\"_parent\">"+newModel+"</a>\r\n\r\n";
							}
						} else {
							// 代替品番がない場合 新シリーズIDで選定画面を呼ぶ
							String newModel = "新シリーズで選定する";
							if (baseLang.indexOf("en-") > -1) {
								newModel = "New Series";
								if (sid != null && sid.isEmpty() == false) {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"/"+sid+"/\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								} else {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								}
							} else if (baseLang.indexOf("zh-") > -1) {
								newModel = "新系列";
								if (sid != null && sid.isEmpty() == false) {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"/"+sid+"/\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								} else {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								}
							} else {
								if (sid != null && sid.isEmpty() == false) {
									ret+= "                     →<a href=\"/webcatalog/s3s/ja-jp/frame/"+o.getConsolidation().newTypeID+"/"+sid+"/\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								} else {
									ret+= "                     →<a href=\"/webcatalog/s3s/ja-jp/frame/"+o.getConsolidation().newTypeID+"\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								}
							}
						}
						ret+="\r\n" +
							"                    </div>";
					}
					ret += "</div><!-- f w-full -->\r\n";
					ret += "</div><!-- w-full -->\r\n";

					// 仕様
					String Specifications = "仕様";
					if (baseLang.indexOf("en-") > -1) {
						Specifications = "Specifications";
					} else if (baseLang.indexOf("zh-") > -1) {
						Specifications = "规格";
					}
					ret +="<div class=\"w-full mb24 px72 py8 s-px16 s-py8 m-px16 m-py8\">"
						+ "    <div class=\"s3s_detail_pain\">\r\n" +
							"        <div class=\"s3s_detail_h\">"+Specifications+": "+o.getProductName()+"</div>\r\n" ;
					String str1 = "フィールドの情報";
					String str2 = "フィールドの値";
					String str3 = "値の情報";
					if (baseLang.indexOf("en-") > -1) {
						str1 = "Field";
						str2 = "Value";
						str3 = "Value Details";
					} else if (baseLang.indexOf("zh-") > -1) {
						str1 = "可选项信息";
						str2 = "可选项的值";
						str3 = "值的信息";
					}
					if (o.getSerial() != null) {
						ret+="        <div class=\"s3s_detail_inner\"> "+o.getSerial().getDescription()+"\r\n" +
							"            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"s3s_prop_tbl\">\r\n" +
							"                <tbody>\r\n" +
							"                    <tr>\r\n" +
							"                        <th scope=\"col\">"+str1+"</th>\r\n" +
							"                        <th scope=\"col\">"+str2+"</th>\r\n" +
							"                        <th scope=\"col\" class=\"last\">"+str3+"</th>\r\n" +
							"                    </tr>\r\n" ;
						if (o.getProperties() != null) {
							for(Prop prop : o.getProperties()) {
								ret+="                        <tr>\r\n" +
								"                            <td>"+prop.getName()+"</td>\r\n" +
								"                            <td>"+prop.getValue()+"</td>\r\n" +
								"                            <td>"+prop.getDescription()+"</td>\r\n" +
								"                        </tr>\r\n";
							}
						}
						ret+="                </tbody>\r\n" +
							"            </table>\r\n" +
							"        </div>\r\n" +
							"        <!--inner-->\r\n" +
							"        </logic:notEmpty>\r\n" +
							"    </div>\r\n" +
							"    <!--pain-->\r\n" +
							"<br />";
					}
					// 関連製品 Accessories
					String Related = "関連製品";
					if (baseLang.indexOf("en-") > -1) {
						Related = "Related Products";
					} else if (baseLang.indexOf("zh-") > -1) {
						Related = "相关产品";
					}
					String str21 = "フィールドの情報";
					String str22 = "フィールドの値";
					String str23 = "値の情報";
					if (baseLang.indexOf("en-") > -1) {
						str21 = "Image";
						str22 = "Part Number";
						str23 = "Part Name";
					} else if (baseLang.indexOf("zh-") > -1) {
						str21 = "图片";
						str22 = "型号";
						str23 = "名称";
					}
					if (o.getRelatedProducts() != null && o.getRelatedProducts().getAccessories() != null && o.getRelatedProducts().getAccessories().size() > 0) {
						ret+="<br />"+
							"        <div class=\"s3s_detail_pain\">\r\n" +
							"            <div class=\"s3s_detail_h\">"+Related+"</div>\r\n" +
							"            <div class=\"s3s_detail_inner\">\r\n" +
							"\r\n" +
							"                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"s3s_prop_tbl\">\r\n" +
							"                    <tbody>\r\n" +
							"                        <tr>\r\n" +
							"                            <th scope=\"col\">"+str21+"</th>\r\n" +
							"                            <th scope=\"col\">"+str22+"</th>\r\n" +
							"                            <th scope=\"col\" class=\"last\">"+str23+"</th>\r\n" +
							"                        </tr>\r\n" ;
						for(Accessories acc : o.getRelatedProducts().getAccessories()) {
							ret+="                            <tr>\r\n" +
							"                                <td class=\"tdc\" width=\"15%\">\r\n" ;
							if (acc.getPicture() != null && acc.getPicture().isEmpty() == false) {
								ret+="                               <img src=\""+acc.getPictureS()+"\" class=\"s3s_picture_S\"/>\r\n";
							} else {
								ret+="                               －\r\n";
							}
							ret+="                                </td>\r\n" +
							"                                <td>"+acc.getModel()+"</td>\r\n" +
							"                                <td>"+acc.getDescription()+"</td>\r\n" +
							"                            </tr>\r\n" ;
						}
						ret+="                    </tbody>\r\n" +
							"                </table>\r\n" +
							"\r\n" +
							"            </div><!--inner-->\r\n" +
							"        </div><!--pain-->\r\n";
					}
					// 関連製品 suitableSeries
					if (o.getRelatedProducts() != null && o.getRelatedProducts().getSuitableSeries() != null && o.getRelatedProducts().getSuitableSeries().size() > 0) {
						ret+="<br />"+
							"<div class=\"s3s_detail_pain\">\r\n" +
							"            <div class=\"s3s_detail_h\">"+Related+"</div>\r\n" +
							"            <div class=\"s3s_detail_inner\">\r\n" +
							"\r\n" +
							"                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"s3s_prop_tbl\">\r\n" +
							"                        <tbody>\r\n" +
							"                            <tr>\r\n" +
							"                                <th scope=\"col\">"+str21+"</th>\r\n" +
							"                                <th scope=\"col\">"+str22+"</th>\r\n" +
							"                                <th scope=\"col\" class=\"last\">"+Related+"</th>\r\n" +
							"                            </tr>\r\n" ;
						for(SuitableSeries suitable : o.getRelatedProducts().getSuitableSeries()) {
							ret+="                                <tr>\r\n" +
							"                                    <td class=\"tdc\" width=\"15%\">\r\n" ;
							if (suitable.getPicture() != null && suitable.getPicture().isEmpty() == false) {
								ret+="                                        <img src=\""+suitable.getPictureS()+"\" class=\"s3s_picture_S\"/>\r\n";
							} else {
								ret+="                                        －\r\n" ;
							}
							ret+="                                    </td>\r\n" +
							"                                    <td>"+suitable.getName()+"</td>\r\n" +
							"                                    <td class=\"tdc\">\r\n" ;
							if (sid != null && sid.isEmpty() == false) {
								ret+="                                    →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+suitable.getTypeID()+"/"+sid+"/\" target=\"_parent\">"+sButton+"</a>\r\n" ;
							} else {
								ret+="                                    →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+suitable.getTypeID()+"\" target=\"_parent\">"+sButton+"</a>\r\n" ;
							}
							ret+="                                    </td>\r\n" +
							"                                </tr>\r\n" ;
						}
						ret+="                        </tbody>\r\n" +
							"                </table>\r\n" +
							"\r\n" +
							"            </div><!-- inner -->\r\n" +
							"        </div><!-- pain -->";
					}
					ret += "</div><!-- w-full -->";
					// シリーズ
					String str31 = "名称";
					String str32 = "カタログ";
					if (baseLang.indexOf("en-") > -1) {
						str31 = "Name";
						str32 = "Catalog";
					} else if (baseLang.indexOf("zh-") > -1) {
						str31 = "名称";
						str32 = "产品目录";
					}
					if (seriesList != null && seriesList.size() > 0) {
						if (is2026) {
							ret+="<div id=\"modal-a\" aria-hidden=\"true\" class=\"micromodal-slide modal\">\r\n" +
									"                        <div class=\"modal__overlay\" tabindex=\"-1\" data-micromodal-close=\"\">\r\n" +
									"                        <div class=\"modal__container h200 w70per\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"modal-1-title\">\r\n" +
									"                            \r\n" +
									"                            <div id=\"modal-1-content\" class=\"modal__content\">\r\n" +
									"                                <table class=\"table-hover s-full border-bottom border-right border-base-stroke-default border-collapse-collapse w100per\" cellpadding=\"0\" cellspacing=\"0\" >\r\n" +
									"    <thead>\r\n" +
									"        <tr>\r\n" +
									"            <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-black text-sm leading-tight fw5\">"+str31+"</th>\r\n" +
									"            <th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-black text-sm leading-tight fw5\">"+str32+"</th>\r\n" +
									"        </tr>\r\n" +
									"    </thead>\r\n" +
									"    <tbody>\r\n" ;
							for(Series s : seriesList) {
								ret+="        <tr>\r\n" +
									"            <td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-black text-xs leading-normal fw5\">"+s.getModelNumber()+":"+s.getName()+"</td>\r\n" +
									"            <td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">"
									+ "            <div class=\"f fc\">\r\n"
									+ "            <a class=\"f fm gap-4\" href=\"/webcatalog/"+lang+"/series/"+s.getModelNumber()+"\">"
									+ "              <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">"+"検索結果を表示"+"</span>\r\n"
									+ "              <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\"\"> "
									+ "            </a>\r\n"
									+ "          </td>\r\n" +
									"        </tr>\r\n" ;
							}
							ret+="    </tbody>\r\n" +
									"                                    </table>\r\n" +
									"                            </div>\r\n" +
									"                        </div>\r\n" +
									"                        </div>\r\n" +
									"                    </div>";
						} else {
							ret+="<div id=\"modal-a\" aria-hidden=\"true\" class=\"micromodal-slide modal\">\r\n" +
									"                        <div class=\"modal__overlay\" tabindex=\"-1\" data-micromodal-close=\"\">\r\n" +
									"                        <div class=\"modal__container\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"modal-1-title\" style=\"height:200px;\">\r\n" +
									"                            \r\n" +
									"                            <div id=\"modal-1-content\" class=\"modal__content\">\r\n" +
									"                                <table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:900px\">\r\n" +
									"    <thead>\r\n" +
									"        <tr>\r\n" +
									"            <th>"+str31+"</th>\r\n" +
									"            <th>"+str32+"</th>\r\n" +
									"        </tr>\r\n" +
									"    </thead>\r\n" +
									"    <tbody>\r\n" ;
							for(Series s : seriesList) {
								ret+="        <tr>\r\n" +
									"            <td>"+s.getModelNumber()+":"+s.getName()+"</td>\r\n" +
									"            <td class=\"tdc\"><a href=\"/webcatalog/"+lang+"/series/"+s.getModelNumber()+"\" style=\"color:#0074bf\">"+sButton+"</a></td>\r\n" +
									"        </tr>\r\n" ;
							}
							ret+="    </tbody>\r\n" +
									"                                    </table>\r\n" +
									"                            </div>\r\n" +
									"                        </div>\r\n" +
									"                        </div>\r\n" +
									"                    </div>";
						}
					}

				} else if (o_result.getCode().equals("23")) {
					result = "search";
					S3SSearchResult s_result = JSON.decode(json_result, S3SSearchResult.class);
//					request.setAttribute("result", s_result);
					ret =header.replace("</head>", _3sHeader+"</head>");
					if (baseLang.indexOf("ja-") > -1) {
						ret+="<div id=\"content\">\r\n" +
								"<div class=\"one_column_cont\">\r\n" +
								"\r\n" +
								"\r\n" +
								"<div class=\"form_box w_form_box\">\r\n" +
								"<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/ja-jp/search3S/\">\r\n" +
								"\r\n" +
								"    <div class=\"sform\">\r\n" +
								"      <label for=\"k\">製品検索</label>\r\n" +
								"      <div class=\"input\">\r\n" +
								"      <input type=\"text\" name=\"kwItem\" value=\"\" id=\"kwItem\" class=\"k\" placeholder=\"フル品番の一部で検索してください\">\r\n" +
								"      <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"searchProductsKeyword('ja-jp');return false;\" onkeypress=\"return inactiveEnter(event);\"></span>\r\n" +
								"      </div>\r\n" +
								"    </div>\r\n" +
								"</form>\r\n"+
								"</div>\r\n"+
								"</div>\r\n";
					} else if (baseLang.indexOf("en-") > -1) {
						ret+="<div id=\"content\">\r\n" +
								"<div class=\"one_column_cont\">\r\n" +
								"\r\n" +
								"\r\n" +
								"<div class=\"form_box w_form_box\">\r\n" +
								"<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/"+lang+"/search3S/\">\r\n" +
								"\r\n" +
								"    <div class=\"sform\">\r\n" +
								"      <label for=\"k\">Product</label>\r\n" +
								"      <div class=\"input\">\r\n" +
								"      <input type=\"text\" name=\"kwItem\" value=\"\" id=\"kwItem\" class=\"k\" placeholder=\"Product Number Check\">\r\n" +
								"      <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"searchProductsKeyword('en-jp');return false;\" onkeypress=\"return inactiveEnter(event);\"></span>\r\n" +
								"      </div>\r\n" +
								"    </div>\r\n" +
								"</form>"+
								"</div>\r\n"+
								"</div>\r\n";
					} else if (baseLang.indexOf("zh-") > -1) {
						ret+="<div id=\"content\">\r\n" +
								"<div class=\"one_column_cont\">\r\n" +
								"\r\n" +
								"\r\n" +
								"<div class=\"form_box w_form_box\">\r\n" +
								"<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/"+lang+"/search3S/\">\r\n" +
								"\r\n" +
								"    <div class=\"sform\">\r\n" +
								"      <label for=\"k\">产品目录</label>\r\n" +
								"      <div class=\"input\">\r\n" +
								"      <input type=\"text\" name=\"kwItem\" value=\"\" id=\"kwItem\" class=\"k\" placeholder=\"型号确认\">\r\n" +
								"      <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"searchProductsKeyword('zh-cn');return false;\" onkeypress=\"return inactiveEnter(event);\"></span>\r\n" +
								"      </div>\r\n" +
								"    </div>\r\n" +
								"</form>"+
								"</div>\r\n"+
								"</div>\r\n";
					}
					if (s_result.getCandidate().size() == 0) {
						if (baseLang.indexOf("ja-") > -1) {
							ret+="<div class=\"no_result\">\r\n" +
								"                検索条件にヒットする製品が見つかりませんでした。\r\n" +
								"            </div>";
						} else if (baseLang.indexOf("en-") > -1) {
							ret+="<div class=\"no_result\">\r\n" +
									"                There were no hits.\r\n" +
									"            </div>";
						} else if (baseLang.indexOf("zh-") > -1) {
							ret+="<div class=\"no_result\">\r\n" +
									"                未找到符合条件的产品。\r\n" +
									"            </div>";
						}
					} else {
						if (baseLang.indexOf("ja-") > -1) {
							ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
								"    <tr>\r\n" +
								"    <th width=\"10%\">画像</th>\r\n" +
								"    <th width=\"45%\">名称</th>\r\n" +
								"    <th width=\"15%\">シリーズ</th>\r\n" +
								"    </tr>";
						} else if (baseLang.indexOf("en-") > -1) {
							ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
									"    <tr>\r\n" +
									"    <th width=\"10%\">Image</th>\r\n" +
									"    <th width=\"45%\">Name</th>\r\n" +
									"    <th width=\"15%\">Series</th>\r\n" +
									"    </tr>";
						} else if (baseLang.indexOf("zh-") > -1) {
							ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
									"    <tr>\r\n" +
									"    <th width=\"10%\">图片</th>\r\n" +
									"    <th width=\"45%\">名称</th>\r\n" +
									"    <th width=\"15%\">系列</th>\r\n" +
									"    </tr>";
						}
						for(Candidate c : s_result.getCandidate()) {
							ret+="<tr>\r\n" +
									"    <td class=\"tdc\"><img src=\""+c.getPicture()+"\" class=\"s3s_picture_S\"/></td>\r\n" +
									"    <td>"+c.getName()+"</td>\r\n" +
									"    <td>"+c.getSeries()+"</td>\r\n" +
									"    </tr>";
						}
						ret+="</table>";
					}
				}
					
				// スピナー  
				ret += "    <!-- loding -->\r\n"
						+ "    <div id=\"bt_3s_overlay\">\r\n"
						+ "        <div class=\"cv-spinner\">\r\n"
						+ "            <span class=\"spinner\"></span>\r\n"
						+ "        </div>\r\n"
						+ "    </div>";
				if (baseLang.equals("zh-cn")) {
					footer = footer.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", partNumber).replace("$YYY$", "1")+"\r\n<footer>");
				}
				ret += "</div><!-- .container-1600 -->\r\n";
				ret += "</main>\r\n";
				ret += footer;
			} else {
				S3SResult o_result = JSON.decode(json_result, S3SResult.class);

				if (o_result.getCode().equals("21") || o_result.getCode().equals("22")) {
					result = "detail";
					S3SDetailResult o = JSON.decode(json_result, S3SDetailResult.class);

					// for CADENASLINK
					HashMap<String, String> cadenas = new LinkedHashMap<String, String>();
					forCadenasLink(cadenas, o, ca_lang);

					// for guide link
					List<Series> seriesList = new ArrayList<Series>();
					for (String _sid : o.getTypeIdJapan()) {
						Series se = seriesService.getFromModelNumber(_sid, ModelState.PROD, err);
						if (se != null) {
							seriesList.add(se);
						}
					}
//					request.setAttribute("seriesList", seriesList);
					ret =header.replace("</head>", _3sHeader+"</head>");
					ret+="<div id=\"content\">";
					String webcatalog = "WEBカタログ";
					if (baseLang.indexOf("en-") > -1) {
						webcatalog = "WEB Catalog";
					} else if (baseLang.indexOf("zh-tw") > -1) {
						webcatalog = "產品目錄";
					} else if (baseLang.indexOf("zh-") > -1) {
						webcatalog = "产品目录";
					}
					String modelNo = "品番";
					String cadPreview = "CADプレビュー";
					String pdfDatasheet = "簡易図面　　";
					String pdfDatasheetLogin = "簡易図面をご利用の際はログインが必要となります。";
					if (baseLang.indexOf("en-") > -1) {
						modelNo = "Product Part Number";
						cadPreview = "CAD Preview";
						pdfDatasheet = "PDF datasheet";
						pdfDatasheetLogin = "You will need to login to use the PDF datasheet.";
					} else if (baseLang.indexOf("zh-tw") > -1) {
						modelNo = "型號";
						cadPreview = "CAD预览";
						pdfDatasheet = "PDF 文件";
						pdfDatasheetLogin = "您需要登入才能使用 PDF 文件。";
					} else if (baseLang.indexOf("zh-") > -1) {
						modelNo = "型号";
						cadPreview = "CAD預覽";
						pdfDatasheet = "PDF 文件";
						pdfDatasheetLogin = "您需要登录才能使用 PDF 文件。";
					}
					ret+="<table width=\"100%\"  cellpadding=\"0\" cellspacing=\"0\" border=\"1\" class=\"result_layout_tbl\">\r\n" +
							"        <tbody>\r\n" +
							"            <tr>\r\n" +
							"                <td width=\"200px\"><img src=\""+o.getPicture()+"\" class=\"s3s_picture_M\"/></td>\r\n" +
							"                <td align=\"left\" valign=\"top\">\r\n" +
							"                    <p style=\"font-weight: bold;font-size: 18px;line-height: 22px\">"+modelNo+":<br/>\r\n" +
							"                        <span style=\"color:#0074bf;font-size: inherit;\"> "+o.getModelNo()+" </span> </p>";
					if (o.getCadParameter() != null && o.getCadParameter().isEmpty() == false) {
						String param = o.getCadParameter();
						if (param.indexOf("\\\\") > 0) param = param.replace("\\\\", "/");
						ret+="<a href=\"https://webapi.partcommunity.com/cgi-bin/plogger.asp?part="+param+
								 "&firm=SMC&language="+ca_lang+"&external=1\" class=\"bt_3s preview\" target=\"_blank\"><span>"+cadPreview+"</span></a>";
					}
					if (baseLang.indexOf("zh-") > -1) { // 中国
						String ca_info = cadenas.get("ca_info");
						String ca_varset = cadenas.get("ca_varset");
						if (ca_info != null && ca_info.isEmpty() == false) {
							ret +="<a href=\"https://webassistants.partcommunity.com/23d-libs/smc_jp_assistant/gui/pcom_emb.html"
								+ "?info="+ca_info+"&varset="+ca_varset+"&language="+cadenas.get("ca_language")+"&name="+cadenas.get("ca_name")+"\" class=\"bt_3s cad23d\" target=\"_blank\"><span>2D/3D CAD</span></a>";
						}
					}
					// for guide link
					if (seriesList != null && seriesList.size() > 0) {
						ret+="<a class=\"bt_3s digi_cat\" data-micromodal-trigger=\"modal-a\"><span>"+webcatalog+"</span></a>";
//						ret+="<a href=\"#TB_inline?height=70&width=600&inlineId=seriesList&modal=false\" class=\"thickbox bt_3s\">"+webcatalog+"</a>";
					}
					ret+="<div class=\"clear\"></div>\r\n";
					
					// ログイン前 2026/3/1
					if (baseLang.equals("ja-jp")) {
						ret+="<div class=\"isLoginFalse\" style=\"display:block; margin-top:10px;\">\r\n"
							+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
							+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">簡易図面、2D/3D CADは\r\n"
							+ "ユーザ登録者限定サービスです。\r\n"
							+ "ログインしてご利用ください"
							+ "</div>\r\n"
							+ "<a href=\"javascript:void(0)\" \r\n"
							+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">ログイン</span></a>\r\n"
							+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
							+ "<button class=\"button neutral solid disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">簡易図面</span></button>\r\n"
							+ "<button class=\"button neutral outline disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">2D/3D CAD</span></button>\r\n"
							+ "</div>\r\n"
							+ "</div>\r\n"
							+ "</div>";
					} else if (baseLang.indexOf("en-") > -1) {
						ret+="<div class=\"isLoginFalse\" style=\"display:block; margin-top:10px;\">\r\n"
								+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
								+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">PDF datasheet and 2D/3D CAD\r\n"
								+ "are services available only to registered users.\r\n"
								+ "Please log in to use these services.</div>\r\n"
								+ "<a href=\"javascript:void(0)\" \r\n"
								+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Login</span></a>\r\n"
								+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
								+ "<button class=\"button neutral solid disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">PDF datasheet</span></button>\r\n"
								+ "<button class=\"button neutral outline disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">2D/3D CAD</span></button>\r\n"
								+ "</div>\r\n"
								+ "</div>\r\n"
								+ "</div>";
					}
					
					// 簡易図面、PDFデータシート
					URL u = new URL(request.getRequestURL().toString());
					String host = u.getHost();
					// 2024/9/27 英語対応完了のため、if() コメントアウト
					// 2024/10/04 中国語は非表示。コメントアウトを外して中国除外
					if (baseLang.equals("ja-jp") || baseLang.indexOf("en-") > -1) {
						if (o.getCadParameter() != null && o.getCadParameter().isEmpty() == false) {
							String param = o.getCadParameter();
							if (param.indexOf("\\\\") > 0) param = param.replace("\\\\", "/");
							else if (param.indexOf("\\") > 0) param = param.replace("\\", "/");
//							ret+="<script src='https://service.web2cad.co.jp/maker/techdemo/webcomponents/8.1.0/api/js/thirdparty.min.js'></script>";
//							ret+="<script src='https://service.web2cad.co.jp/maker/techdemo/webcomponents/8.1.0/api/js/psol.components.min.js'></script>";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/thirdparty.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/psol.components.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/axios.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/crypto-js.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/js.cookie.min.js'></script>\r\n";
							ret+="<script src='https://www.smcworld.com/assets/js/cadenas/pdf_datasheet.js'></script>\r\n";
							ret+="<div class=\"clear\"></div>\r\n";
							ret+="<div id=\"pdf_datasheet_login\" class=\"isLoginTrue\">\r\n";
							ret+="  <a href=\"javascript:void(0);\" onclick=\"downloadCAD('"+param+"', 'PDFDATASHEET', '"+cadenas.get("ca_language")+"');\" class=\"bt_3s pdf_datasheet\" ><span>"+pdfDatasheet+"</span></a>\r\n";
							// 3/1 3D CAD  on 簡易図面、PDFデータシートの下へ margin-top:10px margin-right: 15px;
							String ca_info2 = cadenas.get("ca_info");
							String ca_varset2 = cadenas.get("ca_varset");
							if (ca_info2 != null && ca_info2.isEmpty() == false) {
								ret +="  <a href=\"/products/cad/"+lang+"/"
									+ "?info="+ca_info2+"&varset="+ca_varset2+"&language="+cadenas.get("ca_language")+"&name="+cadenas.get("ca_name")+"\" class=\"bt_3s cad23d\" target=\"_blank\"><span>2D/3D CAD</span></a>\r\n";
							}
							
							ret+="</div>";
						}
					}
					
					// 代替品番
					if (o.getConsolidation() != null) {
						ret+= "<div class=\"clear\"></div>\r\n" +
								"                    <div class=\"s3s_consolidation\">\r\n" +
								"                        <p style=\"\">\r\n" +
								o.getConsolidation().description+"\r\n" +
								"                        </p>\r\n" ;
						// 代替品番がある場合 品番詳細表示
						if (o.getConsolidation().newModel != null && o.getConsolidation().newModel.isEmpty() == false) {
							String newModel = "新品番を表示";
							if (baseLang.indexOf("en-") > -1) {
								newModel = "New Product Number";
								ret+= "                        →<a href=\"/webcatalog/s3s/"+lang+"/detail/"+sid+"/?partNumber="+o.getConsolidation().newModel+"\" target=\"_parent\">"+newModel+"</a>\r\n\r\n";
							} else if (baseLang.indexOf("zh-") > -1) {
								newModel = "新型号";
								ret+= "                        →<a href=\"/webcatalog/s3s/"+lang+"/detail/"+sid+"?partNumber="+o.getConsolidation().newModel+"\" target=\"_parent\">"+newModel+"</a>\r\n\r\n";
							} else {
								ret+= "                        →<a href=\"/webcatalog/s3s/ja-jp/detail/"+sid+"/?partNumber="+o.getConsolidation().newModel+"\" target=\"_parent\">"+newModel+"</a>\r\n\r\n";
							}
						} else {
							// 代替品番がない場合 新シリーズIDで選定画面を呼ぶ
							String newModel = "新シリーズで選定する";
							if (baseLang.indexOf("en-") > -1) {
								newModel = "New Series";
								if (sid != null && sid.isEmpty() == false) {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"/"+sid+"/\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								} else {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								}
							} else if (baseLang.indexOf("zh-") > -1) {
								newModel = "新系列";
								if (sid != null && sid.isEmpty() == false) {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"/"+sid+"/\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								} else {
									ret+= "                     →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+o.getConsolidation().newTypeID+"\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								}
							} else {
								if (sid != null && sid.isEmpty() == false) {
									ret+= "                     →<a href=\"/webcatalog/s3s/ja-jp/frame/"+o.getConsolidation().newTypeID+"/"+sid+"/\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								} else {
									ret+= "                     →<a href=\"/webcatalog/s3s/ja-jp/frame/"+o.getConsolidation().newTypeID+"\" target=\"_parent\">"+newModel+"</a>\r\n" ;
								}
							}
						}
						ret+="\r\n" +
							"                    </div>";
					}
					ret+="</td>\r\n" +
							"            </tr>\r\n" +
							"        </tbody>\r\n" +
							"    </table>";
					// 仕様
					String Specifications = "仕様";
					if (baseLang.indexOf("en-") > -1) {
						Specifications = "Specifications";
					} else if (baseLang.indexOf("zh-") > -1) {
						Specifications = "规格";
					}
					ret+="<div class=\"s3s_detail_pain\">\r\n" +
							"        <div class=\"s3s_detail_h\">"+Specifications+": "+o.getProductName()+"</div>\r\n" ;
					String str1 = "フィールドの情報";
					String str2 = "フィールドの値";
					String str3 = "値の情報";
					if (baseLang.indexOf("en-") > -1) {
						str1 = "Field";
						str2 = "Value";
						str3 = "Value Details";
					} else if (baseLang.indexOf("zh-") > -1) {
						str1 = "可选项信息";
						str2 = "可选项的值";
						str3 = "值的信息";
					}
					if (o.getSerial() != null) {
						ret+="        <div class=\"s3s_detail_inner\"> "+o.getSerial().getDescription()+"\r\n" +
							"            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"s3s_prop_tbl\">\r\n" +
							"                <tbody>\r\n" +
							"                    <tr>\r\n" +
							"                        <th scope=\"col\">"+str1+"</th>\r\n" +
							"                        <th scope=\"col\">"+str2+"</th>\r\n" +
							"                        <th scope=\"col\" class=\"last\">"+str3+"</th>\r\n" +
							"                    </tr>\r\n" ;
						if (o.getProperties() != null) {
							for(Prop prop : o.getProperties()) {
								ret+="                        <tr>\r\n" +
								"                            <td>"+prop.getName()+"</td>\r\n" +
								"                            <td>"+prop.getValue()+"</td>\r\n" +
								"                            <td>"+prop.getDescription()+"</td>\r\n" +
								"                        </tr>\r\n";
							}
						}
						ret+="                </tbody>\r\n" +
							"            </table>\r\n" +
							"        </div>\r\n" +
							"        <!--inner-->\r\n" +
							"        </logic:notEmpty>\r\n" +
							"    </div>\r\n" +
							"    <!--pain-->\r\n" +
							"<br />";
					}
					// 関連製品 Accessories
					String Related = "関連製品";
					if (baseLang.indexOf("en-") > -1) {
						Related = "Related Products";
					} else if (baseLang.indexOf("zh-") > -1) {
						Related = "相关产品";
					}
					String str21 = "フィールドの情報";
					String str22 = "フィールドの値";
					String str23 = "値の情報";
					if (baseLang.indexOf("en-") > -1) {
						str21 = "Image";
						str22 = "Part Number";
						str23 = "Part Name";
					} else if (baseLang.indexOf("zh-") > -1) {
						str21 = "图片";
						str22 = "型号";
						str23 = "名称";
					}
					if (o.getRelatedProducts() != null && o.getRelatedProducts().getAccessories() != null && o.getRelatedProducts().getAccessories().size() > 0) {
						ret+="<br />"+
							"        <div class=\"s3s_detail_pain\">\r\n" +
							"            <div class=\"s3s_detail_h\">"+Related+"</div>\r\n" +
							"            <div class=\"s3s_detail_inner\">\r\n" +
							"\r\n" +
							"                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"s3s_prop_tbl\">\r\n" +
							"                    <tbody>\r\n" +
							"                        <tr>\r\n" +
							"                            <th scope=\"col\">"+str21+"</th>\r\n" +
							"                            <th scope=\"col\">"+str22+"</th>\r\n" +
							"                            <th scope=\"col\" class=\"last\">"+str23+"</th>\r\n" +
							"                        </tr>\r\n" ;
						for(Accessories acc : o.getRelatedProducts().getAccessories()) {
							ret+="                            <tr>\r\n" +
							"                                <td class=\"tdc\" width=\"15%\">\r\n" ;
							if (acc.getPicture() != null && acc.getPicture().isEmpty() == false) {
								ret+="                               <img src=\""+acc.getPictureS()+"\" class=\"s3s_picture_S\"/>\r\n";
							} else {
								ret+="                               －\r\n";
							}
							ret+="                                </td>\r\n" +
							"                                <td>"+acc.getModel()+"</td>\r\n" +
							"                                <td>"+acc.getDescription()+"</td>\r\n" +
							"                            </tr>\r\n" ;
						}
						ret+="                    </tbody>\r\n" +
							"                </table>\r\n" +
							"\r\n" +
							"            </div><!--inner-->\r\n" +
							"        </div><!--pain-->\r\n";
					}
					// 関連製品 suitableSeries
					if (o.getRelatedProducts() != null && o.getRelatedProducts().getSuitableSeries() != null && o.getRelatedProducts().getSuitableSeries().size() > 0) {
						ret+="<br />"+
							"<div class=\"s3s_detail_pain\">\r\n" +
							"            <div class=\"s3s_detail_h\">"+Related+"</div>\r\n" +
							"            <div class=\"s3s_detail_inner\">\r\n" +
							"\r\n" +
							"                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"s3s_prop_tbl\">\r\n" +
							"                        <tbody>\r\n" +
							"                            <tr>\r\n" +
							"                                <th scope=\"col\">"+str21+"</th>\r\n" +
							"                                <th scope=\"col\">"+str22+"</th>\r\n" +
							"                                <th scope=\"col\" class=\"last\">"+Related+"</th>\r\n" +
							"                            </tr>\r\n" ;
						for(SuitableSeries suitable : o.getRelatedProducts().getSuitableSeries()) {
							ret+="                                <tr>\r\n" +
							"                                    <td class=\"tdc\" width=\"15%\">\r\n" ;
							if (suitable.getPicture() != null && suitable.getPicture().isEmpty() == false) {
								ret+="                                        <img src=\""+suitable.getPictureS()+"\" class=\"s3s_picture_S\"/>\r\n";
							} else {
								ret+="                                        －\r\n" ;
							}
							ret+="                                    </td>\r\n" +
							"                                    <td>"+suitable.getName()+"</td>\r\n" +
							"                                    <td class=\"tdc\">\r\n" ;
							if (sid != null && sid.isEmpty() == false) {
								ret+="                                    →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+suitable.getTypeID()+"/"+sid+"/\" target=\"_parent\">"+sButton+"</a>\r\n" ;
							} else {
								ret+="                                    →<a href=\"/webcatalog/s3s/"+lang+"/frame/"+suitable.getTypeID()+"\" target=\"_parent\">"+sButton+"</a>\r\n" ;
							}
							ret+="                                    </td>\r\n" +
							"                                </tr>\r\n" ;
						}
						ret+="                        </tbody>\r\n" +
							"                </table>\r\n" +
							"\r\n" +
							"            </div><!-- inner -->\r\n" +
							"        </div><!-- pain -->";
					}
					// シリーズ
					String str31 = "名称";
					String str32 = "カタログ";
					if (baseLang.indexOf("en-") > -1) {
						str31 = "Name";
						str32 = "Catalog";
					} else if (baseLang.indexOf("zh-") > -1) {
						str31 = "名称";
						str32 = "产品目录";
					}
					if (seriesList != null && seriesList.size() > 0) {
						ret+="<div id=\"modal-a\" aria-hidden=\"true\" class=\"micromodal-slide modal\">\r\n" +
								"                        <div class=\"modal__overlay\" tabindex=\"-1\" data-micromodal-close=\"\">\r\n" +
								"                        <div class=\"modal__container\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"modal-1-title\" style=\"height:200px;\">\r\n" +
								"                            \r\n" +
								"                            <div id=\"modal-1-content\" class=\"modal__content\">\r\n" +
								"                                <table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:900px\">\r\n" +
								"    <thead>\r\n" +
								"        <tr>\r\n" +
								"            <th>"+str31+"</th>\r\n" +
								"            <th>"+str32+"</th>\r\n" +
								"        </tr>\r\n" +
								"    </thead>\r\n" +
								"    <tbody>\r\n" ;
						for(Series s : seriesList) {
							ret+="        <tr>\r\n" +
								"            <td>"+s.getModelNumber()+":"+s.getName()+"</td>\r\n" +
								"            <td class=\"tdc\"><a href=\"/webcatalog/"+lang+"/series/"+s.getModelNumber()+"\" style=\"color:#0074bf\">"+sButton+"</a></td>\r\n" +
								"        </tr>\r\n" ;
						}
						ret+="    </tbody>\r\n" +
								"                                    </table>\r\n" +
								"                            </div>\r\n" +
								"                        </div>\r\n" +
								"                        </div>\r\n" +
								"                    </div>";
					}

				} else if (o_result.getCode().equals("23")) {
					result = "search";
					S3SSearchResult s_result = JSON.decode(json_result, S3SSearchResult.class);
//					request.setAttribute("result", s_result);
					ret =header.replace("</head>", _3sHeader+"</head>");
					if (baseLang.indexOf("ja-") > -1) {
						ret+="<div id=\"content\">\r\n" +
								"<div class=\"one_column_cont\">\r\n" +
								"\r\n" +
								"\r\n" +
								"<div class=\"form_box w_form_box\">\r\n" +
								"<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/ja-jp/search3S/\">\r\n" +
								"\r\n" +
								"    <div class=\"sform\">\r\n" +
								"      <label for=\"k\">製品検索</label>\r\n" +
								"      <div class=\"input\">\r\n" +
								"      <input type=\"text\" name=\"kwItem\" value=\"\" id=\"kwItem\" class=\"k\" placeholder=\"フル品番の一部で検索してください\">\r\n" +
								"      <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"searchProductsKeyword('ja-jp');return false;\" onkeypress=\"return inactiveEnter(event);\"></span>\r\n" +
								"      </div>\r\n" +
								"    </div>\r\n" +
								"</form>\r\n"+
								"</div>\r\n"+
								"</div>\r\n";
					} else if (baseLang.indexOf("en-") > -1) {
						ret+="<div id=\"content\">\r\n" +
								"<div class=\"one_column_cont\">\r\n" +
								"\r\n" +
								"\r\n" +
								"<div class=\"form_box w_form_box\">\r\n" +
								"<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/"+lang+"/search3S/\">\r\n" +
								"\r\n" +
								"    <div class=\"sform\">\r\n" +
								"      <label for=\"k\">Product</label>\r\n" +
								"      <div class=\"input\">\r\n" +
								"      <input type=\"text\" name=\"kwItem\" value=\"\" id=\"kwItem\" class=\"k\" placeholder=\"Product Number Check\">\r\n" +
								"      <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"searchProductsKeyword('en-jp');return false;\" onkeypress=\"return inactiveEnter(event);\"></span>\r\n" +
								"      </div>\r\n" +
								"    </div>\r\n" +
								"</form>"+
								"</div>\r\n"+
								"</div>\r\n";
					} else if (baseLang.indexOf("zh-") > -1) {
						ret+="<div id=\"content\">\r\n" +
								"<div class=\"one_column_cont\">\r\n" +
								"\r\n" +
								"\r\n" +
								"<div class=\"form_box w_form_box\">\r\n" +
								"<form name=\"SearchPsItemForm\" id=\"prod_search_form\" method=\"GET\" action=\"/webcatalog/"+lang+"/search3S/\">\r\n" +
								"\r\n" +
								"    <div class=\"sform\">\r\n" +
								"      <label for=\"k\">产品目录</label>\r\n" +
								"      <div class=\"input\">\r\n" +
								"      <input type=\"text\" name=\"kwItem\" value=\"\" id=\"kwItem\" class=\"k\" placeholder=\"型号确认\">\r\n" +
								"      <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"searchProductsKeyword('zh-cn');return false;\" onkeypress=\"return inactiveEnter(event);\"></span>\r\n" +
								"      </div>\r\n" +
								"    </div>\r\n" +
								"</form>"+
								"</div>\r\n"+
								"</div>\r\n";
					}
					if (s_result.getCandidate().size() == 0) {
						if (baseLang.indexOf("ja-") > -1) {
							ret+="<div class=\"no_result\">\r\n" +
								"                検索条件にヒットする製品が見つかりませんでした。\r\n" +
								"            </div>";
						} else if (baseLang.indexOf("en-") > -1) {
							ret+="<div class=\"no_result\">\r\n" +
									"                There were no hits.\r\n" +
									"            </div>";
						} else if (baseLang.indexOf("zh-") > -1) {
							ret+="<div class=\"no_result\">\r\n" +
									"                未找到符合条件的产品。\r\n" +
									"            </div>";
						}
					} else {
						if (baseLang.indexOf("ja-") > -1) {
							ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
								"    <tr>\r\n" +
								"    <th width=\"10%\">画像</th>\r\n" +
								"    <th width=\"45%\">名称</th>\r\n" +
								"    <th width=\"15%\">シリーズ</th>\r\n" +
								"    </tr>";
						} else if (baseLang.indexOf("en-") > -1) {
							ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
									"    <tr>\r\n" +
									"    <th width=\"10%\">Image</th>\r\n" +
									"    <th width=\"45%\">Name</th>\r\n" +
									"    <th width=\"15%\">Series</th>\r\n" +
									"    </tr>";
						} else if (baseLang.indexOf("zh-") > -1) {
							ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
									"    <tr>\r\n" +
									"    <th width=\"10%\">图片</th>\r\n" +
									"    <th width=\"45%\">名称</th>\r\n" +
									"    <th width=\"15%\">系列</th>\r\n" +
									"    </tr>";
						}
						for(Candidate c : s_result.getCandidate()) {
							ret+="<tr>\r\n" +
									"    <td class=\"tdc\"><img src=\""+c.getPicture()+"\" class=\"s3s_picture_S\"/></td>\r\n" +
									"    <td>"+c.getName()+"</td>\r\n" +
									"    <td>"+c.getSeries()+"</td>\r\n" +
									"    </tr>";
						}
						ret+="</table>";
					}
				}
				// スピナー  
				ret += "<!-- loding -->\r\n"
						+ "    <div id=\"bt_3s_overlay\">\r\n"
						+ "        <div class=\"cv-spinner\">\r\n"
						+ "            <span class=\"spinner\"></span>\r\n"
						+ "        </div>\r\n"
						+ "    </div>";
				ret += "</div><!--content-->";
				if (baseLang.equals("zh-cn")) {
					footer = footer.replaceFirst("<footer>", AppConfig.JingSocialInc + AppConfig.JingSocial.replace("$XXX$", partNumber).replace("$YYY$", "1")+"\r\n<footer>");
				}
				ret += footer;
			} // if (json) else if (is2026)
			if (langObj.isVersion()) {
				// 変換処理
				ret = html.changeLang(ret, baseLang, lang);
			}
		} catch (BadRequestException e) {
			log.error("response=" + e.getResponse().toString());
			throw new ResponseStatusException(
					  HttpStatus.BAD_REQUEST, "BadRequest.");
		} catch(InternalServerErrorException e) {
			log.error(e.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
		} catch(ProcessingException e) {
			log.error(e.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.FORBIDDEN, "Timeout");
		} catch (Throwable ex) {
			log.error(ex.getMessage());
			throw new ResponseStatusException(
					  HttpStatus.FORBIDDEN, "FORBIDDEN!");
		}

		return ret;
	}
	
	// 簡易図面の非ログイン時の戻り
	// assets/js/cadenas/pdf_datasheet.jsに処理あり。
	// 1995f864-9b55-411b-b95e-a532d2a944ab
	@GetMapping(value={"/{lang}/callback", "/{lang}/callback/"})
	@CrossOrigin(origins= {"http://192.168.0.36","http://192.168.0.34","http://153.120.135.17","https://153.120.135.17",
			"http://localhost:8080","http://webcatalog.smcworld.com","http://dev1.smcworld.com","http://ap1.smcworld.com","http://ap2.smcworld.com","https://ap1admin.smcworld.com",
			"http://www.smc3s.com", "https://www.smc3s.com", "http://133.242.52.163", "http://153.120.139.192",
			"https://3sapi.smcworld.com","http://3sapi.smcworld.com", "https://test.smcworld.com", "https://cdn.smcworld.com", "https://www.smcworld.com", "https://www.smc.com.cn"},allowCredentials="true")
	public String getCallback(@PathVariable(name = "lang", required = true) String lang,
			HttpServletRequest request) {

		String ret = "<html>\r\n" +
				"\r\n" +
				"<head>\r\n" +
				"		<script type=\"text/javascript\" src='https://www.smcworld.com/assets/js/cadenas/axios.min.js'></script>\r\n" +
				"		<script type=\"text/javascript\" src='https://www.smcworld.com/assets/js/cadenas/crypto-js.min.js'></script>\r\n"+
				"		<script type=\"text/javascript\" src='https://www.smcworld.com/assets/js/cadenas/js.cookie.min.js'></script>\r\n"+
				"		<script type=\"text/javascript\" src=\"https://www.smcworld.com/assets/js/cadenas/pdf_datasheet_callback.js\"></script>\r\n" +
				"	</head>\r\n" +
				"\r\n" +
				"<body><p id=\"err\"></p></body>"+
				"</html>";

		return ret;
	}
	// iframeの親へ選択されたpartNumberを渡す
	@GetMapping(value={"/{lang}/pre/", "/{lang}/pre", "/{lang}/pre/{typeid}", "/{lang}/pre/{typeid}/", "/{lang}/pre/{typeid}/{sid}", "/{lang}/pre/{typeid}/{sid}/",
			 "/{lang}/pre/{typeid}/{sid}/{sid2}", "/{lang}/pre/{typeid}/{sid}/{sid2}/",
			 "/{lang}/pre/{typeid}/{sid}/{sid2}/{sid3}", "/{lang}/pre/{typeid}/{sid}/{sid2}/{sid3}/"}, produces="text/html;charset=UTF-8")
	@CrossOrigin(origins= {"http://192.168.0.36","http://192.168.0.34","http://153.120.135.17","https://153.120.135.17",
			"http://localhost:8080","http://webcatalog.smcworld.com","http://dev1.smcworld.com","http://ap1.smcworld.com","http://ap2.smcworld.com","https://ap1admin.smcworld.com",
			"http://www.smc3s.com", "https://www.smc3s.com", "http://133.242.52.163", "http://153.120.139.192",
			"https://3sapi.smcworld.com","http://3sapi.smcworld.com", "https://test.smcworld.com", "https://cdn.smcworld.com", "https://www.smcworld.com", "https://www.smc.com.cn"},allowCredentials="true")
	public String getPre3S(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "typeid", required = false) String typeid,
			@PathVariable(name = "sid", required = false) String sid,
			@PathVariable(name = "sid2", required = false) String sid2,
			@PathVariable(name = "sid3", required = false) String sid3,
			@RequestParam(name = "partNumber", required = true) String partNumber,
			@RequestParam(name = "result", required = false) String result,
			HttpServletRequest request) {

		String url = "";
		try {
			URL u = new URL(request.getRequestURL().toString());
			String host = u.getHost();
			if (host.indexOf("www.smcworld.com") > -1) {
				url = "https://www.smcworld.com/";
			} else if (host.indexOf("www.smc.com.cn") > -1) {
				url = "https://www.smc.com.cn/";
			} else if (host.indexOf("test.smcworld.com") > -1) {
				url = "https://test.smcworld.com/";
			} else if (host.indexOf("cdn.smcworld.com") > -1) {
				url = "https://cdn.smcworld.com/";
			} else if (host.indexOf("ap1admin.smcworld.com") > -1) {
				url = "https://ap1admin.smcworld.com/";
			} else if (host.indexOf("localhost") > -1) {
				url = "http://localhost:8080/";
			} else {
				url = "http://"+host+"/";
			}
		} catch (MalformedURLException e) {
		}
		String ret = "<html>\r\n" +
				"\r\n" +
				"<head>\r\n" +
				"		<script type=\"text/javascript\">\r\n" +
				"			//Get the callback PartNumber parameter\r\n" +
				"			function GetQueryString(name)\r\n" +
				"			{\r\n" +
				"				 var reg = new RegExp(\"(^|&)\"+ name +\"=([^&]*)(&|$)\");\r\n" +
				"				 var r = window.location.search.substr(1).match(reg);\r\n" +
				"				 if (r != null) return unescape(r[2]); return null;\r\n" +
				"			}\r\n" +
				"\r\n" +
				"			var partNumber = GetQueryString(\"partNumber\");\r\n" +
				"			window.onload = function(){\r\n" +
				"				if (partNumber != '' && (typeof partNumber) != 'object' && partNumber.toString().indexOf('[object Object]') == -1){\r\n" +
				"					// calling the a function in homepage.html\r\n" +
				"					window.top.postMessage(partNumber, \""+url+"\");\r\n" +
				"				}\r\n" +
				"			};\r\n" +
				"\r\n" +
				"		</script>\r\n" +
				"	</head>\r\n" +
				"\r\n" +
				"</html>";

		return ret;
	}


	// 「品番確認」のiframe表示
	// idはsid JSY-P-E
	// ※選定PGのロータリーアクチュエーターから呼ばれている。
	// userIDが設定されている場合はmodelNoにtypeidを設定。
	@GetMapping(value={"/{lang}/frame/{typeid}", "/{lang}/frame/{typeid}/", "/{lang}/frame/{typeid}/{sid}", "/{lang}/frame/{typeid}/{sid}/",
			"/{lang}/frame/{typeid}/{sid}/{sid2}", "/{lang}/frame/{typeid}/{sid}/{sid2}/",
			"/{lang}/frame/{typeid}/{sid}/{sid2}/{sid3}", "/{lang}/frame/{typeid}/{sid}/{sid2}/{sid3}/",}, produces="text/html;charset=UTF-8")
	@CrossOrigin(origins= {"http://192.168.0.36","http://192.168.0.34","http://153.120.135.17","https://153.120.135.17",
			"http://localhost:8080","http://dev1.smcworld.com","http://ap1.smcworld.com","http://ap2.smcworld.com","https://ap1admin.smcworld.com",
			"http://www.smc3s.com", "https://www.smc3s.com", "http://133.242.52.163", "http://153.120.139.192",
			"https://dev1admin.smcworld.com", "https://ap1admin.smcworld.com",
			"http://3sapi.smcworld.com","https://3sapi.smcworld.com", "https://test.smcworld.com", "https://cdn.smcworld.com", "https://www.smcworld.com", "https://www.smc.com.cn"},allowCredentials="true")
	public String getFrame3S(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "typeid", required = true) String typeid,
			@PathVariable(name = "sid", required = false) String sid,
			@PathVariable(name = "sid2", required = false) String sid2, // /が入っているSeriesID
			@PathVariable(name = "sid3", required = false) String sid3, // /が入っているSeriesID
			@RequestParam(name="userID", required = false) String userID,
			HttpServletRequest request) {

		String ret = null;
		ErrorObject err = new ErrorObject();

		Lang langObj = langService.getLang(lang, err);
		if (langObj == null) {
			log.error("Lang is Bad or Empty! lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "Lang is Empty!");
		}

		// ※選定PGのロータリーアクチュエーターから呼ばれている。
		// userIDが設定されている場合はmodelNoにtypeidを設定。
		String searchKw = "typeID";
		if (userID == null || userID.equals("")) {
			userID = "JP_test";
		} else {
			searchKw = "modelNo";
		}
		if (typeid.indexOf("[]") > -1) {
			typeid = typeid.replace("[]",  "");
			typeid = typeid.replace("--", "-");
			if (typeid.lastIndexOf("-") == typeid.length()-1) {
				typeid = typeid.substring(0, typeid.length()-1);
			}
		}

		String baseLang = lang;
		if (langObj.isVersion()) {
			baseLang = langObj.getBaseLang();
		}
		Locale baseLocale = getLocale(baseLang);
		
		String reqUrl = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(reqUrl);
		
		ModelState m = ModelState.PROD;
		if (isTestSite) m = ModelState.TEST;
		Boolean isActive = true;
		if (isTestSite) isActive = null; 

		Template t = templateService.getTemplateFromBean(baseLang, m);
		String header = t.getHeader();
		String footer = t.getFooter();
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			header = toT.getHeader();
			footer = toT.getFooter();
		}
		boolean is2026 = t.is2026();

		String catpan = "<div class=\"catpan\">\r\n" +
				"製品情報&nbsp;»&nbsp;\r\n" +
				"<a href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n" +
				"</div>";
		if (is2026) {
			header += "<main class=\"relative bg-motif s-bg-image-none m-bg-image-none\">\r\n";
			catpan = " <div class=\"container-1600\">\r\n"
					+ "\r\n"
					+ "        <div class=\"px72 py8 s-px16 s-py8 m-px16 m-py8\">\r\n"
					+ "          <nav class=\"breadcrumb leading-normal text-sm\">\r\n"
					+ "            <ol class=\"f fm gap-8 s-text-xs m-text-xs\">\r\n"
					+ "              <li>製品情報\r\n"
					+ "              </li>\r\n"
					+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
					+ "              <li><a class=\"breadcrumb-item\" href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n"
					+ "              </li>\r\n"
					+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
					+ "              <li><a class=\"breadcrumb-item\" href=\"/\">品番確認</a>\r\n"
					+ "              </li>\r\n"
					+ "            </ol>\r\n"
					+ "          </nav>\r\n"
					+ "        </div>\r\n"
					+ "";
		}
		String lang3S = "ja-JP";
		String button = "詳細表示";
		String numberCheck = "品番確認";
		if (baseLang.indexOf("ja-") > -1) {
			header+=catpan;
		} else if (baseLang.indexOf("en-") > -1) {
			catpan = catpan.replace("製品情報", "Product Information");
			catpan = catpan.replace("WEBカタログ", "WEB Catalog");
			numberCheck = "Product Number Check";
			header+= catpan.replace("品番確認", numberCheck);
			lang3S = "en-US";
			button = "Details";
		} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
			catpan = catpan.replace("製品情報", "产品信息");
			catpan = catpan.replace("WEBカタログ", "产品目录");
			numberCheck = "型号确认";
			header+= catpan.replace("品番確認", numberCheck);
			lang3S = "zh-CHS";
			button = "显示详情";
		} else if (baseLang.equals("zh-tw")) {
			// 3Sはとりあえず簡体語
			catpan = catpan.replace("製品情報", "產品信息");
			catpan = catpan.replace("WEBカタログ", "產品目錄");
			numberCheck = "型號確認";
			header+= catpan.replace("品番確認", numberCheck);
			lang3S = "zh-CHS";
			button = "显示详情";
		}

		if (sid != null && sid.isEmpty() == false) {
			if (sid2 != null && sid2.isEmpty() == false) sid = sid+"/"+sid2;
			if (sid3 != null && sid3.isEmpty() == false) sid = sid+"/"+sid3;
			String tmp = "<div class=\"catpan\"><a href=\"/webcatalog/s3s/ja-jp/list/{sid}\" class=\"bt_3s\">一覧に戻る</a></div>\r\n" +
			"        <div class=\"clear\"></div>";
			if (is2026) {
				tmp = "<div class=\"catpan px72 py8 s-px16 s-py8 m-px16 m-py8 text-sm\">\r\n"
						+ "<a class=\"breadcrumb-item\" href=\"/webcatalog/s3s/ja-jp/list/{sid}\">\r\n"
						+ "    <img class=\"s16 pt4 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left.svg\" alt=\"\" title=\"\">\r\n"
						+ "    <span>一覧に戻る</span>\r\n"
						+ "</a></div>";
			}
			tmp = tmp.replace("{sid}", sid);
			if (baseLang.indexOf("en-") > -1) {
				tmp = tmp.replace("/ja-jp/", "/"+baseLang+"/");
				tmp = tmp.replace("一覧に戻る", "Return to List");
			} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
				tmp = tmp.replace("/ja-jp/", "/"+baseLang+"/");
				tmp = tmp.replace("一覧に戻る", "返回列表");
			} else if (baseLang.equals("zh-tw") ) {
				tmp = tmp.replace("/ja-jp/", "/"+baseLang+"/");
				tmp = tmp.replace("一覧に戻る", "返回列表");
			}
			header+= tmp;
		} else {
			sid = "";
		}
		ret = header;
		// hook先を指定
		String host = "";
		String url = "";
		try {
			URL u = new URL(request.getRequestURL().toString());
			host = u.getHost();
		} catch (MalformedURLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		// 後ろの/はあえて付けない。EventListenerで消される。
		if (host.indexOf("www.smcworld.com") > -1) {
			url = "https://www.smcworld.com";
		} else if (host.indexOf("www.smc.com.cn") > -1) {
			url = "https://www.smc.com.cn";
		} else if (host.indexOf("test.smcworld.com") > -1) {
			url = "https://test.smcworld.com";
		} else if (host.indexOf("cdn.smcworld.com") > -1) {
			url = "https://cdn.smcworld.com";
		} else if (host.indexOf("ap1admin.smcworld.com") > -1) {
			url = "https://ap1admin.smcworld.com";
		} else if (host.indexOf("localhost") > -1) {
			url = "https://www.smcworld.com";
		} else {
			url = "http://"+host;
		}
//		String frame = "https://3sapi.smcworld.com/Product/SelectIFrame?typeID="+typeid+"&language="+lang3S+"&userID=JP_test&btnLabel="+button+
//		"&hookUrl=https://www.smcworld.com/webcatalog/"+lang+"/detail/?sid="+sid+"&partNumber=";
//		String frame = "https://3sapi.smcworld.com/Product/SelectIFrame?typeID="+typeid+"&language="+lang3S+"&userID=JP_test&btnLabel="+button+
//		"&hookUrl="+url+"/webcatalog/s3s/"+lang+"/pre/"+sid+"/?partNumber=";
		try {
			String frame = url+"/THREES/Product/SelectIFrame?"+searchKw+"="+typeid+"&language="+lang3S+"&userID="+userID+"&btnLabel="+button;
			
			String hookUrl = url+"/webcatalog/s3s/"+lang+"/pre/"+typeid+"/";
			if (sid != null && sid.isEmpty() == false) {
				hookUrl+=sid+"/";
			}
			if (userID != null && userID.isEmpty() == false) {
				hookUrl+="?userID="+userID+"&partNumber=";
			} else {
				hookUrl+="?partNumber=";
			}
			frame += "&hookUrl="+URLEncoder.encode(hookUrl, StandardCharsets.UTF_8.toString());
			ret += "    <center>\r\n" +
					"    <iframe src=\""+frame+"\" width=\"100%\" height=\"1200\" style=\"margin-top: 0px;border: none;max-width:1000px;\"></iframe>\r\n" +
					"    </center>\r\n" +
					"\r\n" ;
		} catch(Exception e) {
			log.error("getFrame3S()", e.getMessage());
			ret += "    <center>\r\n" +
					"    <p color=\"red\">URLの生成に失敗しました。URL generation failed.</p>\r\n" +
					"    </center>\r\n" +
					"\r\n" ;
		}
/*
		ret+="            <script>\r\n" +
		"                //Display the returned model number\r\n" +
		"                function a(modelNo){\r\n" +
		"                    //$(\"#modellist\").append(\"<span>\"+modelNo+\"</span></br>\");\r\n" +
		"                    //You should redirect your page here, please change the localhost to your actual ip addr.\r\n" +
		"                    window.location.href=\""+url+"/webcatalog/s3s/"+lang+"/pre/"+sid+"/?partNumber=\" + modelNo;\r\n" +
		"                }\r\n" +
		"            </script>\r\n" +
		"\r\n" ;
*/
		ret+="            <script>\r\n" +
				"                //Display the returned model number\r\n" +
				"                window.addEventListener(\"message\", receiveMessage);\r\n" +
				"                function receiveMessage(event) {\r\n" +
				"                    if (event.origin !== \""+url+"\") {\r\n" +
//				"                                           // ↑ 送信元のオリジン\r\n" +
//				"                           alert(\"オリジン違反\"+event.origin+\""+url+"\");\r\n" +
				"                           return;\r\n" +
				"                    }\r\n" ;
		ret +=	"                    if ((typeof (event.data)) == 'object') return;\r\n";
//				"alert(event.data);" +
			if (sid != null && sid.isEmpty() == false) {
				if (userID != null && userID.isEmpty() == false) {
					ret+="                    window.location.href=\""+url+"/webcatalog/s3s/"+lang+"/detail/"+typeid+"/"+sid+"/?userID="+userID+"&partNumber=\" + event.data;\r\n" ;
				} else {
					ret+="                    window.location.href=\""+url+"/webcatalog/s3s/"+lang+"/detail/"+typeid+"/"+sid+"/?partNumber=\" + event.data;\r\n" ;
				}
			} else {
				if (userID != null && userID.isEmpty() == false) {
					ret+="                    window.location.href=\""+url+"/webcatalog/s3s/"+lang+"/detail/"+typeid+"/?userID="+userID+"&partNumber=\" + event.data;\r\n" ;
				} else {
					ret+="                    window.location.href=\""+url+"/webcatalog/s3s/"+lang+"/detail/"+typeid+"/?partNumber=\" + event.data;\r\n" ;
				}
			}
		ret+="                }\r\n" +
				"            </script>\r\n";
		ret+="\r\n"+
		"<p><br></p>\r\n" +
		"\r\n" +
		"<div class=\"clear\"></div>\r\n" +
		"</div><!--main-->";
		if (is2026) { ret += "</main>"; }
		ret += footer;
		if (langObj.isVersion()) {
			// 変換処理
			ret = html.changeLang(ret, baseLang, lang);
		}

		return ret;
	}

	// 「品番確認」の後の一覧
	// sidは JSY-P-E
	// sidは2022/8で削除
	@GetMapping(value={"/{lang}/list/{sid}", "/{lang}/list/**"}, produces="text/html;charset=UTF-8")
	public String getProductNumberList(@PathVariable(name = "lang", required = true) String lang,
			@PathVariable(name = "sid", required = false) String sid,
			@RequestParam(name="userID", required = false) String userID,
			HttpServletRequest request) {

		String ret = null;
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
		Locale baseLocale = getLocale(baseLang);

		if (sid == null) {
			String tmp = request.getRequestURI();
			if (tmp != null) {
				int s = tmp.indexOf("/list/");
				if (s > -1) {
					sid = tmp.substring(s+"/list/".length());
					if (sid.indexOf("%E3%83%BB") > -1) {
						sid = sid.replace("%E3%83%BB", "・");
					}
				}
			}
		} else if (sid.trim().equals("undefined")) {
			// 大量アクセス対策 2024/3/15
			log.error("Throw not found. sid is undefined. lang=" + lang);
			throw new ResponseStatusException(
					  HttpStatus.NOT_FOUND, "sid is undefined!");
		}
		
		String reqUrl = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(reqUrl);
		
		ModelState m = ModelState.PROD;
		if (isTestSite) m = ModelState.TEST;
		Boolean isActive = true;
		if (isTestSite) isActive = null; 

		Template t = templateService.getTemplateFromBean(baseLang, m);
		String header = t.getHeader();
		String footer = t.getFooter();
		if (langObj.isVersion()) {
			// 変換処理
			Template toT = templateService.getTemplateFromBean(lang, m);
			header = toT.getHeader();
			footer = toT.getFooter();
		}
		boolean is2026 = t.is2026();

		String lang3S = "ja-JP";
		String button = "詳細表示";
		String numberCheck = "品番確認";
		if (is2026) {
			header += "<main class=\"relative bg-motif s-bg-image-none m-bg-image-none\">\r\n";
			String catpan = " <div class=\"container-1600\">\r\n"
					+ "\r\n"
					+ "        <div class=\"px72 py8 s-px16 s-py8 m-px16 m-py8\">\r\n"
					+ "          <nav class=\"breadcrumb leading-normal text-sm\">\r\n"
					+ "            <ol class=\"f fm gap-8 s-text-xs m-text-xs\">\r\n"
					+ "              <li>製品情報\r\n"
					+ "              </li>\r\n"
					+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
					+ "              <li><a class=\"breadcrumb-item\" href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n"
					+ "              </li>\r\n"
					+ "              <li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"/></li>\r\n"
					+ "              <li><a class=\"breadcrumb-item\" href=\"/\">品番確認</a>\r\n"
					+ "              </li>\r\n"
					+ "            </ol>\r\n"
					+ "          </nav>\r\n"
					+ "        </div>\r\n"
					+ "";
			if (baseLang.indexOf("ja-") > -1) {
				header+=catpan;
			} else if (baseLang.indexOf("en-") > -1) {
				catpan = catpan.replace("製品情報", "Product Information");
				catpan = catpan.replace("WEBカタログ", "WEB Catalog");
				numberCheck = "Product Number Check";
				header+= catpan.replace("品番確認", numberCheck);
			} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
				catpan = catpan.replace("製品情報", "产品信息");
				catpan = catpan.replace("WEBカタログ", "产品目录");
				numberCheck = "型号确认";
				header+= catpan.replace("品番確認", numberCheck);
			} else if (baseLang.equals("zh-tw")) {
				catpan = catpan.replace("製品情報", "產品信息");
				catpan = catpan.replace("WEBカタログ", "產品目錄");
				numberCheck = "型號確認";
				header+= catpan.replace("品番確認", numberCheck);
			}
		} else {
			String catpan = "<div class=\"catpan\">\r\n" +
					"製品情報&nbsp;»&nbsp;\r\n" +
					"<a href=\"/webcatalog/"+lang+"/\">WEBカタログ</a>\r\n" +
					"</div>";
			if (baseLang.indexOf("ja-") > -1) {
				header+=catpan;
			} else if (baseLang.indexOf("en-") > -1) {
				catpan = catpan.replace("製品情報", "Product Information");
				header+= catpan.replace("WEBカタログ", "WEB Catalog");
			} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
				catpan = catpan.replace("製品情報", "产品信息");
				header+= catpan.replace("WEBカタログ", "产品目录");
			} else if (baseLang.equals("zh-tw")) {
				catpan = catpan.replace("製品情報", "產品信息");
				header+= catpan.replace("WEBカタログ", "產品目錄");
			}
		}
		if (baseLang.indexOf("en-") > -1) {
			lang3S = "en-US";
			button = "Details";
		} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
			lang3S = "zh-CHS";
			button = "显示详情";
		} else if (baseLang.equals("zh-tw")) {
			lang3S = "zh-CHS";
			button = "显示详情";
		}
		
		String tmp = "<div class=\"catpan\"><a href=\"/webcatalog/ja-jp/series/{sid}\" class=\"bt_3s\">製品情報に戻る</a></div>\r\n" +
		"        <div class=\"clear\"></div>";
		if (is2026) {
			tmp = "<div class=\"catpan px72 py8 s-px16 s-py8 m-px16 m-py8 text-sm\">\r\n"
					+ "<a class=\"breadcrumb-item\" href=\"/webcatalog/ja-jp/series/{sid}\">\r\n"
					+ "    <img class=\"s16 pt4 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left.svg\" alt=\"\" title=\"\">\r\n"
					+ "    <span>製品情報に戻る</span>\r\n"
					+ "</a></div>";
		}
		tmp = tmp.replace("{sid}", sid);
		if (baseLang.indexOf("en-") > -1) {
			tmp = tmp.replace("/ja-jp/", "/"+baseLang+"/");
			tmp = tmp.replace("製品情報に戻る", "Return to Product Information");
		} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
			tmp = tmp.replace("/ja-jp/", "/"+baseLang+"/");
			tmp = tmp.replace("製品情報に戻る", "返回至产品信息");
		} else if (baseLang.equals("zh-tw")) {
			tmp = tmp.replace("/ja-jp/", "/"+baseLang+"/");
			tmp = tmp.replace("-ZH\"", "-ZHTW\"");
			tmp = tmp.replace("製品情報に戻る", "返回產品情報");
		}
		if (is2026) {
			tmp = StringUtils.replace(tmp, "\"catpan\"", "\"catpan px72 py8 s-px16 s-py8 m-px16 m-py8\"");
			tmp = StringUtils.replace(tmp, "<a href=\"", "<a class=\"breadcrumb-item\" href=\"");
		}
		header+= tmp;

		JpServiceUtil util = new JpServiceUtil();

		JpServiceResult res = null;
		{
			res = util.list(request.getServletContext(), sid, baseLang);
		}
		if (res != null && res.getErrno().isEmpty() == false && res.getErrno().equals("0") && res.getData().size() > 0 )
		{
			ret = header;
			
			String strType = "型式・名称";
			String strNumber = numberCheck;
			String strDetail = "詳細";
			if (baseLang.indexOf("en-") > -1) {
				strType = "Product name";
				strNumber = "Download";
				strDetail = "Details";
			} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
				strType = "型号、名称";
				strDetail = "详情";
			} else if (baseLang.equals("zh-tw")) {
				strType = "型號、名稱";
				strDetail = "詳情";
			}

			if (is2026) {
				String title = "<div class=\"w-full mb24 px72 py8 s-px16 s-py8 m-px16 m-py8\">\r\n"
						+ "            <div class=\"mb64 m-mb36 s-mb36\">\r\n"
						+ "              <h2 class=\"text-6xl leading-tight fw5 s-fw6 s-text-3xl m-fw6 m-text-3xl\"><span class=\"text-primary\">$$$title21$$$</span><span class=\"text-base-foreground-default\">$$$title22$$$</span></h2>\r\n"
						+ "            </div>";
				title = StringUtils.replace(title, "$$$title21$$$", numberCheck.substring(0, 1));
				title = StringUtils.replace(title, "$$$title22$$$", numberCheck.substring(1));
				ret += title;
				
				ret+= "<table class=\"table\">\r\n"
						+ "                                            <thead>\r\n"
						+ "                                              <tr>\r\n" +
						"			<th class=\"th\" colspan=\"2\">"+strType+"</th>\r\n" +
						"			<th class=\"th w120 text-center\">"+strNumber+"</th>\r\n" +
						"		</tr>\r\n"
						+ "</thead><tbody>";
				for(S3SSeriesInfo s : res.getData()) {
					String url = "/webcatalog/s3s/"+lang + "/frame/"+s.getTypeId()+"/";
					if (sid != null && sid.isEmpty() == false) {
						url+=sid+"/";
					}
					if (userID != null && userID.isEmpty() == false) {
						url+="?userID="+userID;
					}
					ret += "<tr>\r\n" +
						"			<td class=\"td text-center\" colspan=\"1\"><span class=\"w100\"><img src=\""+s.getPicUrl()+"\"/></span></td>\r\n" +
						"			<td class=\"td text-sm\" colspan=\"1\">"+s.getName()+"</td>\r\n" +
						"			<td class=\"td w120 text-center\" colspan=\"1\">\r\n" +
						"					<a class=\"button secondary solid medium\" href=\""+url+"\" >"+strDetail+"</a>\r\n" +
						"			</td>\r\n" +
						"		</tr>";
				}
				ret+="</tbody>\r\n"
						+ "</table>\r\n";
				String backUrl = request.getHeader("REFERER");
				String mes = messagesource.getMessage("button.back.to.list", null,  baseLocale);
				if (backUrl != null && backUrl.isEmpty() == false && backUrl.indexOf("/webcatalog/") > -1 && backUrl.indexOf("/s3s/") == -1 ) {
					String temp = "<div class=\"f fc mt16 mb48\">\r\n"
							+ "      <a class=\"button large primary solid w264 gap-8 s-w-full m-w-full\" href=\"$$$url$$$\">\r\n"
							+ "        <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left-white.svg\" alt=\"\" title=\"\">\r\n"
							+ "        <span>$$$message$$$</span>\r\n"
							+ "      </a>\r\n"
							+ "    </div>";
					temp = StringUtils.replace(temp, "$$$url$$$", backUrl);
					temp = StringUtils.replace(temp, "$$$message$$$", mes);
					ret += temp;
				}
				
				ret+= "</div>\r\n</div>\r\n</main>"+footer;
			} else {
				// 3Sの結果を表示
				ret+="<div id=\"content\">";
				ret+="<div style=\"font-weight: bold; font-size: 18px; line-height: 22px;\">品番確認</div>";
				if (baseLang.indexOf("en-") > -1) {
					ret = ret.replace("製品検索", "Product");
					ret = ret.replace("品番確認", numberCheck);
				} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
					ret = ret.replace("製品検索", "产品目录");
					ret = ret.replace("品番確認", numberCheck);
				} else if (baseLang.equals("zh-tw")) {
					ret = ret.replace("製品検索", "產品目錄");
					ret = ret.replace("品番確認", numberCheck);
				}

				ret+= "<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
						"		<tr>\r\n" +
						"			<th colspan=\"2\" class=\"\">"+strType+"</th>\r\n" +
						"			<th class=\"last\">"+strNumber+"</th>\r\n" +
						"		</tr>";
				for(S3SSeriesInfo s : res.getData()) {
					String url = "/webcatalog/s3s/"+lang + "/frame/"+s.getTypeId()+"/";
					if (sid != null && sid.isEmpty() == false) {
						url+=sid+"/";
					}
					if (userID != null && userID.isEmpty() == false) {
						url+="?userID="+userID;
					}
					ret += "<tr>\r\n" +
						"			<td class=\"\" style=\"padding: 5px;border-right: none;width:55px;height:60px;\"><img src=\""+s.getPicUrl()+"\"/></td>\r\n" +
						"			<td class=\"\" style=\"text-align: left;padding: 5px;\">"+s.getName()+"</td>\r\n" +
						"			<td class=\"td text-center\" colspan=\"1\">\r\n" +
						"					<a href=\""+url+"\" >"+strDetail+"</a>\r\n" +
						"			</td>\r\n" +
						"		</tr>";
				}
				ret+="</table>";
				String backUrl = request.getHeader("REFERER");
				String mes = messagesource.getMessage("button.back.to.list", null,  baseLocale);
				if (backUrl != null && backUrl.isEmpty() == false && backUrl.indexOf("/webcatalog/") > -1 && backUrl.indexOf("/s3s/") == -1 ) {
					ret+= "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes);
				}
				ret+= "</div>\r\n"+footer;
			}
		} else {
			if (is2026) {
				
			} else {
				ret = header;
				ret+="<div id=\"content\">";
	
				String message = "品番確認システムに対応しておりませんので、大変お手数ですが、カタログをご参照ください。";
				if (baseLang.indexOf("en-") > -1) {
					message = "We do not support the part number confirmation system, so we are sorry to trouble you, but please refer to the catalog.";
				} else if (baseLang.equals("zh-cn") || baseLang.equals("zh-hk")) {
					message = "对不起，型号确认系统暂不支持此产品，请参考样本。";
				} else if (baseLang.equals("zh-tw")) {
					message = "由於產品型號確認系統無法對應，煩請參照目錄。";
				}
	
				ret+= "<p>\r\n" +
						"<br/>\r\n" +
						"<br/>\r\n" +
						message+"\r\n" +
						"<br/>\r\n" +
						"<br/>\r\n" +
						"<br/>\r\n" +
						"<br/>\r\n" +
						"</p>";

				String backUrl = request.getHeader("REFERER");
				String mes = messagesource.getMessage("button.back.to.list", null,  baseLocale);
				if (backUrl != null && backUrl.isEmpty() == false && backUrl.indexOf("/webcatalog/") > -1 && backUrl.indexOf("/s3s/") == -1 ) {
					tmp = "<div class=\"backToList\"><a href=\"$$$url$$$\" class=\"backToList\">$$$message$$$</a></div>\r\n".replace("$$$url$$$", backUrl).replace("$$$message$$$", mes);
				} else {
					tmp = "<div class=\"catpan\"><a href=\"/webcatalog/ja-jp/series/{sid}\" class=\"bt_3s\">製品情報に戻る</a></div>\r\n" +
							"        <div class=\"clear\"></div>";
					tmp = tmp.replace("{sid}", sid);
					if (lang.indexOf("en-") > -1) {
						tmp = tmp.replace("/ja-jp/", "/"+lang+"/");
						tmp = tmp.replace("製品情報に戻る", "Return to Product Information");
					} else if (lang.equals("zh-cn") || lang.equals("zh-hk")) {
						tmp = tmp.replace("/ja-jp/", "/"+lang+"/");
						tmp = tmp.replace("製品情報に戻る", "返回至产品信息");
					} else if (lang.equals("zh-tw")) {
						tmp = tmp.replace("/ja-jp/", "/"+lang+"/");
						tmp = tmp.replace("製品情報に戻る", "返回產品情報");
					}
				}
				ret+=tmp;
	
				ret+= "</div>\r\n"+footer;
			}
		}
		if (langObj.isVersion()) {
			// 変換処理
			ret = html.changeLang(ret, baseLang, lang);
		}
		return ret;
	}

	/*
	 * for CADENASLINK
	 * WEB-INF/files/docs/20210329_SMC様_PcomEmbedded_呼出しURL書式.pdf参照
	 */

	private void forCadenasLink(HashMap<String, String> cadenas, S3SDetailResult result, String ca_lang)
			throws UnsupportedEncodingException {

		String ca_info = "";//project path
		String ca_varset = "";//cadparams
		String ca_language = ca_lang;//(or english,chinese)
		String ca_name = result.getProductName();//print name
		//String ca_name=URLEncoder.encode(result.getProductName(),"utf-8");//print name

		ca_info = result.getCadParameter();
		ca_info = ca_info.replaceFirst("^.*(smc_jp\\/.*\\.prj).*$", "$1");

		ca_varset = result.getCadParameter();
		ca_varset = ca_varset.replaceFirst("^.*\\.prj},(.*)$", "$1");
		ca_varset = ca_varset.replaceFirst(",$", "");
		ca_varset = URLEncoder.encode(ca_varset, "utf-8");

		cadenas.put("ca_info", ca_info);
		cadenas.put("ca_varset", ca_varset);
		cadenas.put("ca_language", ca_language);
		cadenas.put("ca_name", ca_name);

	}

	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}
}
