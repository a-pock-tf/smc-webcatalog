package com.smc.webcatalog.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smc.omlist.service.OmlistServiceImpl;
import com.smc.psitem.model.PsItem;
import com.smc.psitem.service.PsItemService;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.Lang;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.Series;
import com.smc.webcatalog.model.SeriesHtml;
import com.smc.webcatalog.model.Template;
import com.smc.webcatalog.model.api.CadenasResult;
import com.smc.webcatalog.model.api.SearchResult;
import com.smc.webcatalog.service.CategoryService;
import com.smc.webcatalog.service.LangService;
import com.smc.webcatalog.service.SeriesService;
import com.smc.webcatalog.service.TemplateService;
import com.smc.webcatalog.util.AESUtils;
import com.smc.webcatalog.util.JpServiceResult;
import com.smc.webcatalog.util.JpServiceUtil;
import com.smc.webcatalog.util.LibHtml;
import com.smc.webcatalog.util.LibOkHttpClient;
import com.smc.webcatalog.util.S3SPartialMatchResult;
import com.smc.webcatalog.util.S3SPartialMatchResultData;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 検索用API
 * @author tfujishima
 *
 * search() 
 * 
 */
@RestController
@ResponseBody
@RequestMapping("/api/v1")
@Slf4j
@CrossOrigin(origins= {"http://192.168.0.36","http://192.168.0.34","http://153.120.135.17","https://153.120.135.17",
		"http://localhost:8081", "http://localhost:8080", "http://localhost:5173", "http://127.0.0.1:5173", 
		"http://dev1.smcworld.com","http://ap1.smcworld.com","http://ap2.smcworld.com",
		"https://dev1admin.smcworld.com","https://ap1admin.smcworld.com",
		"http://www.smc3s.com", "https://www.smc3s.com", "http://133.242.52.163", "http://153.120.139.192",
		"http://3sapi.smcworld.com","https://3sapi.smcworld.com", "https://test.smcworld.com", "https://cdn.smcworld.com",
		"http://local.smcworld.com","https://local.smcworld.com",
		"https://www.smcworld.com", "https://www.smc.com.cn"},allowCredentials="true")
public class ApiRestController {

	@Autowired
	LangService langService;

	@Autowired
	CategoryService service;

	@Autowired
	SeriesService seriesService;

	@Autowired
	PsItemService psItemService;

	@Autowired
	OmlistServiceImpl omlistService;

	@Autowired
	TemplateService templateService;
	
    @Autowired
    SeriesFaqRepository faqRepo;

	@Autowired
    MessageSource messagesource;

	@Autowired
	LibHtml libHtml;
	
	@Autowired
	JpServiceUtil util;

	@Autowired
    HttpServletRequest req;

	@ApiOperation(value="PsItemを検索。<table class=resulttbl>のHTMLを返す。日本語、英語は前方一致選択可、cd=1。デフォルトは英語は前方一致、その他の国は部分一致、cd=2", 
			notes="PsItemはTEST無し。異常時は message=ng または hitCount=-1。\r\n"
					+ "末尾が - の場合は削除して検索。2024/5/15\r\n"
					+ "日本語はデフォルト前方一致から部分一致に変更。英語は前方一致のまま2024/12/10\r\n")
	@RequestMapping(path = "/search", method = { RequestMethod.GET, RequestMethod.POST })
	public SearchResult search(@RequestParam(name = "lang", required = true) String lang,
			@RequestParam(name = "kw", required = true) String[] kwArr,
			@RequestParam(name = "cd", required = false, defaultValue = "2") String cd,
			@RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
			HttpServletRequest request
			) {
		
		SearchResult res = new SearchResult();
		String html = "";
		StringBuilder sbHtml = new StringBuilder();
		String baseLang = "";
		
		String url = request.getRequestURL().toString();
		boolean isTestSite = LibHtml.isTestSite(url);

		try {
			if (lang != null && lang.isEmpty() == false) {
				if (lang.indexOf("ja") > -1 || lang.equals("jp") ) {
					baseLang = "ja-jp";
					if (cd.equals("1") == false && cd.equals("2") == false) {
						 cd = "2"; // 1,2以外の場合、部分一致
					}
				} else if (lang.indexOf("en") > -1) {
					baseLang="en-jp";
					if (cd.equals("1") == false && cd.equals("2") == false) {
						cd = "1"; // 1,2以外の場合、英語なら前方一致
					}
				} else if (lang.equals("zh-tw")) {
					baseLang="zh-tw";
					cd = "2";
				} else if (lang.indexOf("zh") > -1) {
					baseLang="zh-cn";
					cd = "2";
				}
			}
			Template t = templateService.getTemplateFromBean(baseLang, ModelState.PROD);
			boolean is2026 = t.is2026();

			// 以下、ProdRestControllerにも同様の処理があるが、そちらは絞り込みがあるので、同じ処理には出来ない。
			int cnt = 0;
			int max = 10; // 10件まで
			if (limit > 0) max = limit;
			if (max > 100) max = 100;
			// 末尾が - の場合は削除 2024/5/15
			if (kwArr != null) {
				for(int i = 0; i < kwArr.length; i++) {
					if (kwArr[i].lastIndexOf("-") == kwArr[i].length()-1) {
						kwArr[i] = kwArr[i].substring(0, kwArr[i].length()-1); 
					}
				}
			}
			List<PsItem> list = psItemService.searchKeywordAndOr(kwArr, baseLang, cd, max, true);
			if (list != null && list.size() < 10) max = list.size();
			// output HTML
			if (list != null && list.size() > 0) {
				String th[] = AppConfig.PsItemSearchResultTableTh[0]; 
				if (lang.indexOf("en-") > -1) {
					th = AppConfig.PsItemSearchResultTableTh[1];
				} else if (lang.indexOf("zh-") > -1) {
					th = AppConfig.PsItemSearchResultTableTh[2];
				}

				if (isTestSite || is2026) {
					sbHtml.append("<div class=\"w-full overflow-auto mb48\">\r\n<table class=\"table\">\r\n<thead>\r\n<tr>\r\n")
					.append("<th class=\"th w284\" colspan=\"1\">").append(th[0]).append("</th>")
					.append("<th class=\"th w234\" colspan=\"1\">").append(th[1]).append("</th>")
					.append("<th class=\"th w234\" colspan=\"1\">").append(th[2]).append("</th>")
					.append("<th class=\"th w234\" colspan=\"1\">").append(th[3]).append("</th>")
					.append("<th class=\"th w120\" colspan=\"1\">&nbsp;</th>")
					.append("</tr><thead><tbody><tr>");
				} else {
					// 旧デザイン
					sbHtml.append("<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n<tbody><tr>")
					.append("<th style=\"width:280px;\"" + th[0] +"</th>")
					.append("<th>").append(th[1]).append("</th>")
					.append("<th>").append(th[2]).append("</th>")
					.append("<th>").append(th[3]).append("</th>")
					.append("<th style=\"width:80px;\">&nbsp;</th>")
					.append("</tr><tr>");
				}

				String detail = "詳細へ";
				if (lang.indexOf("en-") > -1) {
					detail = "Detail";
				} else if (lang.indexOf("zh-") > -1) {
					detail = "详情";
				}
				for(PsItem s : list) {
					String c1c2 = s.getC1()+"/"+s.getC2();
					
					if (isTestSite || is2026) {
						sbHtml.append("<td class=\"td text-sm\" colspan=\"1\">" + c1c2 +"</td>")
						.append("<td class=\"td text-sm\" colspan=\"1\">" + s.getName() +"</td>")
						.append("<td class=\"td text-sm\" colspan=\"1\">" + s.getSeries() +"</td>")
						.append("<td class=\"td text-sm\" colspan=\"1\">" + s.getItem() +"</td>");
						// queryが複数あれば複数対応
						String seriesUrl = s.getQuery();
						String[] arr2 = null;
						if (seriesUrl != null) arr2 = seriesUrl.split("id=");
						if (arr2 != null) {
							sbHtml.append("<td class=\"td text-center\" colspan=\"1\">\r\n")
									.append( "    <a class=\"button secondary solid medium\" href=\"").append(AppConfig.ProdRelativeUrl).append(baseLang).append("/seriesList/?").append(seriesUrl).append("\">").append(detail).append("</a>\r\n")
									.append( "</td>\r\n");
						} else {
							sbHtml.append("<td>&nbsp;</td>");
						}
					} else {
						sbHtml.append("<td>").append(c1c2).append("</td>")
						.append("<td>").append(s.getName()).append("</td>")
						.append("<td>").append(s.getSeries()).append("</td>")
						.append("<td>").append(s.getItem()).append("</td>");
						// queryが複数あれば複数対応
						String seriesUrl = s.getQuery();
						String[] arr2 = null;
						if (seriesUrl != null) arr2 = seriesUrl.split("id=");
						if (arr2 != null) {
							sbHtml.append("<td><a href=\"").append(AppConfig.ProdRelativeUrl).append(baseLang).append("/seriesList/?").append(seriesUrl).append("\">").append(detail).append("</a></td>");
						} else {
							sbHtml.append("<td>&nbsp;</td>");
						}
					}
					cnt++;
					if (cnt < max) sbHtml.append("</tr>\r\n<tr>");
				}
				
				sbHtml.append("</tr>\r\n</tbody></table>");
				if (isTestSite || is2026) {
					sbHtml.append("</div>\r\n");
				}
				html = sbHtml.toString();
				res.setMessage("ok");
				res.setHitCount(psItemService.searchKeywordAndOrCount(kwArr, baseLang, cd, true));
				res.setHtml(html);
			} else {
				res.setMessage("ok");
				res.setHitCount(0);
			}
			
		} catch (Exception e) {
			log.error("search() ERROR! message="+e.getMessage());
			res.setMessage("exception. message="+e.getMessage());
			res.setHitCount(0);
		}

		return res;
	}

	// 全体検索用、3Sの部分一致検索
	@ApiOperation(value="3S部分一致検索。<table class=resulttbl>のHTMLを返す。", 
			notes="前方一致はcd=1、部分一致はcd=2。異常時は message=ng または hitCount=-1。") 
	@RequestMapping(path = "/search3S", method = { RequestMethod.GET, RequestMethod.POST })
	public SearchResult getGlobalSearch3S(@RequestParam(name = "lang", required = true ) String lang,
			@RequestParam(name = "kw", required = false) String[] kwArr,
			@RequestParam(name = "limit", required = false, defaultValue = "5") int limit,
			@RequestParam(name = "cd", required = false, defaultValue = "1") String cd,
			HttpServletRequest request) {
		
		SearchResult ret = new SearchResult();
		String html = "";
		ErrorObject err = new ErrorObject();

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
		
		int max = 5; // 10件まで
		if (limit > 0) max = limit;
		if (max > 100) max = 100;
		
		// 3S
		S3SPartialMatchResult res = null;
		
		// スペースがあれば区切って、関連語を取得
		// 2023/10/20 当面、日本語のみ
		try {
			List<String> kwList = null;
			if (kwArr != null && kwArr.length > 0) {
				kwList = new LinkedList<>();
				kwList.addAll(Arrays.asList(kwArr));
			}
			
			if (kwList != null) {
				String searchType = "in";
				if (cd == null || cd.equals("1")) {
					searchType = "fore";
				}
				res = util.search(request.getServletContext(), kwList, baseLang, null, max, searchType);
			}
			if (res != null && res.getHitCount().isEmpty() == false && res.getHitCount().equals("0") == false) {
				
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
				String u = request.getRequestURL().toString();
				boolean isTestSite = LibHtml.isTestSite(u);
				Template t = templateService.getTemplateFromBean(baseLang, ModelState.PROD);
				boolean is2026 = t.is2026();
				if (isTestSite || is2026) {
					if (lang.equals("ja-jp")) strDetail += "へ";
					html += "<div class=\"w-full overflow-auto mb48\">\r\n"
							 + "        <table class=\"table\">\r\n"
							 + "           <thead>\r\n"
							 + "             <tr>\r\n";
						html+="<th class=\"th\" colspan=\"2\">" + strType +"</th>";
						html+="<th class=\"th\" colspan=\"1\">" + strNumber +"</th>";
						html+="</tr><thead>"
								+ "<tbody><tr>";
				} else {
					html = "<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
						"		<tr>\r\n" +
						"			<th colspan=\"2\" class=\"\">"+strType+"</th>\r\n" +
						"			<th class=\"last\">"+strNumber+"</th>\r\n" +
						"		</tr>";
				}
				for(S3SPartialMatchResultData d : res.getSearchData()) {
					String url = "/webcatalog/s3s/"+lang + "/frame/"+d.getTypeID()+"/";
					if (isTestSite || is2026) {
						html += "<tr>\r\n" ;
						if (d.getPicFile() != null && d.getPicFile().isEmpty() == false) {
							html += "			<td class=\"td text-center w85\" ><img src=\""+d.getPicFile().replace("{size}", "S")+"\"/></td>\r\n" ;
						} else {
							html += "			<td class=\"td text-center w85\" ></td>\r\n" ;
						}
						
						html += "			<td class=\"td text-sm text-left\">"+d.getDescription()+"</td>\r\n" +
							"			<td class=\"td w120\">\r\n" +
							"					<a class=\"button secondary solid medium\" href=\""+url+"\" target=\"_blank\">"+strDetail+"</a>\r\n" +
							"			</td>\r\n" +
							"		</tr>";
					} else {
						html += "<tr>\r\n" ;
						if (d.getPicFile() != null && d.getPicFile().isEmpty() == false) {
							html += "			<td class=\"\" style=\"padding: 5px;border-right: none;width:55px;height:60px;\"><img src=\""+d.getPicFile().replace("{size}", "S")+"\"/></td>\r\n" ;
						} else {
							html += "			<td class=\"\" style=\"padding: 5px;border-right: none;width:55px;height:60px;\"></td>\r\n" ;
						}
						html += "			<td class=\"\" style=\"text-align: left;padding: 5px;\">"+d.getDescription()+"</td>\r\n" +
							"			<td class=\"last\">\r\n" +
							"					<a href=\""+url+"\" target=\"_blank\">"+strDetail+"</a>\r\n" +
							"			</td>\r\n" +
							"		</tr>";
					}
				}
				html+="</table>\r\n";
				if (isTestSite || is2026) html+="</div>\r\n";
				ret.setMessage("ok");
				ret.setHitCount(Long.parseLong(res.getHitCount()));
				ret.setHtml(html);
			} else {
				// noHit
				ret.setMessage("ok");
				ret.setHitCount(0);
			}
		} catch (Exception e) {
			log.error("search() ERROR! message="+e.getMessage());
			ret.setMessage("exception. message="+e.getMessage());
			ret.setHitCount(0);
		}

		return ret;
	}

	// pathを返す。
	// "/webcatalog/s3s/"+lang+"/detail/?partNumber="+kw;
	@RequestMapping(path = "/check3S", method = { RequestMethod.GET, RequestMethod.POST })
	@ApiOperation("フル品番の有無チェック。Hit時はpathを返す。\"/webcatalog/s3s/\"+lang+\"/detail/?partNumber=\"+kw")
	public String getSearch3SandPsItem(@RequestParam(name = "lang", required = true) String lang,
			@RequestParam(name = "kw", required = true) String[] kwArr,
			HttpServletRequest request) {

		String ret = null;
		ErrorObject err = new ErrorObject();

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
		JpServiceResult res = null;
		if (kwArr == null || kwArr.length == 0) {
			// 何もしない。
		} else {
			String kw = kwArr[0];
			if ( kw != null && kw.isEmpty() == false && kw.getBytes().length != kw.length()) {
				kw = Normalizer.normalize(kw, Normalizer.Form.NFKC); // 全角英数を半角に
				if (kw.indexOf("ー") > -1) kw = kw.replace("ー", "-");
				if (kw.indexOf("‐") > -1) kw = kw.replace("‐", "-");
				if (kw.indexOf("―") > -1) kw = kw.replace("―", "-");
			}
			if (kw != null && kw.matches("^[A-z0-9-_/+#]{1,30}$")) { // 全部半角の場合のみ3S検索
				res = util.get(request.getServletContext(), kw, baseLang);
			}
			if (res != null && res.getCode().isEmpty() == false && (res.getCode().equals("21") || res.getCode().equals("22")) ){
				// 3Sの結果を表示
				if (kw.indexOf('#') > -1) kw = kw.replace("#", "%23");
				if (lang.indexOf("zh") > -1) {
					ret = "/webcatalog/s3s/"+lang+"/detail/?partNumber="+kw;
				} else if (lang.indexOf("en") > -1) {
					ret = "/webcatalog/s3s/"+lang+"/detail/?partNumber="+kw;
				} else {
					ret = "/webcatalog/s3s/"+lang+"/detail/?partNumber="+kw;
				}
			}
		}
		return ret;
	}
	@ApiOperation(value="ガイドを検索。デフォルト5件。<div class=\"p_block\"><div class=\"result\">のHTMLを返す。", notes="異常時は message=ng または hitCount=-1")
	@RequestMapping(path = "/searchSite", method = { RequestMethod.GET, RequestMethod.POST })
	public SearchResult search(@RequestParam(name = "lang", required = true) String lang,
			@RequestParam(name = "kw", required = true) String[] kwArr,
			@RequestParam(name = "limit", required = false, defaultValue = "5") int limit,
			@RequestParam(name = "test", required = false, defaultValue = "0") int test,
			HttpServletRequest request) {
		
		SearchResult res = new SearchResult();
		boolean isProd = true; 
		StringBuilder html = new StringBuilder();
		String baseLang = "";
		ErrorObject err = new ErrorObject();

		try {
			if (test == 1) isProd = false;
			
			if (lang != null && lang.isEmpty() == false) {
				if (lang.indexOf("ja") > -1 || lang.equals("jp") ) {
					baseLang = "ja-jp";
				} else if (lang.indexOf("en") > -1) {
					baseLang="en-jp";
				} else if (lang.equals("zh-tw")) {
					baseLang="zh-tw";
				} else if (lang.indexOf("zh") > -1) {
					baseLang="zh-cn";
				}
			}
			String u = request.getRequestURL().toString();
			boolean isTestSite = LibHtml.isTestSite(u);
			if (isTestSite) {
				isProd = false;
			}

			SeriesHtml sHtml = null;
			if (isTestSite) sHtml = new SeriesHtml(LibHtml.getLocale(baseLang), messagesource, omlistService, faqRepo);

			// 以下、ProdRestControllerにも同様の処理があるが、そちらは絞り込みがあるので、同じ処理には出来ない。
			int cnt = 0;
			int max = 5; // デフォルト5件まで
			if (limit != max) max = limit;
			if (max > 100) max = 100; // 100件以上は許可しない。
			List<Series> list = seriesService.searchKeywordAndOr(kwArr, baseLang, max, isProd, true);
			
			if (list != null && list.size() < 10) max = list.size();
			// output HTML
			if (list != null && list.size() > 0) {
				html.append("<div class=\"p_block\">");
				for(Series s : list) {
					if (isTestSite) {
						s.setLink(seriesService.getLink(s.getId(), err));
						html.append( sHtml.getGuide2026(s, null, null, request.getRequestURI(), s.getLang(), false, true));
						if ((max-1) > cnt) html.append("<div class=\"w-full h1 bg-base-stroke-default my36\"></div>");
					} else {
						html.append( libHtml.getFileFromHtml(lang + "/series/" + s.getModelNumber() + "/s.html"));
					}
					cnt++;
				}
				html.append("</div>");
				res.setHtml(html.toString());
				res.setHitCount(seriesService.searchKeywordAndOrCount(kwArr, baseLang, isProd, true));
				res.setMessage("ok");
			} else {
				res.setMessage("ok");
				res.setHitCount(0);
			}
		} catch (Exception e) {
			log.error("search() ERROR! message="+e.getMessage());
		}
		return res;
	}
	/**
	 * Secret AES and HMAC Key, will be changed for every customer and have to be kept secret.
	 * These two keys should be not stored or rendered on client side 
	 */
//	private static final String aesKey = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"; // 256 bit AES key
//	private static final String aesKey = "DtTWvmFRaYT4r5CVZrR25aA7W4Cc6bGE"; // 256 bit AES key Sample
	private static final String aesKey = "Q0450Mg43moYKQbBvz95551a0J802h75"; // 256 bit AES key Sample
	
	
	// customer specific portalname
//	private static final String portalName = "smc-jp-embedded"; // old
//	private static final String portalName = "smc-jp-remote-embedded.qa"; // TEST site
	private static final String portalName = "smc-jp-remote-embedded"; // PROD site
	
	@RequestMapping(path = "/cadenasAesUrl", method = { RequestMethod.GET, RequestMethod.POST })
	@CrossOrigin(origins= {"http://192.168.0.36","http://192.168.0.34","http://153.120.135.17","https://153.120.135.17",
			"http://localhost", "http://localhost:8080","http://localhost:8081",
			"http://dev1.smcworld.com","http://ap1.smcworld.com","http://ap2.smcworld.com",
			"https://dev1admin.smcworld.com","https://ap1admin.smcworld.com",
			"http://www.smc3s.com", "https://www.smc3s.com", "http://133.242.52.163", "http://153.120.139.192",
			"https://3sapi.smcworld.com", "https://test.smcworld.com","https://cdn.smcworld.com", 
			"https://www.smcworld.com", "https://www.smc.com.cn"},allowCredentials="true")
	// act: create update delete login createAndLogin
	public CadenasResult getCadenasAes(@RequestParam(name = "us", required = true) String us,
			@RequestParam(name = "email", required = true) String email,
			@RequestParam(name = "lang", required = false) String lang
			) {
		CadenasResult ret = new CadenasResult();
		// generation time 
		long genTimeMin = System.currentTimeMillis() / 1000 / 60;
		
		String servletUrl = "https://" + portalName + ".partcommunity.com/PARTcommunityAPI/UserRemoteServlet?portal=" + portalName;
	
		// userdata string for create / update user
		String plainTextCreate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
				+ "<DataToken>"
				// these fields are mandatory and must contain meaningful data
				+ "<email><![CDATA[" + email + "]]></email>"
				+ "<nopassword><![CDATA[1]]></nopassword>"
				// usage of password is not recommended, it may lead to inconsistent data
				// + "<password><![CDATA[userpwd]]></password>"
				+ "<country><![CDATA[JP]]></country>"
				+ "<language><![CDATA[ja]]></language>"
				+ "<activate><![CDATA[1]]></activate>"
				+ "<gentimemin><![CDATA[" + genTimeMin + "]]></gentimemin>"
	
				// these fields are required, but may be filled with a white space or dummy data
				+ "<firstname><![CDATA[CAD3D]]></firstname>"
				+ "<lastname><![CDATA[IFRAME]]></lastname>"
				
				+ "<street><![CDATA["+us+"]]></street>"
				+ "<newsdesired><![CDATA[0]]></newsdesired>"
				
				// these fields are optional and may be skipped
/*				+ "<title><![CDATA[Mr]]></title>"
				+ "<company><![CDATA[Cadenas GmbH]]></company>"
				+ "<street><![CDATA[Berliner Allee 28b]]></street>"
				+ "<zip><![CDATA[86153]]></zip>"
				+ "<city><![CDATA[Augsburg]]></city>"
				+ "<state><![CDATA[Bavaria]]></state>"
				+ "<phone><![CDATA[000]]></phone>"
				+ "<fax><![CDATA[000]]></fax>"*/
				+ "</DataToken>";
		// login token
		try {
			String rlogintoken = email + "||0||" + genTimeMin;
			byte[] cipherText = AESUtils.encrypt(plainTextCreate, aesKey);
//			String encCreate = Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText).replaceAll("[\\s_]", "");
//			String encCreate = Base64.getEncoder().encodeToString(cipherText).replaceAll("[\\s_]", "");
//			String encCreate = Base64.getUrlEncoder().encodeToString(cipherText);
			String encCreate = Base64.getEncoder().encodeToString(cipherText);
			encCreate = URLEncoder.encode(encCreate, StandardCharsets.UTF_8.name());
			
			cipherText = AESUtils.encrypt(rlogintoken, aesKey);
			//String encLoginToken = URLEncoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText), "UTF-8");
			String encLoginToken = Base64.getEncoder().encodeToString(cipherText);
			encLoginToken = URLEncoder.encode(encLoginToken, StandardCharsets.UTF_8.name());
			
			// 結果をセット
			String createUrl = servletUrl + "&topic=create" + "&data=" + encCreate;
			ret.setCreate( createUrl );
			ret.setUpdate( servletUrl + "&topic=update" + "&data=" + encCreate);
			ret.setLogin("https://" + portalName + ".partcommunity.com/3d-cad-models/?rlogintoken=" + encLoginToken);
			
	        try{
		        String response = LibOkHttpClient.getHtml(createUrl);

		        if(response != null && response.isEmpty() == false)
		        {
		        	ret.setEmail(email);
		        	ret.setError("");
		        	ret.setResult( response );
		        } else {
		        	ret.setResult( response );
			    	log.error("Error! login error. response="+response);
		        	ret.setError("Error! login error. response="+response);
		        }

		    }catch(Exception ex){
		    	log.error("getCadenasAes()"+ex.toString());
	        	ret.setError("Error! login exception."+ex.toString());
		    }finally{
		    }
			// createAndLogin
//			ret = "https://" + portalName + ".partcommunity.com/3d-cad-models?rlogintoken=" + encLoginToken + "&data=" + encCreate;
		} catch (Exception e) {
			log.error("getCadenasAes() exception="+e.getMessage());
        	ret.setError("Error! login data exception.");
		}
		return ret;
	}
	public static final String decode(final String encodedText, final String key) throws IllegalArgumentException, GeneralSecurityException, UnsupportedEncodingException {
		String text = URLDecoder.decode(encodedText, "UTF-8");
		byte[] result = AESUtils.decrypt(Base64.getUrlDecoder().decode(text), key);
		return new String(result, StandardCharsets.UTF_8);
	}

}
