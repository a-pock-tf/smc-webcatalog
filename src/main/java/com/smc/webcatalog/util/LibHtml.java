
package com.smc.webcatalog.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.service.DiscontinuedSeriesServiceImpl;
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
import com.smc.webcatalog.service.NarrowDownServiceImpl;
import com.smc.webcatalog.service.SeriesService;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class LibHtml {

    @Autowired
	Environment env;
    
    @Autowired
	LangService langService;

    @Autowired
    DiscontinuedSeriesServiceImpl disconSeriesService;

    @Autowired
    OmlistServiceImpl omlistService;
    
    @Autowired
    NarrowDownServiceImpl narrowDownService;
    
    @Autowired
    SeriesFaqRepository faqRepo;

    MessageSource messagesource;

    boolean isLocal = false;

	private Locale _locale = Locale.JAPANESE;
	private HttpClient client = null;
	private String htmlPath = "";

	public LibHtml() {
	}
	public LibHtml(Locale loc, MessageSource msg) {
		_locale = loc;
		messagesource = msg;
		Init();
	}

	public void Init(Locale loc, MessageSource msg) {
		_locale = loc;
		messagesource = msg;
		Init();
	}

	private void Init() {
		client = new HttpClient();
		try {
			htmlPath = env.getProperty("smc.webcatalog.static.page.path");
		} catch (Exception e) {
			e.printStackTrace();
	    }
	}

	public void InitOffLine(Environment ev, String lang, MessageSource msg, SeriesFaqRepository repo) {
		client = new HttpClient();
		isLocal = true;
		messagesource = msg;
		env = ev;
		if (faqRepo == null) faqRepo = repo;
		try {
			htmlPath = env.getProperty("smc.webcatalog.offline.page.path");
			if (lang.equals("ja-jp")) {
				htmlPath = htmlPath.replace("/offline/", "/offline_jp/");
			} else {
				htmlPath = htmlPath.replace("/offline/", "/offline_en/");
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	
	// テストか本番かのチェック
	// 2026のリニューアルでテンプレートのチェックが必要になった。
	// テスト用。旧デザイン表示のため。
	public boolean isTestSite(String url) {
		return false;
	}
	// 本番用。
//	public boolean isTestSite(String url) {
//		boolean ret = false;
//		if (url != null && url.isEmpty() == false) {
//			ret = (url.indexOf("test.smcworld.com") > -1
//				|| url.indexOf("ap1admin.smcworld.com") > -1
//				|| url.indexOf("dev1admin.smcworld.com") > -1
//				|| url.indexOf("local.smcworld.com") > -1
//				|| url.indexOf("127.0.0.1") > -1
//				|| url.indexOf("localhost") > -1
//			);
//		}
//		if (ret) log.info("LibHtml.isTestSite() true. url="+url);
//		return ret;
//	}

	// 言語のテンプレート分割
	// 2026のリニューアルでは<main>タグで判定
	public List<String> getDivHtml(String url, String[] div) {
		List<String> ret = null;
		String html = LibHttpClient.getHttpsHtml(url);
		if (html != null) {
			if (html.indexOf("<main") > -1) {
				ret = divHtml2026(html);
			} else {
				ret = divHtml(html, div);
			}
		}
		return ret;
	}
	
	public List<String> divHtml2026(String html) {
		List<String> ret = new LinkedList<String>();
		String[] tmp = html.split("<main");
		if (tmp.length == 2) {
			String[] tmp2 = tmp[1].split("</main>");
			if (tmp2.length == 2) {
				ret.add(tmp[0]);
				ret.add("<main" + tmp2[0] + "</main>");
				ret.add(tmp2[1]);
			} else {
				log.error("Template lang </main> nothing or dupulicate. length="+tmp2.length);
			}
		} else {
			log.error("Template lang <main> nothing or dupulicate. length="+tmp.length);
		}
		
		return ret;
	}
	
	// 同じdivで囲われた内部を抽出
	public String extractHtml(String html, String div) {
		String ret = "";
		String[] arr = html.split(div);
		if (arr.length == 3) {
			ret = arr[1];
		}
		return ret;
	}


	/// divが3つ以上は難しい。２つまで。
	public List<String> divHtml(String html, String[] div) {
		List<String> ret = new LinkedList<String>();
		String after = html;
		for(String d : div) {
			String[] tmp = after.split(d);
			if (tmp.length > 1) {
				for (int i = 0; i < tmp.length-1; i++) {
					ret.add(tmp[i]);
				}
				after = tmp[tmp.length-1];
			}
			else
			{
				log.debug("not find. div=" + d);
				break;
			}
		}
		if (after != null && after.length() > 1) ret.add(after);
		return ret;
	}

	public List<String> divHtmlLimit(String html, String[] div, int limit) {
		List<String> ret = new LinkedList<String>();
		String after = html;
		for(String d : div) {
			String[] tmp = after.split(d, limit);
			if (tmp.length > 1) {
				for (int i = 0; i < tmp.length-1; i++) {
					ret.add(tmp[i]);
				}
				after = tmp[tmp.length-1];
			}
			else
			{
				log.debug("not find. div=" + d);
				break;
			}
		}
		if (after != null && after.length() > 1) ret.add(after);
		return ret;
	}


	/**
	 *
	 * @param t
	 * @param tc
	 * @param c
	 * @param c2 null OK
	 * @param list null OK
	 * @param seriesService null OK
	 * @return
	 */
	public boolean outputTemplateCategoryToHtml(Template t, TemplateCategory tc, Category c, Category c2, List<Series> list, CategoryService categoryService, SeriesService seriesService)
	{
		boolean ret = false;

		String write = t.getHeader();
		String temp = tc.getTemplate();

		String catpan = "";
		String formbox = "";
		String h1box = "";
		String content = "";
		String contentPicture = ""; // 写真一覧 2024/10/8 Add
		String contentCompare = ""; // 仕様比較 2024/10/8 Add
		Map<String, String> sList = null; // 検索結果（カテゴリ有、特長へのリンク、マイリスト有り。） カテゴリ一覧（カテゴリ無し、マイリスト有り）
		Map<String, String> gList = null; // ガイド用。特長なし、マイリスト無し。ページ作成
		Map<String, String> seriesList = null; // 特長あり。戻るボタンはreferrer
		try {
			if (c2 != null ) {
				sList = new HashMap<String, String>();
				gList = new HashMap<String, String>();
				seriesList = new HashMap<String, String>();
				if (c2.getSlug() != null && c2.getSlug().isEmpty() == false) {
					// 2024/10/8 写真一覧追加。index.htmlは今までのガイド一覧。picture.htmlは写真一覧。compare.htmlは仕様比較
					catpan +="<a href=\""+AppConfig.ProdRelativeUrl+c.getLang()+"/"+c.getSlug()+"\">"+c.getName()+"</a>\r\n";
					catpan +="&nbsp;»&nbsp;";
					String url = AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug();
//					catpan +="<a href=\""+ url +"\">"+c2.getName()+"</a>";
					catpan +=c2.getName();
					formbox = tc.getH1box().replace("$$$title$$$", c2.getName()); // ↓小カテゴリは逆
					h1box = tc.getFormbox(); // ↑小カテゴリは逆

					Locale locale = getLocale(c.getLang());
					SeriesHtml sHtml = new SeriesHtml(locale, messagesource, omlistService, faqRepo);
					ErrorObject err = new ErrorObject();
					for(Series s : list) {
						s.setLink(seriesService.getLink(s.getId(), err));
						String str = sHtml.get(s, c, c2, url, c.getLang(), false, false);
						sList.put(s.getModelNumber(), str);
						content += str;
						String strG =  sHtml.get(s, c, c2, url, c.getLang(), false, true);
						gList.put(s.getModelNumber(), strG);
						String strA = sHtml.get(s, c, c2, url, c.getLang(), true, false);
						seriesList.put(s.getModelNumber(), strA);
					}
					content = "<div class=\"p_block\">\r\n" + content + "</div><!-- p_block -->\r\n";
					contentPicture = "<div class=\"p_block\">\r\n" + sHtml.getPictureList(c, c2, list) + "</div><!-- p_block -->\r\n";
//					List<NarrowDownCompare> compareList = narrowDownService.getCategoryCompare(c2.getId(), true, err);
					String baseLang = c.getLang();
					Lang langObj = langService.getLang(baseLang, err);
					if (langObj.isVersion()) {
						baseLang = langObj.getBaseLang();
					}
					if (c2.isCompare()) {
						HashMap<String, List<NarrowDownValue>> map = new HashMap<>();
						for (Series s : list) {
							List<NarrowDownValue> valList = narrowDownService.getCategorySeriesValue(c2.getId(), s.getId(), true, err);
							map.put(s.getId(), valList);
						}
						List<NarrowDownColumn> colList = narrowDownService.getCategoryColumn(c2.getId(), true, err);
						contentCompare = "<div class=\"p_block\">\r\n" + sHtml.getCompareHtml(c.getLang(), baseLang, c, c2, colList, list, map, null) + "</div><!-- p_block -->\r\n";
					}
				}
			} else {
				catpan = c.getName();
				formbox =  tc.getFormbox();
				h1box =tc.getH1box().replace("$$$title$$$", c.getName());
				content = tc.getContent();
				if (list != null && list.size() > 0 ) { // 大カテゴリにシリーズ一覧がある場合
					sList = new HashMap<String, String>();
					gList = new HashMap<String, String>();
					seriesList = new HashMap<String, String>();
					Locale locale = getLocale(c.getLang());
					SeriesHtml sHtml = new SeriesHtml(locale, messagesource, omlistService, faqRepo);
					ErrorObject err = new ErrorObject();
					String url = AppConfig.ProdRelativeUrl+c.getLang()+"/"+c.getSlug();
					for(Series s: list) {
						s.setLink(seriesService.getLink(s.getId(), err));
						String str = sHtml.get(s, c, c2, url, c.getLang(), false, false); // 特長なし
						sList.put(s.getModelNumber(), str);
						content += str;
						String strG =  sHtml.get(s, c, c2, url, c.getLang(), false, true);
						gList.put(s.getModelNumber(), strG);
						String strA = sHtml.get(s, c, c2, url, c.getLang(), true, false); // 特長あり
						seriesList.put(s.getModelNumber(), strA);
					}
					content = "<div class=\"p_block\">\r\n" + content + "</div><!-- p_block -->\r\n";
				}
			}

			temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", catpan));
			String sidebar =  tc.getSidebar();
			ErrorObject obj = new ErrorObject();
			List<Category> cList = categoryService.listAll(c.getLang(), ModelState.PROD, c.getType(), obj);
			// 2024/9/20 カテゴリの下にシリーズリンクを作成するため、シリーズの情報付加。
			List<Category> setCategoryList = new LinkedList<>();
			for(Category cate :  cList) {
				Category setC = categoryService.getWithSeries(cate.getId(), null, obj);
				if (setC != null) setCategoryList.add( setC );
			}
			List<String> category = getCategoryMenu(c, c2, setCategoryList);
			String viewStr = "";
			String viewPicture = "";
			String viewCompare = "";
			if (c2 != null ) {
				String narrowDown = getNarrowDown(c.getLang(), c, c2, null); // 2024/10/24 絞り込み検索 PreviewControllerにも同様の処理あり
				sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", narrowDown); // 2024/10/24 絞り込み検索
				viewStr = getListDisplaySelection(c.getLang(), c2, "list", null, 0, null);
				viewPicture = getListDisplaySelection(c.getLang(), c2, "picture", null, 0, null);
				if (c2.isCompare()) {
					viewCompare = getListDisplaySelection(c.getLang(), c2, "compare", null, 0, null);
				}
			} else {
				sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", "");
			}
			sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
			sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
			temp = StringUtils.replace(temp,"$$$sidebar$$$",sidebar);
			String preContent = temp;
			preContent = StringUtils.replace(preContent,"$$$h1box$$$", ""); // 特長書き出し用。検索ボックス、h1box無し。
			preContent = StringUtils.replace(preContent,"$$$formbox$$$", ""); // 特長書き出し用。検索ボックス、h1box無し。
			temp = StringUtils.replace(temp,"$$$formbox$$$", formbox);
			String tempPicture = StringUtils.replace(temp,"$$$h1box$$$", h1box + viewPicture);
			String tempCompare = StringUtils.replace(temp,"$$$h1box$$$", h1box + viewCompare);
			temp = StringUtils.replace(temp,"$$$h1box$$$", h1box + viewStr);

			if (c2 != null) {
				tempPicture = StringUtils.replace(tempPicture, "$$$content$$$", contentPicture);
				String writePicture = write + tempPicture;
				writePicture += t.getFooter();
				
				String writeCompare = null;
				if (c2.isCompare()) {
					tempCompare = StringUtils.replace(tempCompare, "$$$content$$$", contentCompare);
					writeCompare = write + tempCompare;
					writeCompare += t.getFooter();
				}
				
				temp = StringUtils.replace(temp, "$$$content$$$", content);
				write += temp;
				write += SeriesHtml._seriesCadModal;
				write += t.getFooter();
				if (isLocal) {
					write = changeLocalUrl(write, 4, c.getLang(), true);
					outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/index.html" , write);
				} else {
					outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/index.html" , write);
				}
				if (isLocal) {
					writePicture = changeLocalUrl(writePicture, 4, c.getLang(), true);
					outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/picture.html" , writePicture);
				} else {
					outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/picture.html" , writePicture);
				}
				if (c2.isCompare()) {
					if (isLocal) {
						writeCompare = changeLocalUrl(writeCompare, 4, c.getLang(), true);
						outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html" , writeCompare);
					} else {
						outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html" , writeCompare);
					}
				} else {
					// 落ちた可能性があるので、deleteを掛ける。
					String path = "";
					if (isLocal) { 
						path = htmlPath + ("/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html").substring(1);
					}
					else path = htmlPath + c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html";
					File file = new File(path);
		            if (file.exists() == false) {
		            	try {
		            		file.delete();
		            	} catch(Exception e) {
		            		log.debug(e.getMessage());
		            	}
		            }
				}
			} else {
				temp = StringUtils.replace(temp, "$$$content$$$", content);
				write+=temp;
				write+=t.getFooter();
				if (isLocal) {
					write = changeLocalUrl(write, 3, c.getLang(), true);
					write = changeTemplateUrl(write, c.getLang());
					outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/index.html" , write);
				} else {
					outputHtml( c.getLang()+"/"+c.getSlug()+"/index.html" , write);
				}
			}

			// 特長無し
			if (sList != null && sList.size() > 0 ) {
				for(String k : sList.keySet()) {
					if (k == null) continue;

					String tmp =  sList.get(k);
					if (isLocal) {
						tmp = changeLocalUrl(tmp, 5, c.getLang(), true);
						outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/s.html", tmp);
					} else {
						outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/s.html", tmp);
					}

					ErrorObject err = new ErrorObject();
					Series s = seriesService.getFromModelNumber(k, ModelState.PROD, err);
					List<String> cats = s.getCatpansHtml(c.getLang()); // カテゴリリンク
					String ca = "";
					for(String cat : cats) {
						ca += cat+"<br>\r\n";
					}
					tmp = tmp.replace("</h2>", "</h2>\r\n" + ca);
					if (isLocal) {
						// 検索結果で利用。検索結果は/webcatalog/lang/series/searchResult.htmlなので、3階層
						int layer = 3;
						// resultのみなので、changeLocalUrl()ではなくchangeOfflineUrlContents();
						String str = changeOfflineUrlContents(tmp, layer, c.getLang(), false);
						if (str.indexOf("<ul class=\"pro_service_bt\">") > -1) {
							// ボタン表示があれば表示するボタンを編集
							str = changeSeries(str, layer);
						}
						tmp = str;
						if (k.indexOf("/") > -1) k = k.replace("/", "_");
						outputHtml( "/webcatalog/"+c.getLang()+"/series/"+ k + "/s.html", tmp);

					} else {
						outputHtml( c.getLang()+"/series/"+ k + "/s.html", tmp);
					}

				}
			}
			// ガイド用
			if (gList != null && gList.size() > 0 ) {
				for(String k : gList.keySet()) {
					// メニューも必要。ただし、メニューは閉じる。
					String str = gList.get(k);
					str = "<div class=\"p_block\">\r\n" + str.replace("$$$backUrl$$$", "") + "\r\n</div><!-- .p_block -->";
					String output = StringUtils.replace(preContent, "$$$content$$$", str);
					output = output.replaceFirst("<span class.* CUCA_IDS\">.*</span>", "").replaceFirst("child open", "");
					output = t.getHeader() + output + SeriesHtml._seriesCadModal + t.getFooter();
					if (isLocal) {
						output = changeLocalUrl(output, 4, c.getLang(), true);
						outputHtml( "/webcatalog/"+c.getLang()+"/series/" + k + "/guide.html", output);
					} else {
						outputHtml( c.getLang()+"/series/" + k + "/guide.html", output);
					}
				}
			}
			// 特長あり
			if (seriesList != null && seriesList.size() > 0 ) {
				for(String k : seriesList.keySet()) {
					String output = StringUtils.replace(preContent, "$$$content$$$", seriesList.get(k));
					output = output.replaceFirst("<span class.* CUCA_IDS\">.*</span>", "");
					output = t.getHeader() + output + SeriesHtml._seriesCadModal + t.getFooter();
					String seriesStr = output;
					if (isLocal) {
						output = changeLocalUrl(output, 5, c.getLang(), true);
						output = output.replace("$$$backUrl$$$", "");
						outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/index.html", output);
					} else {
						outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/index.html", output);
					}

					// シリーズ単体
					if (isLocal) {
						seriesStr = changeLocalUrl(seriesStr, 4, c.getLang(), true);
						seriesStr = seriesStr.replace("$$$backUrl$$$", "");
						outputHtml( "/webcatalog/"+c.getLang()+"/series/" + k + "/index.html", seriesStr);
					} else {
						outputHtml( c.getLang()+"/series/" + k + "/index.html", seriesStr);
					}
				}
			}
			ret = true;
		}catch (Exception e) {
			log.error("ERROR!" + e.getMessage() );
			e.printStackTrace();
		}

		return ret;
	}
	/**
	 * 2026年デザイン用
	 * @param t
	 * @param tc
	 * @param c
	 * @param c2 null OK
	 * @param list null OK
	 * @param seriesService null OK
	 * @return
	 */
	public boolean outputTemplateCategoryToHtml2026(Template t, TemplateCategory tc, Category c, Category c2, 
			List<Series> list, CategoryService categoryService, SeriesService seriesService)
	{
		boolean ret = false;

		String write = t.getHeader();
		String temp = tc.getTemplate();

		
		List<String> catTitleList = new LinkedList<>(); // catpan
		List<String> catSlugList = new LinkedList<>(); // catpan
		String catpan = "";
		String formbox = "";
		String h1box = "";
		String content = "";
		String contentPicture = ""; // 写真一覧 2024/10/8 Add
		String contentCompare = ""; // 仕様比較 2024/10/8 Add
		Map<String, String> sList = null; // 検索結果（カテゴリ有、特長へのリンク、マイリスト有り。） カテゴリ一覧（カテゴリ無し、マイリスト有り）
		Map<String, String> gList = null; // ガイド用。特長なし、マイリスト無し。ページ作成
		Map<String, String> seriesList = null; // 特長あり。戻るボタンはreferrer
		try {
			if (c2 != null ) {
				sList = new HashMap<String, String>();
				gList = new HashMap<String, String>();
				seriesList = new HashMap<String, String>();
				if (c2.getSlug() != null && c2.getSlug().isEmpty() == false) {
					// 2024/10/8 写真一覧追加。index.htmlは今までのガイド一覧。picture.htmlは写真一覧。compare.htmlは仕様比較
					// catpan
					catTitleList = new LinkedList<>();
					catTitleList.add(c.getName());
					catTitleList.add(c2.getName());
					catSlugList = new LinkedList<>();
					catSlugList.add(c.getSlug());
					catSlugList.add(c2.getSlug());

					String url = AppConfig.ProdRelativeUrl + c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug();
					formbox = tc.getFormbox();
					h1box = getH1box2026(tc.getH1box(), c2.getName());

					Locale locale = getLocale(c.getLang());
					SeriesHtml sHtml = new SeriesHtml(locale, messagesource, omlistService, faqRepo);
					ErrorObject err = new ErrorObject();
					for(Series s : list) {
						s.setLink(seriesService.getLink(s.getId(), err));
						String str = sHtml.getGuide2026(s, c, c2, url, c.getLang(), false, false); // 特長、リスト無し
						sList.put(s.getModelNumber(), str);
						content += str + SeriesHtml._DivSeriesList2026;
						String strG =  sHtml.getGuide2026(s, c, c2, url, c.getLang(), false, true); // リスト用
						gList.put(s.getModelNumber(), strG);
						String strA = sHtml.getGuide2026(s, c, c2, url, c.getLang(), true, false); // 特長有り
						seriesList.put(s.getModelNumber(), strA);
					}
// 旧処理。動作確認OKなら削除 3/31
//					content = "<div class=\"p_block\">\r\n" + content + "</div><!-- p_block -->\r\n";
//					contentPicture = "<div class=\"p_block\">\r\n" + sHtml.getPictureList2026(c, c2, list) + "</div><!-- p_block -->\r\n";
					contentPicture = sHtml.getPictureList2026(c, c2, list);

					String baseLang = c.getLang();
					Lang langObj = langService.getLang(baseLang, err);
					if (langObj.isVersion()) {
						baseLang = langObj.getBaseLang();
					}
					if (c2.isCompare()) {
						HashMap<String, List<NarrowDownValue>> map = new HashMap<>();
						for (Series s : list) {
							List<NarrowDownValue> valList = narrowDownService.getCategorySeriesValue(c2.getId(), s.getId(), true, err);
							map.put(s.getId(), valList);
						}
						List<NarrowDownColumn> colList = narrowDownService.getCategoryColumn(c2.getId(), true, err);
						contentCompare = "<div class=\"p_block\">\r\n" + sHtml.getCompareHtml2026(c.getLang(), baseLang, c, c2, colList, list, map, null) + "</div><!-- p_block -->\r\n";
					}
				}
			} else {
				// catpan
				catTitleList.add(c.getName());
				catSlugList.add(c.getSlug());

				formbox =  tc.getFormbox();
				h1box = getH1box2026(tc.getH1box(), c.getName());
				content = tc.getContent();
				if (list != null && list.size() > 0 ) { // 大カテゴリにシリーズ一覧がある場合
					sList = new HashMap<String, String>();
					gList = new HashMap<String, String>();
					seriesList = new HashMap<String, String>();
					Locale locale = getLocale(c.getLang());
					SeriesHtml sHtml = new SeriesHtml(locale, messagesource, omlistService, faqRepo);
					ErrorObject err = new ErrorObject();
					String url = AppConfig.ProdRelativeUrl+c.getLang()+"/"+c.getSlug();
					for(Series s: list) {
						s.setLink(seriesService.getLink(s.getId(), err));
						String str = sHtml.get(s, c, c2, url, c.getLang(), false, false); // 特長なし
						sList.put(s.getModelNumber(), str);
						content += str;
						String strG =  sHtml.get(s, c, c2, url, c.getLang(), false, true);
						gList.put(s.getModelNumber(), strG);
						String strA = sHtml.get(s, c, c2, url, c.getLang(), true, false); // 特長あり
						seriesList.put(s.getModelNumber(), strA);
					}
					content = "<div class=\"p_block\">\r\n" + content + "</div><!-- p_block -->\r\n";
				}
			}
			// catpan
			temp = StringUtils.replace(temp,"$$$catpan$$$", getCatpan2026(c.getLang(), tc.getCatpan(), catTitleList, catSlugList));

			String sidebar =  tc.getSidebar();
			ErrorObject obj = new ErrorObject();
			List<Category> cList = categoryService.listAll(c.getLang(), ModelState.PROD, c.getType(), obj);
			// 2024/9/20 カテゴリの下にシリーズリンクを作成するため、シリーズの情報付加。
			List<Category> setCategoryList = new LinkedList<>();
			for(Category cate :  cList) {
				Category setC = categoryService.getWithSeries(cate.getId(), null, obj);
				if (setC != null) setCategoryList.add( setC );
			}
			String c2id = null;
			if (c2 != null) c2id = c2.getId();
			List<String> category = getCategoryMenu2026(c.getLang(), c.getId(), c2id, setCategoryList);
			String viewStr = "";
			String viewPicture = "";
			String viewCompare = "";
			if (c2 != null ) {
				String narrowDown = getNarrowDown2026(c.getLang(), c, c2, null); // 2024/10/24 絞り込み検索 PreviewControllerにも同様の処理あり
				sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", narrowDown); // 2024/10/24 絞り込み検索
				viewStr = getListDisplaySelection2026(c.getLang(), c2, "list", null, 0, null);
				viewPicture = getListDisplaySelection2026(c.getLang(), c2, "picture", null, 0, null);
				if (c2.isCompare()) {
					viewCompare = getListDisplaySelection2026(c.getLang(), c2, "compare", null, 0, null);
				}
			} else {
				sidebar = StringUtils.replace(sidebar,"$$$narrowdown$$$", "");
			}
			sidebar = StringUtils.replace(sidebar,"$$$category$$$",category.get(0));
			sidebar = StringUtils.replace(sidebar,"$$$category2$$$",category.get(1));
			temp = StringUtils.replace(temp,"$$$sidebar$$$",sidebar);
			String preContent = temp;
			preContent = StringUtils.replace(preContent,"$$$h1box$$$", ""); // 特長書き出し用。検索ボックス、h1box無し。
			preContent = StringUtils.replace(preContent,"$$$formbox$$$", ""); // 特長書き出し用。検索ボックス、h1box無し。
			temp = StringUtils.replace(temp,"$$$formbox$$$", formbox);
			String tempPicture = StringUtils.replace(temp,"$$$h1box$$$", h1box + viewPicture);
			String tempCompare = StringUtils.replace(temp,"$$$h1box$$$", h1box + viewCompare);
			temp = StringUtils.replace(temp,"$$$h1box$$$", h1box + viewStr);

			if (c2 != null) {
				tempPicture = StringUtils.replace(tempPicture, "$$$content$$$", contentPicture);
				String writePicture = write + tempPicture;
				writePicture += t.getFooter();
				
				String writeCompare = null;
				if (c2.isCompare()) {
					tempCompare = StringUtils.replace(tempCompare, "$$$content$$$", contentCompare);
					writeCompare = write + tempCompare;
					writeCompare += t.getFooter();
				}
				
				temp = StringUtils.replace(temp, "$$$content$$$", content);
				write += temp;
				write += SeriesHtml._seriesCadModal;
				write += t.getFooter();
				if (isLocal) {
					write = changeLocalUrl(write, 4, c.getLang(), true);
					outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/index.html" , write);
				} else {
					outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/index.html" , write);
				}
				if (isLocal) {
					writePicture = changeLocalUrl(writePicture, 4, c.getLang(), true);
					outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/picture.html" , writePicture);
				} else {
					outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/picture.html" , writePicture);
				}
				if (c2.isCompare()) {
					if (isLocal) {
						writeCompare = changeLocalUrl(writeCompare, 4, c.getLang(), true);
						outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html" , writeCompare);
					} else {
						outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html" , writeCompare);
					}
				} else {
					// 落ちた可能性があるので、deleteを掛ける。
					String path = "";
					if (isLocal) { 
						path = htmlPath + ("/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html").substring(1);
					}
					else path = htmlPath + c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/compare.html";
					File file = new File(path);
		            if (file.exists() == false) {
		            	try {
		            		file.delete();
		            	} catch(Exception e) {
		            		log.debug(e.getMessage());
		            	}
		            }
				}
			} else {
				temp = StringUtils.replace(temp, "$$$content$$$", content);
				write+=temp;
				write+=t.getFooter();
				if (isLocal) {
					write = changeLocalUrl(write, 3, c.getLang(), true);
					write = changeTemplateUrl(write, c.getLang());
					outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/index.html" , write);
				} else {
					outputHtml( c.getLang()+"/"+c.getSlug()+"/index.html" , write);
				}
			}

			// 特長無し
			if (sList != null && sList.size() > 0 ) {
				Locale locale = getLocale(c.getLang());
				SeriesHtml sHtml = new SeriesHtml(locale, messagesource, omlistService, faqRepo);
				for(String k : sList.keySet()) {
					if (k == null) continue;

					String tmp =  sList.get(k);
					if (isLocal) {
						tmp = changeLocalUrl(tmp, 5, c.getLang(), true);
						outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/s.html", tmp);
					} else {
						outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/s.html", tmp);
					}

					ErrorObject err = new ErrorObject();
					Series s = seriesService.getFromModelNumber(k, ModelState.PROD, err);
					// ガイドはカテゴリの一覧をパンくずで表示
					List<String> titleList = new LinkedList<>();
					List<String> slugList = new LinkedList<>();
					String catStr = "";
					boolean catRet = s.getCatpansTitleAndLink(titleList, slugList);
					if (catRet) {
						catStr += sHtml.getGuideCatpan2026(tc.getLang(), titleList, slugList);
					}
					tmp = tmp.replaceFirst("<div class=\"f w-full s-fclm m-fclm gap-48", catStr + "\r\n"+ "<div class=\"f w-full s-fclm m-fclm gap-48" );
					// mylist用
					String title = s.getName()+" "+s.getNumber();
					String link = AppConfig.ProdRelativeUrl+s.getLang()+"/"+ c.getSlug() + "/" + c2.getSlug() + "/"+ s.getModelNumber();
					tmp = tmp.replaceFirst("<div class=\"f ft gap-8\">", "<div class=\"f ft gap-8\" data-ml-title-row>\r\n<span class=\"chk_area\" data-ml-checkbox-slot></span>\r\n");
					tmp = tmp.replaceFirst("class=\"leading-tight text-primary text-2xl fw6 ", "data-ml-title=\"primary\" data-product-url=\""+link+"\" data-product-title=\""+title+"\" class=\"leading-tight text-primary text-2xl fw6 ");
					if (isLocal) {
						// 検索結果で利用。検索結果は/webcatalog/lang/series/searchResult.htmlなので、3階層
						int layer = 3;
						// resultのみなので、changeLocalUrl()ではなくchangeOfflineUrlContents();
						String str = changeOfflineUrlContents(tmp, layer, c.getLang(), false);
						if (str.indexOf("<ul class=\"pro_service_bt\">") > -1) {
							// ボタン表示があれば表示するボタンを編集
							str = changeSeries(str, layer);
						}
						tmp = str;
						if (k.indexOf("/") > -1) k = k.replace("/", "_");
						outputHtml( "/webcatalog/"+c.getLang()+"/series/"+ k + "/s.html", tmp);

					} else {
						outputHtml( c.getLang()+"/series/"+ k + "/s.html", tmp);
					}

				}
			}
			// ガイド用
			if (gList != null && gList.size() > 0 ) {
				for(String k : gList.keySet()) {
					// メニューも必要。ただし、メニューは閉じる。
					String str = gList.get(k);
					str = str.replace("$$$backUrl$$$", "");
					String output = StringUtils.replace(preContent, "$$$content$$$", str);
					output = output.replaceFirst("<span class.* CUCA_IDS\">.*</span>", "").replaceFirst("child open", "");
					output = t.getHeader() + output + SeriesHtml._seriesCadModal + t.getFooter();
					if (isLocal) {
						output = changeLocalUrl(output, 4, c.getLang(), true);
						outputHtml( "/webcatalog/"+c.getLang()+"/series/" + k + "/guide.html", output);
					} else {
						outputHtml( c.getLang()+"/series/" + k + "/guide.html", output);
					}
				}
			}
			// 特長あり
			if (seriesList != null && seriesList.size() > 0 ) {
				for(String k : seriesList.keySet()) {
					String output = StringUtils.replace(preContent, "$$$content$$$", seriesList.get(k) + tc.getProductsSupport() );
					output = output.replaceFirst("<span class.* CUCA_IDS\">.*</span>", "");
					output = t.getHeader() + output + SeriesHtml._seriesCadModal + t.getFooter();
					String seriesStr = output;
					if (isLocal) {
						output = changeLocalUrl(output, 5, c.getLang(), true);
						output = output.replace("$$$backUrl$$$", "");
						outputHtml( "/webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/index.html", output );
					} else {
						outputHtml( c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug() + "/" + k + "/index.html", output );
					}

					// シリーズ単体
					if (isLocal) {
						seriesStr = changeLocalUrl(seriesStr, 4, c.getLang(), true);
						seriesStr = seriesStr.replace("$$$backUrl$$$", "");
						outputHtml( "/webcatalog/"+c.getLang()+"/series/" + k + "/index.html", seriesStr);
					} else {
						outputHtml( c.getLang()+"/series/" + k + "/index.html", seriesStr);
					}
				}
			}
			ret = true;
		}catch (Exception e) {
			log.error("ERROR!" + e.getMessage() );
			e.printStackTrace();
		}

		return ret;
	}
	/**
	 * 指定Pathへhtmlを書き出し
	 * @param path
	 * @param html
	 * @return
	 */
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
            	boolean res = file2.mkdirs();
            	if (res == false) {
//            		throw new IOException("could not make dir. path="+tmp);
            	}
            }
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(html);
            bw.flush();
        } catch(IOException e) {
            e.printStackTrace();
            log.error("ERROR! IOException."+e.getMessage());
        } finally {
            try {
                if(bw != null) {
                    bw.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
                log.error("ERROR! IOException."+e.getMessage());
            }
        }
		return ret;
	}

	// ========== Offline Start ==========
	/**
	 *
	 * @param html
	 * @param layer
	 * @return
	 */
	public String changeLocalUrl(String html, int layer, String lang, boolean down) {
		String ret = "";
		if (html != null) {
			String[] arr = html.split("\r\n");
			String tmp = "";
			for(String str : arr) {
				// <div id="content"> から </div><!--content--> までをコンテンツとして個別に処理
				if (str.indexOf("<div id=\"content\">") > -1) {
					ret += changeOfflineUrlHeader(tmp, layer, lang);
					tmp = str+"\r\n";
				} else if (str.indexOf("</div><!--content-->") > -1) {
					String content = changeOfflineUrlContents(tmp, layer, lang, down);
					if (content.indexOf("<ul class=\"pro_service_bt\">") > -1) {
						// ボタン表示があれば表示するボタンを編集
						content = changeSeries(content, layer);
					}
					ret+=content;
					tmp = str+"\r\n";
				} else {
					tmp+= str+"\r\n";
				}
			}
			ret += changeOfflineUrlFooter(tmp, layer, lang);
		}
		return ret;
	}
	/**
	 * テンプレートの特定HTMLを削除。
	 * 特定開発品、ピックアップコンテンツなど。
	 * @param html
	 * @param layer
	 * @param lang
	 * @return
	 */
	public String changeTemplateUrl(String html, String lang) {
		String ret = "";
		if (html != null) {
			boolean isLi = false;
			boolean isSkipLi = false;
			String strLi = "";

			boolean isH1 = false;
			String strH1 = "";

			int cntSkip = 0;
			String skipStr = "";

			String[] arr = html.split("\r\n");
			for(String str : arr) {
				if (isSkipLi) {
					if (str.indexOf("</li>") > -1) {
						isSkipLi = false;
						isLi = false;
						strLi = "";
					}
				} else if (isLi) {
					if (str.indexOf("/pg/ja/index.html") > -1
							|| str.indexOf("/pg/en/index.html") > -1) {
						isSkipLi = true;
						strLi = "";
					} else if (str.indexOf("</li>") > -1) {
						isLi = false;
						ret += strLi + "\r\n";
						strLi = "";
					} else {
						strLi+= str+ "\r\n";
					}

				} else if (isH1) {
					if (str.indexOf("<h1>ピックアップコンテンツ</h1>") > -1
							|| str.indexOf("<h1>Pickup Contents</h1>") > -1) {
						cntSkip = 2;
						skipStr = "</div>";
						isH1 = false;
						strH1 = "";
					} else if (str.indexOf("</div>") > -1) {
						isH1 = false;
						ret += strH1 + str + "\r\n";
						strH1 = "";
					} else {
						strH1+= str+ "\r\n";
					}
				} else if (cntSkip > 0) {
					if (str.indexOf(skipStr) > -1) {
						cntSkip--;
					}
				} else if (str.indexOf("<li>") > -1) {
					isLi = true;
					strLi+= str+ "\r\n";
				} else if (str.indexOf("<div class=\"h1_box\"") > -1
						|| str.indexOf("<div class=\"h1_box \"") > -1) {
					isH1 = true;
					strH1+= str+ "\r\n";
				} else if (str.indexOf("<div class=\"wc_box\">") > -1) {
					cntSkip = 2;
					skipStr = "</div>";
				} else if (str.indexOf("/chiller/online/") > -1
						|| str.indexOf("/support/chiller/") > -1
						|| str.indexOf("/international/f-gas.jsp") > -1
						|| str.indexOf("/products/solutions/ja-jp/chiller/") > -1
						|| str.indexOf("/products/solutions/en-jp/chiller/") > -1
						|| str.indexOf("/temperature_controller/index.html") > -1) {
					if (str.indexOf("</a>") > -1) {

					} else {
						cntSkip = 1;
						skipStr = "</a>";
					}

				} else {
					ret += str + "\r\n";
				}
			}
		}
		return ret;
	}

	/**
	 * 言語のテンプレート変換。
	 * @param t
	 * @param layer
	 * @return
	 */
	public String offlineTemplate(Template t, int layer) {
		String ret = "";
		if (t != null) {
			String header = t.getHeader();
			if (header.isEmpty() == false) {
				ret += changeOfflineUrlHeader(header, layer, t.getLang());

			}
			String contents = t.getContents();
			if (contents.isEmpty() == false) {
				ret += changeOfflineUrlContents(contents, layer, t.getLang(), true);
			}
			String footer = t.getFooter();
			if (footer.isEmpty() == false) {
				ret += changeOfflineUrlFooter(footer, layer, t.getLang());
			}
		}
		// 特定開発品
		{
			ret = changeTemplateUrl(ret, t.getLang());
		}
		return ret;
	}
	/**
	 * CSS,JSをサーバーからDL。
	 * @param t
	 * @param layer 相対パスの階層
	 * @return
	 */
	public String offlineTemplateCategory(Template t,TemplateCategory tc, Category c, Category c2, List<Series> list,
			CategoryService categoryService, SeriesService seriesService, int layer) {
		String ret = "";
		if (t != null) {
			if (tc.is2026()) {
				outputTemplateCategoryToHtml2026(t, tc, c, c2, list, categoryService, seriesService);
			} else {
				outputTemplateCategoryToHtml(t, tc, c, c2, list, categoryService, seriesService);
			}

		}
		return ret;
	}
	/**
	 * 検索結果ページ作成
	 * @param t
	 * @param tc
	 * @param c
	 * @param c2
	 * @param list 全シリーズリスト。検索で利用
	 * @return
	 */
	public String offlineTemplateCategoryToResult(Template t,TemplateCategory tc, Category c, Category c2,
			List<Series> list) {
		String ret = "";
		String seriesContent = "";
		if (t != null) {
			// series配下にあるシリーズIDすべてのs.htmlを非表示で保持
			Path dirpath = Paths.get(htmlPath + "webcatalog/"+c.getLang()+"/series/");
			List<String> ids = new ArrayList<String>();
			try(Stream<Path> stream = Files.list(dirpath)) {
			      stream.forEach(p -> ids.add(p.getFileName().toString()));
		    }catch(IOException e) {
			      System.out.println(e);
		    }
			for(String sid : ids) {
				try {
					if (sid.indexOf(".") > -1) continue; // .は除外
				    File sFile = new File(htmlPath + "webcatalog/"+c.getLang()+"/series/"+sid+"/s.html");
				    if (sFile.exists()) {
				        Path pathS = Paths.get(sFile.getAbsolutePath());
						List<String> contents = Files.readAllLines(pathS);
						String tmp = "";
						for(String str : contents) {
							if (str.indexOf("<div class=\"result\">") > -1) {
								tmp+=str.replace("<div class=\"result\">", "<div class=\"result\" id=\""+sid+"\" style=\"display:none;\">")+"\r\n";
							} else {
								tmp+=str+"\r\n";
							}
						}
						seriesContent+=tmp;
				    } else {
				    	log.error("ERROR!! exists=false. s.html. sid= "+sid);
				    }
			    }catch(IOException e) {
				    log.error("ERROR!! read s.html. sid= "+sid);
			    }
			}

			// 保存されているファイルを読み込み、<div class="p_block">の中を空にする。
	        File ioFile = new File(htmlPath + "webcatalog/"+c.getLang()+"/"+c.getSlug()+"/"+c2.getSlug()+"/index.html");
        	Path path = Paths.get(ioFile.getAbsolutePath());
        	try {
				List<String> content = Files.readAllLines(path);
				boolean isPBlock = false;
				boolean isH1 = false;
				boolean isCatpan = false;
				Locale locale = getLocale(c.getLang());
				String strResult = messagesource.getMessage("g.search.result", null, locale);

				for(String str : content) {
					if ( str.indexOf("/smc_js/offline.js") > -1) {
						// listをjavascriptのオブジェクトに変換
						ret+="<script type=\"text/javascript\" language=\"javascript\">"+
						"var seriesList = [";
						int cnt = 0;
						for(Series s : list) {
							if (cnt == 1000) {
								ret+="];\r\nvar seriesList2 = [";
							}
							ret+="['"+s.getModelNumber()+"','"+s.getName()+"'],\r\n";
							cnt++;
						}
						if (cnt <= 1000) {
							ret+="];\r\nvar seriesList2 = [";
						}
						ret+="];</script>"+"\r\n";
						ret+="<script type=\"text/javascript\" language=\"javascript\" src=\"../../../smc_js/offline.js\"></script>\r\n";
						ret+="<script type=\"text/javascript\" language=\"javascript\" src=\"../../../smc_js/offline_result.js\"></script>\r\n";

					} else if (str.indexOf("<div class=\"p_block\">") > -1) {
						isPBlock = true;
						ret+=str + seriesContent;

					} else if (isPBlock) {
						// p_block内は除外
						if (str.indexOf("</div><!-- p_block -->") > -1) {
							isPBlock = false;
							ret+=str+"\r\n";
						}

					} else if (isH1) {
						if (str.indexOf("</div><!--h1_box-->") > -1) {
							isH1 = false;
						}
					} else if (str.indexOf("<div class=\"h1_box\">") > -1) {
						isH1 = true;

					} else if (str.indexOf("<div class=\"catpan\">") > -1) {
						if (str.indexOf("</div>") > -1) {
							String[] arr = str.split("<a");
							if (arr.length == 1) {
								ret += "</div>\r\n";
							} else {
								for(String tmp : arr) {
									if (tmp.indexOf("/air-management-system/") > -1) {
										ret+="&nbsp;»&nbsp;"+strResult+"</div>"+"\r\n";
									} else {
										ret+=tmp+"\r\n";
									}
								}
							}
						} else {
							isCatpan = true;
							ret+=str+"\r\n";
						}

					} else if (isCatpan) {
						if (str.indexOf("</div>") > -1) {
							String[] arr = str.split("<a");
							if (arr.length == 1) {
								ret += "</div>\r\n";
							} else {
								for(String tmp : arr) {
									if (tmp.indexOf("/air-management-system/") > -1) {
										ret+="&nbsp;»&nbsp;"+strResult+"</div>"+"\r\n";
									} else {
										ret+=tmp+"\r\n";
									}
								}
							}
							isCatpan = false;
						} else if (str.indexOf("/air-management-system/") > -1) {
							ret+="&nbsp;»&nbsp;"+strResult+"\r\n";
						} else if (str.indexOf("エアマネジメントシステム") > -1) {
							// なにもしない。
						} else {
							ret+=str+"\r\n";
						}
					} else {
						if (str.indexOf("../../") > -1) {
							ret+=str.replace("../../../../", "../../../")+"\r\n";;
						} else {
							ret+=str+"\r\n";;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.error("ERROR!! offlineTemplateCategoryToResult()");
			}
        	outputHtml( "/webcatalog/"+c.getLang()+"/series/searchResult.html", ret);
		}
		return ret;
	}

	private String changeOfflineUrlHeader(String header, int layer, String lang) {
		String ret = "";
		String[] arr = header.split("\r\n");
		boolean ulSkip = false;
		boolean scriptSkip = false;
		boolean divSkip = false;
		for(String str : arr) {
			if (ulSkip) {
				if (str.indexOf("</ul>") == 0) {
					ulSkip = false;
				} else {
					continue;
				}
			} else if (divSkip) {
				if (str.indexOf("</div>") > -1) {
					divSkip = false;
				} else {
					continue;
				}
			} else if (scriptSkip) {
				if (str.indexOf("navigator.userAgent") > -1) { // この行が来たら対象
					scriptSkip = false;
					ret+="<script>"+str+"\r\n";
				}
				else if (str.indexOf("</script>") > -1) {
					scriptSkip = false;
				} else {
					continue;
				}
			} else if (str.indexOf("stylesheet.jsp?id=") > -1 || str.indexOf("stylesheet.jsp?id=") > -1
					) {
				continue; // 除外
			} else if (str.indexOf("https://www.googletagmanager.com/gtag") > -1) {
				continue; // 除外
			} else if ( str.indexOf("assets/js/switching.js") > -1) {
				str = str.replace("<script src=\"/assets/js/switching.js\"></script>", "");
				ret+=str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n"; // webcatalog.js
				ret+="<link rel=\"stylesheet\" href=\""+getLayerString(layer)+"smc_css/offline.css\"></script>\r\n";
				ret+="<script type=\"text/javascript\" language=\"javascript\" src=\""+getLayerString(layer)+"smc_js/offline.js\"></script>\r\n";
				// offline.css jsをコピー
				File f = new File(env.getProperty("smc.webcatalog.static.page.path") + "smc_css/offline.css");
				File t = new File(htmlPath + "smc_css/offline.css");
				try {
					FileUtils.copyFile(f, t);
				} catch (Exception e) {
					// TODO: handle exception
				}
				f = new File(env.getProperty("smc.webcatalog.static.page.path") + "smc_js/offline.js");
				t = new File(htmlPath + "smc_js/offline.js");
				try {
					FileUtils.copyFile(f, t);
				} catch (Exception e) {
					// TODO: handle exception
				}
			} else if (str.indexOf("class=\"h_logo pc\"") > -1) { // リンク変更
				str = str.replace("href=\"//www.smcworld.com/"+lang+"/\"", "href=\""+getLayerString(layer)+"\"");
				str = str.replace("class=\"h_logo pc\"", "class=\"h_logo\"");
				ret+= str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n"; // img logo

			} else if (str.indexOf("class=\"m_logo\"") > -1) { // リンク変更
				str = str.replace("href=\"#\"", "href=\""+getLayerString(layer)+"\"");
				ret+=str+"\r\n";

			} else if (str.indexOf("src=\"/") > -1) {
				// srcなら保存してパス変換
				if (layer == 2) str = saveLocal("src=\"/", str, '"', lang, true, false);
				ret+=str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n";

			} else if ( str.indexOf("href=\"/") > -1 ) {
				// hrefなら保存してパス変換
				if (layer == 2) str = saveLocal("href=\"/", str, '"', lang, true, false);
				ret+=str.replace("href=\"/", "href=\""+getLayerString(layer))+"\r\n";

			} else if (str.indexOf("<ul class=\"head_bts\">") > -1) { // 除外
				ulSkip = true;
			} else if (str.indexOf("<ul class=\"menu01\">") > -1) { // 除外
				ulSkip = true;
			} else if (str.indexOf("<div class=\"menuWrapper\">") > -1) { // 除外
				divSkip = true;

			} else if (str.indexOf("<script src=\"/assets/js/global_sc.js\">") > -1) { // 除外
				// １行なので、何もしない。

			} else if (str.indexOf("<script>") > -1) { // 除外
				// 次がanalyticsなら除外
				scriptSkip = true;
			} else {
				ret+=str+"\r\n";
			}
		}
		return ret;
	}
	private String changeOfflineUrlContents(String contents, int layer, String lang, boolean down)  {
		String ret = "";
		String[] arr = contents.split("\r\n");

		boolean ulSkip = false;
		boolean divSkip = false;
		boolean aSkip = false;
		int cntSkip = 0;
		boolean tagSkip = false; // 特定のタグ、コメントまでスキップ
		boolean nextTagSkip = false; // 次のタグまでは生き。タグはスキップ
		String strSkip = "";
		boolean isDetail = false; // 特長に入ったらtrue;
		boolean isGuideDetail = false; // ガイドの詳細に入ったらtrue;
		boolean isGuideButton = false; // ガイドのボタンに入ったらtrue;

		boolean isCatLink = false; // catLinkGu と cate_link。pdf以外は除外。全部除外なら前後の囲みdivも削除
		String catLinkGu = "";

		int class_cnt_top_sec_title = 0;
		int class_cnt_oneColumn_cont_gr = 0;
		for(String str : arr) {
//			if (str.indexOf("jsy5000-h/images/10.jpg") > -1) {
//				log.info("jsy5000-h/images/10.jpg");
//			}
			if (tagSkip) {
				if (str.indexOf(strSkip) > -1) {
					tagSkip = false;
				}
				continue;
			}
			else if (nextTagSkip) {
				if (str.indexOf(strSkip) == -1) {
					ret+=str+"\r\n";
				} else {
					nextTagSkip = false;
					strSkip = "";
				}
				continue;
			}
			else if (ulSkip) {
				if (str.indexOf("</ul>") > -1) {
					ulSkip = false;
				} else {
					continue;
				}
			} else if (divSkip) {
				if (str.indexOf("</div>") > -1) {
					divSkip = false;
				} else {
					continue;
				}
			} else if (aSkip) {
				if (str.indexOf("</a>") > -1) {
					aSkip = false;
				} else {
					continue;
				}
			} else if (cntSkip > 0) {
				if (str.indexOf("</div>") > -1) {
					cntSkip--;
					if (cntSkip <= 0) {
						str = str.substring(str.indexOf("</div>")+"</div>".length());
						if (str.indexOf("<p class=\"search_result_discon\">") > -1) { // 生産終了のリンク除外
							if (str.indexOf("</p>") > -1) {
								String tmp = str.substring(0, str.indexOf("<p class=\"search_result_discon\">"));
								tmp+=str.substring(str.indexOf("</p>")+"</p>".length());
								ret+=tmp+"\r\n";
							} else {
								tagSkip = true;
								strSkip = "</p>";
							}
						}
						else if (str.indexOf("<table") > -1) {
							ret+=str.replace("</div>", "")+"\r\n";
						}
					}
				}
			} else if (isDetail) {
				if (str.indexOf("data-rel=\"lightcase\"") > -1
						||  str.indexOf("class=\"colorbox\"") > -1)  {
					if (str.indexOf("</a>") > -1) { // 同一行に閉じタグまである場合、中身を取得
						int start = str.indexOf("<a");
						if (start > -1) {
							String tmp = deleteAnchorTag(str);
							if (tmp.indexOf("src=\"/") > -1) {
								// srcなら保存してパス変換
								tmp = saveLocal("src=\"/", tmp, '"', lang, true, false);
								ret+=tmp.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n";
							} else {
								ret+=tmp;
							}
						}
					} else {
						nextTagSkip = true;
						strSkip = "</a>";
					}
				} else if (str.indexOf("src=\"/") > -1) {
						// srcなら保存してパス変換
						str = saveLocal("src=\"/", str, '"', lang, true, false);
						ret+=str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n";
				} else if (str.indexOf("<map name=\"ImageMap\">") > -1) { // ガイドのタブ除外
					tagSkip = true;
					strSkip = "</map>";
				} else {
					ret+=str+"\r\n";
				}
			} else if (str.indexOf("<a name=\"detail\"></a>") > -1)  { // 特長ページ
				isDetail = true;
				ret+=str+"\r\n";

			} else if (isGuideDetail) {
				if (str.indexOf("</ul>") > -1) {
					isGuideDetail = false;
				}
				// aタグは除去
				str = deleteAnchorTag(str);
				ret+=str+"\r\n";
			} else if (str.indexOf("<ul class=\"pro_details_text\">") > -1)  { // ガイドの詳細ページ
				isGuideDetail = true;
				ret+=str+"\r\n";

			} else if (isGuideButton) {
				if (str.indexOf("</ul>") > -1) {
					isGuideButton = false;
				}
				// pdf以外は除去
				if (str.indexOf(".pdf") > -1) {
					str = saveLocal("href=\"/", str, '"', lang, true, false);
					ret+=str.replace("href=\"/", "href=\""+getLayerString(layer))+"\r\n";
				} else if (str.indexOf("</ul>") > -1) { // PDFが無くて</ul>なら</ul>のみ追加
					ret+="</ul>"+"\r\n";
				}
			} else if (str.indexOf("<ul class=\"pro_service_bt\">") > -1)  { // ガイドのボタン
				isGuideButton = true;
				ret+=str+"\r\n";

			} else if (str.indexOf("<p class=\"search_result_discon\">") > -1) { // 生産終了のリンク除外
				if (str.indexOf("</p>") > -1) {
					String tmp = str.substring(0, str.indexOf("<p class=\"search_result_discon\">"));
					tmp+=str.substring(str.indexOf("</p>")+"</p>".length());
					ret+=tmp+"\r\n";
				} else {
					tagSkip = true;
					strSkip = "</p>";
				}

			} else if (str.indexOf("/catalog_option/fittings-and-tubing/") > -1) { // カラバリ 除外
				if (str.indexOf("</a>") > -1) {
					aSkip = false;
				} else {
					aSkip=true;
				}

			} else if (isCatLink) {
				String tmp = changeCatLinkGu(str, layer);
				if (tmp == null || tmp.trim().isEmpty()) {

				} else {
					catLinkGu+=tmp;
				}
				if (str.indexOf("</div>") > -1) {
					if (catLinkGu.indexOf("<a") > -1) {
						ret += catLinkGu+"\r\n";
					}
					catLinkGu = "";
					isCatLink = false;
				}
			} else if (str.indexOf("<div class=\"cat_link_gu\">") > -1
					|| str.indexOf("<div class=\"cate_link\">") > -1) {
				isCatLink = true;
				catLinkGu = str;

			} else if (str.indexOf("<a ") > -1
					&& (str.indexOf("/pg/en/search.do?") > -1
					|| str.indexOf("/pg/ja/search.do?") > -1
					|| str.indexOf("/products/ja/s.do?ca_id=") > -1
					|| str.indexOf("/products/en/s.do?ca_id=") > -1
					|| str.indexOf("/products/ja/freon/") > -1
					|| str.indexOf("/products/en/freon/") > -1
					|| str.indexOf("/pl/ja-jp/oil.html") > -1
					|| str.indexOf("/pl/en-jp/oil.html") > -1
					|| str.indexOf("/products/select_guide/") > -1
					|| str.indexOf("/products/pickup/") > -1
					|| str.indexOf("/rohs/ja/") > -1
					|| str.indexOf("/rohs/en/") > -1)) { // 特定開発品 除外
				if (str.indexOf("</a>") > -1) {
					// 閉じタグが同一行なら<a>で分解
					String tmp = changeCatLinkGu(str, layer);
					if (tmp == null || tmp.trim().isEmpty()) {
						// カラなら最後の"<div class=\"cat_link_gu\">"を削除
						int s = ret.lastIndexOf("<div class=\"cat_link_gu\">");
						if (s > -1) {
							ret = ret.substring(0, s);
							if (str.indexOf("</div>") == -1) { // 閉じdivが無ければ
								// "<div class=\"cat_link_gu\">"の閉じDivをスキップ
								divSkip = true;
							}
						}
					} else if (tmp.indexOf("</div>") == 0) {
						// カラなら最後の"<div class=\"cat_link_gu\">"を削除
						int s = ret.lastIndexOf("<div class=\"cat_link_gu\">");
						if (s > -1) {
							ret = ret.substring(0, s);
						}
					} else {
						ret+=tmp+"\r\n";
					}

				} else {
					aSkip=true;
				}

			} else if (str.indexOf("src=\"/") > -1) {
				// srcなら保存してパス変換
				str = saveLocal("src=\"/", str, '"', lang, true, false);

				// 画像リンクの場合 hrefも同時
				if ( str.indexOf("href=\"/") > -1 ) {
					if (str.indexOf(".jpg") > -1 || str.indexOf(".pdf") > -1 ) {
						// jpg,pdfなら保存してパス変換
						str = saveLocal("href=\"/", str, '"', lang, true, false);
						str = str.replace("href=\"/", "href=\""+getLayerString(layer))+"\r\n";
					}
				}
				ret+=str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n";

			} else if (str.indexOf("src=\"https://www.smcworld.com/") > -1) {
				// srcなら保存してパス変換
				str = str.replace("https://www.smcworld.com", ""); // フルパスから絶対へ。
				str = saveLocal("src=\"/", str, '"', lang, true, false);
				ret+=str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n";

			} else if ( str.indexOf("href=\"/webcatalog") > -1 ) {
				// /webcatalogなら変換のみ。
				if (layer == 2 && str.indexOf("/indexSearch/") > -1) {
					str = saveLocal("href=\"/", str, '"', lang, true, true);
				} else {
					str = saveLocal("href=\"/", str, '"', lang, false, false);
				}
				ret+=str.replace("href=\"/", "href=\""+getLayerString(layer))+"\r\n";

			} else if ( str.indexOf("href='/webcatalog") > -1 ) { // s.htmlのcatpan
				str = saveLocal("href='/", str, '\'', lang, false, false);
				ret+=str.replace("href='/", "href='"+getLayerString(layer))+"\r\n";

			} else if ( str.indexOf("href=\"/") > -1 ) {
				if (str.indexOf(".jpg") > -1 || str.indexOf(".pdf") > -1 ) {
					// jpg,pdfなら保存してパス変換
					str = saveLocal("href=\"/", str, '"', lang, true, false);
				} else if (str.indexOf(".html") > -1 ) {
					if (down) {
						// htmlなら保存してパス変換
						str = saveLocal("href=\"/", str, '"', lang, true, true);
					} else {
						str = saveLocal("href=\"/", str, '"', lang, false, false);
					}
				} else {
					// 他は保存しない
					str = saveLocal("href=\"/", str, '"', lang, false, false);
				}
				ret+=str.replace("href=\"/", "href=\""+getLayerString(layer))+"\r\n";

			} else if (str.indexOf("<div class=\"sform search\">") > -1) { // 除外
				// Full Part Number Search（検索窓）
				cntSkip=3; // </div>が２つ
			} else if (str.indexOf("<div class=\"bt_area\">") > -1) { // My list 除外
				cntSkip=3; // </div>が3つ

			} else if (str.indexOf("<ul class=\"s_tabs\">") > -1) { // 除外
				ulSkip = true;

			} else if (str.indexOf("<ul class=\"side_mylist\">") > -1) { // 除外
				ulSkip = true;
			} else if (str.indexOf("<div class=\"web_tabs\">") > -1) { // ガイドのタブ除外
				tagSkip = true;
				strSkip = "<!--web_tabs-->";
			} else if (str.indexOf("<map name=\"ImageMap\">") > -1) { // ガイドのタブ除外
				tagSkip = true;
				strSkip = "</map>";
			} else if (str.indexOf("<div class=\"additional\">") > -1) { // indexSearchの絞り込み除外
				ret+=str.substring(0, str.indexOf("<div class=\"additional\">"));
				cntSkip=3; // </div>が3つ

			} else if (str.indexOf("<div class=\"top_sec_title\">") > -1) { // 除外
				class_cnt_top_sec_title++;
				if (class_cnt_top_sec_title == 2) { // 2箇所目
					divSkip = true;
				} else {
					ret+=str+"\r\n";
				}

			} else if (str.indexOf("<div class=\"oneColumn_cont_gr\">") > -1) { // 除外
				class_cnt_oneColumn_cont_gr++;
				if (class_cnt_oneColumn_cont_gr == 2) { // 2箇所目
					divSkip = true;
				} else {
					ret+=str+"\r\n";
				}
			} else if (str.indexOf("action=\"/webcatalog/ja-jp/search3S/\"") > -1) {
				ret+=str.replace("action=\"/webcatalog/ja-jp/search3S/\"", "action=\""+getLayerString(layer)+"webcatalog/ja-jp/searchResult.html\"");
			} else if (str.indexOf("searchProductsSiteKeyword(") > -1) {
				// 検索js変換
				ret+=str.replace("searchProductsSiteKeyword(", "searchProductsSiteLocal(");//.replace("inactiveEnter(", "inactiveEnterLocal(");
			} else if (str.indexOf("<div class=\"cata_list_se\">") > -1) {
				ret+=str.replace("cata_list_se", "cata_list")+"\r\n";
			} else {
				ret+=str+"\r\n";
			}
		}
		return ret;
	}
	private String changeOfflineUrlFooter(String footer, int layer, String lang)  {
		String ret = "";
		String[] arr = footer.split("\r\n");
		boolean divSkip = false;
		for(String str : arr) {
			if (divSkip) {
				if (str.indexOf("</div><!--f_area_top-->") > -1) {
					divSkip = false;
				} else {
					continue;
				}
			} else if (str.indexOf("src=\"/") > -1) {
				// srcなら保存してパス変換
				if (layer == 2) str = saveLocal("src=\"/", str, '"', lang, true, false);
				else str = saveLocal("src=\"/", str, '"', lang, false, false);
				ret+=str.replace("src=\"/", "src=\""+getLayerString(layer))+"\r\n";

			} else if (str.indexOf("<div class=\"f_area_top\">") > -1) { // 除外
				divSkip = true;
			} else {
				ret+=str+"\r\n";
			}
		}
		return ret;
	}
	String layerStr = "../";
	private String getLayerString(int layer) {
		String ret = "";
		if (layer > 0) {
			for(int i = 0; i < layer; i++) {
				ret+=layerStr;
			}
		}
		return ret;
	}
	private String changeSeries(String html, int layer) {
		String ret = "";
		String[] arr = html.split("\r\n");

		boolean proServiceBt = false;
		boolean proDetailsText = false;
		boolean isCatalog = false;

		boolean isCatLinkGu = false;
		String strCatLinkGu = "";

		for(String str : arr) {
			if (str == null || str.isEmpty() == true) continue;

			if (str.indexOf("<ul class=\"pro_details_text\">") > -1) {
				proDetailsText = true; // <a>除去
				ret+=str+"\r\n";
			} else if (proDetailsText) {
				if (str.indexOf("</ul>") > -1) {
					proDetailsText=false;
					if (str.indexOf("<a") > -1) {
						String tmp = deleteAnchorTag(str);
						if (tmp != null && tmp.isEmpty() == false) {
							str = tmp;
						}
					}
					ret+=str+"\r\n";
				} else {
					// <a>除去
					String tmp = deleteAnchorTag(str);
					if (tmp != null && tmp.isEmpty() == false) {
						str = tmp;
					}
					ret+=str+"\r\n";
				}
			} else if (str.indexOf("<ul class=\"pro_service_bt\">") > -1) {
				proServiceBt = true;
				ret+=str+"\r\n";
			} else if (proServiceBt) {
				if( str.indexOf("<li class=\"cat_pdf\">") > -1) {
					if (str.indexOf("</li>") > -1) { // １行の場合
						isCatalog=false;
					} else {
						isCatalog=true;
					}
					ret+=str+"\r\n";
				} else if (isCatalog) {
					if (str.indexOf("</li>") > -1) isCatalog = false;
					ret+=str+"\r\n";
				} else if (str.indexOf("</ul>") > -1) {
					isCatalog = false;
					proServiceBt = false;
					ret+=str+"\r\n";
				}
			} else if (isCatLinkGu) {
				if (str.indexOf("</div>") > -1 ) {
					isCatLinkGu = false;
					String tmp =changeCatLinkGu(str, layer);
					if (tmp != null && tmp.isEmpty() == false) {
						if (strCatLinkGu.indexOf("<a") == -1) {
							// <a>がなければ対象外。スキップ

						} else {
							ret+=strCatLinkGu+tmp+"\r\n";
						}
					} else if (strCatLinkGu.trim().equals("<div class=\"cat_link_gu\">")){
						// カラならスキップ
						int s = ret.lastIndexOf("<div class=\"cat_link_gu\">");
						if (s > -1) {
							ret = ret.substring(0, s);
						}
					} else {
						ret+=strCatLinkGu;
					}
				} else {
					strCatLinkGu += changeCatLinkGu(str, layer);
				}
			} else if (str.indexOf("<div class=\"cat_link_gu\">") > -1) {
				if (str.indexOf("</div>") > -1) {
					String tmp = changeCatLinkGu(str, layer);
					if (tmp != null && tmp.isEmpty() == false) {
						if (tmp.indexOf("</div>") == 0) {
							// </div>のみは対象外。スキップ
							int s = ret.lastIndexOf("<div class=\"cat_link_gu\">");
							if (s > -1) {
								ret = ret.substring(0, s);
							}
						} else {
							ret+=str+"\r\n";
							ret+=tmp+"</div>"+"\r\n";
						}
					}
					// カラならスキップ
				} else {
					isCatLinkGu = true;
					// 次は改行が無い。<a で区切る
					// <a href="/upfiles/pl/M-03-3C-01.pdf" target="_blank">安全上のご注意</a>　<a href="/upfiles/pl/M-03-3C-02.pdf" target="_blank">3・4・5ポート電磁弁 共通注意事項</a><br/><a href="/catalog/BEST-technical-data/pdf/6-1-1-m12-29.pdf" target="_blank">機種選定・技術資料</a>　<a href="/pg/ja/search.do?ca_id=A01&kw=&off=0">特定開発品情報</a>　<a href="/rohs/ja/" target="_blank">グリーン対応（RoHS）</a>
					strCatLinkGu = str;
				}
			} else {
				ret+=str+"\r\n";
			}
		}
		return ret;
	}

	private String deleteAnchorTag (String str) {
		String ret = "";

		if (str.indexOf("<a") > -1) {
			str = str.replaceAll("</a>", "");
			String[] arr = str.split("<a");
			if (arr != null && arr.length > 1) {
				int cnt = 0;
				for(String s : arr) {
					if (cnt == 0) ret += s;
					else {
						int end = s.indexOf(">");
						if (end > -1) {
							ret+=s.substring(end+1);
						}
					}
					cnt++;
				}
			}
		} else {
			ret = str;
		}

		return ret;
	}
	// </div>のみが返る場合は</div>が同一行でカラの場合
	private String changeCatLinkGu(String str, int layer) {
		String ret = "";

		String key = "href=\"/";
		boolean changePath = true; // 変換前
		if (str.indexOf("../../") > -1) {
			// 変換後
			key = "href=\""+ getLayerString(layer);
			changePath = false;
		}

		// <a で区切る
		String[] arr2 = str.split("<a");
		for(String str2 : arr2) {
			if (str2.indexOf(".pdf") > -1) {
				// ローカルに無ければ保存
				int start = str2.indexOf(key);
				int end = str2.indexOf('"', start+key.length());

				if (start > -1 && end > -1) {
					String url = str2.substring(start+key.length(), end); // ../../webcatalog/en-jp/ -> webcatalog/en-jp/
					File f = new File(htmlPath + url);
					if (f.exists() == false) {
						try {
							URL fetchWebsite = new URL(AppConfig.PageProdUrl +"/"+ url);
					        FileUtils.copyURLToFile(fetchWebsite, f);
						}catch (Exception e) {
							// TODO: handle exception
						}

					}
					if (changePath) {
						str2 = str2.replace(key, "href=\""+getLayerString(layer))+"\r\n";
					}
					ret+="<a "+str2;
				}
			}
		}
		if (str.indexOf("</div>") > -1 && ret.indexOf("</div>") == -1) {
			ret+="</div>"+"\r\n";
		}

		return ret;
	}
	/**
	 *
	 * @param key
	 * @param link
	 * @param quote
	 * @param save
	 * @param changeLocal 保存したhtmlをローカル用に変換
	 * @return html中の indexSearch/A は indexSearch/A/index.html にしてhtml全体を返す。
	 */
	private String saveLocal(String key, String link, char quote, String lang, boolean save, boolean changeLocal) {
		String ret = link;
		String file = null;

		int start = link.indexOf(key);
		int end = link.indexOf(quote, start+key.length());

		String url = link.substring(start+key.length(), end); // /webcatalog/en-jp/ -> webcatalog/en-jp/

		String[] arr = url.split("/");

		if (url.lastIndexOf('/') == url.length()-1) { // スラッシュで終っている場合、/index.htmlで返す。
			file = url+"index.html";
			ret = ret.replace(url, url+"index.html");
		}
		else if (arr.length > 1) { // スラッシュを含む場合
			if (arr[arr.length-1].equals("#detail")) {
				file = url.replace("#detail", "index.html#detail"); // #detail で終わっている場合。/#detail -> /index.html#detailにする
				ret = ret.replace(url, url.replace("#detail", "index.html#detail"));
			} else if (arr[arr.length-1].indexOf(".jpg?") > -1) {
				// .jpg?01 などの?01を取る
				int s =url.indexOf("?");
				if (s > -1) file = url.substring(0, s);
			} else if (arr[arr.length-1].indexOf(".") == -1) { // 最後に拡張子があるか。なければ/index.htmlを付ける。
				file = url+"/index.html";
				ret = ret.replace(url, url+"/index.html");

			} else {
				file = url;
			}
		}
//		if (file.indexOf("/en-jp/index.html") > -1) {
//			log.info("/en-jp/index.html : file="+file);
//		}
		if (save) {
			try {
				URL fetchWebsite = new URL(AppConfig.PageCDNUrl +"/"+ url); // wwwだと文字化け。cdn.に変更。2023/8/25
				if (fetchWebsite == null) throw new Exception("URL is null. url="+url);
		        File ioFile = new File(htmlPath + file);
				if (ioFile == null) throw new Exception("File is null. file="+file);
		        FileUtils.copyURLToFile(fetchWebsite, ioFile);

		        if (ioFile.getName().indexOf(".css") > -1) { // cssなら中身を変換
		        	// 中にURLがあれば保存。パス変換
		        	String write = "";
		        	Path path = Paths.get(ioFile.getAbsolutePath());
		        	List<String> content = Files.readAllLines(path);
		        	for(String str : content) {
		        		if (str.indexOf("url('/") > -1) {
							// srcなら保存してパス変換
							str = saveLocal("url('/", str, '\'', lang, true, false); // 先頭が/なら階層は1
							write+=str.replace("url('/", "url('"+getLayerString(1))+"\r\n";
						} else if ( str.indexOf("url(\"") > -1 ) {
							// hrefなら保存してパス変換
							str = saveLocal("url(\"/", str, '"', lang, true, false);
							write+=str.replace("url(\"/", "url(\""+getLayerString(1))+"\r\n";
						} else if ( str.indexOf("url(/") > -1 ) {
							// hrefなら保存してパス変換
							str = saveLocal("url(/", str, ')', lang, true, false);
							write+=str.replace("url(/", "url("+getLayerString(1))+"\r\n";
						} else {
							write+=str+"\r\n";
						}
		        	}
		        	if (write.isEmpty() == false) {
		        		FileWriter filewriter = new FileWriter(ioFile);
		        		filewriter.write(write);
		        		filewriter.close();
		        	}
		        }
		        if (changeLocal) {
		        	// パスからlayer取得
		        	String[] arr2 = url.split("/");
		        	int layer = arr2.length;

		        	Path path = Paths.get(ioFile.getAbsolutePath());
		        	List<String> list = Files.readAllLines(path);
		        	String html = "";
		        	for(String s : list) {
		        		html+=s+"\r\n";
		        	}

		        	html = changeLocalUrl(html, layer, lang, false);
		        	FileWriter filewriter = new FileWriter(ioFile);
	        		filewriter.write(html);
	        		filewriter.close();
		        }

			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		return ret;
	}
	// ========== Offline End ==========

	public String getSearchResult(String kw,
			String lang,  Template t, TemplateCategory tc, CategoryService categoryService,
			List<Series> list, int page, int hitCnt, String message)
	{
		String ret = "";

		String temp = tc.getTemplate();

		Locale locale = getLocale(lang);
		temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", messagesource.getMessage("msg.search.title", null, locale)));
		temp = StringUtils.replace(temp,"$$$sidebar$$$", tc.getSidebar().replace("class=\"child open\"", "class=\"child\""));
		if (kw != null && kw.isEmpty() == false) temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox().replaceFirst("value=\"\"", "value=\""+kw+"\""));
		else temp = temp.replace("$$$formbox$$$", "");
		temp = StringUtils.replace(temp,"$$$h1box$$$", ""); // 検索結果のタイトルは要らない
		temp = StringUtils.replace(temp,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない

		ErrorObject obj = new ErrorObject();
		List<Category> cList = categoryService.listAll(lang, ModelState.PROD, CategoryType.CATALOG, obj);
		List<String> category = getCategoryMenu(lang, null, null, cList);
		temp = StringUtils.replace(temp,"$$$category$$$",category.get(0));
		temp = StringUtils.replace(temp,"$$$category2$$$",category.get(1));
		
		String content = "";
		if (message != null && message.isEmpty() == false) {
			content+= "<p>"+message+ "</p><br>";
		} else {
			if (hitCnt > 0) content+= "<p>"+ messagesource.getMessage("msg.search.hit", new Object[] {kw, hitCnt}, locale) + "</p><br>";
			if (isDisconHit(lang, kw)) content +="<p class=\"search_result_discon\"><a href=\""+AppConfig.PageProdDisconUrl+lang+"/?kw=" + kw +"\">"+messagesource.getMessage("msg.search.discon.hit", null, locale) + "</a></p><br>\r\n";
		}
		// 絞り込み

		if (list != null && list.size() > 0) {
			for(Series s : list) {
				if (s.isPre()) {
					content+= "<br><p>" + messagesource.getMessage("msg.search.pre", null, locale) + "</p>";
					content+= "<br><br><p><a href=\"/webcatalog/"+lang+"/\" style=\"color:#0072c1\">" + messagesource.getMessage("msg.search.pre.return", null, locale) + "</a></p>\r\n";
				} else {
					content += getFileFromHtml(lang + "/series/" + s.getModelNumber() + "/s.html");
				}
			}
			if (content == null || content.isEmpty()) { // en-jpなのにxxx-ZHなど
				content+= "<br><p>" + messagesource.getMessage("msg.search.empty", null, locale) + "</p>";
				if (kw == null || kw.isEmpty()) content+= "<br><br><p><a href=\"/webcatalog/"+lang+"/\" style=\"color:#0072c1\">" + messagesource.getMessage("msg.search.pre.return", null, locale) + "</a></p>\r\n";
			}
		} else {
			content+= "<br><p>" + messagesource.getMessage("msg.search.empty", null, locale) + "</p>";
			if (kw == null || kw.isEmpty()) content+= "<br><br><p><a href=\"/webcatalog/"+lang+"/\" style=\"color:#0072c1\">" + messagesource.getMessage("msg.search.pre.return", null, locale) + "</a></p>\r\n";
		}
		temp = StringUtils.replace(temp, "$$$content$$$", content);
		temp = "<div class=\"p_block\">" + temp + "</div>"; // <h2>の色付けに必要
		//PageProdDisconUrl
		ret = t.getHeader() + temp + SeriesHtml._seriesCadModal + t.getFooter();
		return ret;

	}
	public String getSearchResult2026(String kw,
			String lang,  Template t, TemplateCategory tc, CategoryService categoryService, SeriesService seriesService, SeriesHtml sHtml,
			List<Series> list, int page, int hitCnt, String message, Boolean isTestSite)
	{
		String ret = "";

		String temp = tc.getTemplate();

		Locale locale = getLocale(lang);
		List<String> titleList = new LinkedList<>();
		titleList.add(messagesource.getMessage("msg.search.title", null, locale));
		List<String> slugList = new LinkedList<>();
		slugList.add("#");
		temp = StringUtils.replace(temp,"$$$catpan$$$", getCatpan2026(lang, tc.getCatpan(), titleList, slugList));
		
		String catpan = "<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"></li>"
				+ "<li><a class=\"breadcrumb-item\" href=\"/webcatalog/"+lang+"/\">CATPAN</a>\r\n"
				+ "              </li>";
		temp = StringUtils.replace(temp,"$$$catpan_title$$$", catpan.replace("CATPAN", getSearchResultTitle(lang)));
		
		temp = StringUtils.replace(temp,"$$$sidebar$$$", tc.getSidebar().replaceFirst("is-current\"", "\"")); // メニューを閉じる
		if (kw != null && kw.isEmpty() == false) temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox().replaceFirst("value=\"\"", "value=\""+kw+"\""));
		else temp = temp.replace("$$$formbox$$$", "");
		temp = StringUtils.replace(temp,"$$$h1box$$$", ""); // 検索結果のタイトルは要らない
		temp = StringUtils.replace(temp,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない

		ErrorObject obj = new ErrorObject();
		List<Category> cList = categoryService.listAll(lang, ModelState.PROD, CategoryType.CATALOG, obj);
		List<String> category = getCategoryMenu2026(lang, null, null, cList);
		temp = StringUtils.replace(temp,"$$$category$$$",category.get(0));
		temp = StringUtils.replace(temp,"$$$category2$$$",category.get(1));
		
		String content = "";
		if (message != null && message.isEmpty() == false) {
			content+= "<p>"+message+ "</p><br>";
		} else {
			if (hitCnt > 0) content+= "<p>"+ messagesource.getMessage("msg.search.hit", new Object[] {kw, hitCnt}, locale) + "</p><br>";
			if (isDisconHit(lang, kw)) content +="<p class=\"search_result_discon\"><a href=\""+AppConfig.PageProdDisconUrl+lang+"/?kw=" + kw +"\">"+messagesource.getMessage("msg.search.discon.hit", null, locale) + "</a></p><br>\r\n";
		}
		// 絞り込み

		if (list != null && list.size() > 0) {
			int cnt = 0;
			int max = list.size();
			for(Series s : list) {
				if (s.isPre()) {
					content+= "<br><p>" + messagesource.getMessage("msg.search.pre", null, locale) + "</p>";
					content+= "<br><br><p><a href=\"/webcatalog/"+lang+"/\" style=\"color:#0072c1\">" + messagesource.getMessage("msg.search.pre.return", null, locale) + "</a></p>\r\n";
				} else if (tc.is2026() && isTestSite) { // 新デザイン && TEST
					s.setLink(seriesService.getLink(s.getId(), obj));
					content += sHtml.getGuide2026(s, null, null, "", lang, false, true);
					if (cnt < max-1) content += "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>";
				} else {
					content += getFileFromHtml(lang + "/series/" + s.getModelNumber() + "/s.html");
				}
				cnt++;
			}
			if (content == null || content.isEmpty()) { // en-jpなのにxxx-ZHなど
				content+= "<br><p>" + messagesource.getMessage("msg.search.empty", null, locale) + "</p>";
				if (kw == null || kw.isEmpty()) content+= "<br><br><p><a href=\"/webcatalog/"+lang+"/\" style=\"color:#0072c1\">" + messagesource.getMessage("msg.search.pre.return", null, locale) + "</a></p>\r\n";
			}
		} else {
			content+= "<br><p>" + messagesource.getMessage("msg.search.empty", null, locale) + "</p>";
			if (kw == null || kw.isEmpty()) content+= "<br><br><p><a href=\"/webcatalog/"+lang+"/\" style=\"color:#0072c1\">" + messagesource.getMessage("msg.search.pre.return", null, locale) + "</a></p>\r\n";
		}
		temp = StringUtils.replace(temp, "$$$content$$$", content);
		temp = "<div class=\"p_block\">" + temp + "</div>"; // <h2>の色付けに必要
		//PageProdDisconUrl
		ret = t.getHeader() + temp + SeriesHtml._seriesCadModal + t.getFooter();
		return ret;

	}

	// 生成済みのGuideの一覧を取得
	// highlight()を最後に付与
	// pagePathはlogin.do?dst=に使う
	public String getSearchResultFromSeriesFile(String kw, String lang,  Template t, TemplateCategory tc, 
			CategoryService categoryService, SeriesHtml sHtml, List<Series> list, String pagePath, int page, int hitCnt, boolean isTest)
	{
		String ret = "";
		boolean isNoHit = false; // 検索結果0件

		Locale locale = getLocale(lang);

		String temp = tc.getTemplate();

		if (tc.is2026()) {
			// catpan
			List<String> titleList = new LinkedList<>();
			titleList.add(messagesource.getMessage("msg.search.title", null, locale));
			List<String> slugList = new LinkedList<>();
			slugList.add("#");
			temp = StringUtils.replace(temp,"$$$catpan$$$", getCatpan2026(lang, tc.getCatpan(), titleList, slugList));
			temp = StringUtils.replace(temp,"$$$formbox$$$", "");
			temp = StringUtils.replace(temp,"$$$sidebar$$$", tc.getSidebar());
			temp = StringUtils.replace(temp,"$$$h1box$$$", getH1box2026(tc.getH1box(), kw)+tc.getFormbox().replace("pt24 px96 pb36 s-p24 s-gap-32 m-p24 m-gap-32", "p24 border s-px16 m-px16")); // 検索結果のh1にはキーワード、その下にformbox
			temp = StringUtils.replace(temp,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない

		} else {
			// 旧
			temp = StringUtils.replace(temp,"$$$catpan$$$", tc.getCatpan().replace("$$$title$$$", messagesource.getMessage("msg.search.title", null, locale)
					+ "<div id=\"kw\" class=\"hidden\">"+kw.replace(" ", ",")+"</div>"));
			temp = StringUtils.replace(temp,"$$$sidebar$$$", tc.getSidebar().replace("class=\"child open\"", "class=\"child\""));
			temp = StringUtils.replace(temp,"$$$formbox$$$", tc.getFormbox());
			temp = StringUtils.replace(temp,"$$$h1box$$$", ""); // 検索結果のタイトルは要らない
			temp = StringUtils.replace(temp,"$$$narrowdown$$$", ""); // 検索結果の絞り込みは要らない
		}
		
		if (temp != null) temp = temp.replaceFirst("value=\"\" id=\"kwSite\"", "value=\""+kw+"\" id=\"kwSite\"");

		ErrorObject obj = new ErrorObject();
		List<Category> cList = null;
		List<String> category = null;
		if (isTest) {
			cList = categoryService.listAll(lang, ModelState.TEST, CategoryType.CATALOG, obj);
		} else {
			cList = categoryService.listAll(lang, ModelState.PROD, CategoryType.CATALOG, obj);
		}
		if (tc.is2026()) {
			category = getCategoryMenu2026(lang, null, null, cList);
		} else {
			category = getCategoryMenu(lang, null, null, cList);
		}
		temp = StringUtils.replace(temp,"$$$category$$$",category.get(0));
		temp = StringUtils.replace(temp,"$$$category2$$$",category.get(1));

		String content = "";
		if (list != null && list.size() > 0) {
			if (tc.is2026()) {
				 // 検索結果 7件
				content+= "<div class=\"mt48 mb24 s-mt36 s-mb8 s-mt36 m-mb8\">\r\n"
						+ "          <div class=\"f fm mb24 gap-16\">\r\n"
						+ "                    <div class=\"text-2xl fw6 leading-tight\">"+messagesource.getMessage("g.search.result", null, locale)+"</div>\r\n"
						+ "                    <div class=\"badge large filled\">"+hitCnt+"件</div>\r\n"
						+ "          </div>\r\n"
						+ "<p class=\"text-base\">キーワード検索に"+hitCnt+"件のヒットがありました。</p>\r\n";
				if (isDisconHit(lang, kw)) {
					String strDiscon = "";
					if (lang.indexOf("en-") > -1) {
						strDiscon = "Your search matched discontinued products. Please click here for details.";
					} else if (lang.indexOf("zh-") > -1) {
						strDiscon = "结果中包含了停产产品，点击此处了解更多信息。";
					} else {
						strDiscon = "生産終了製品のご案内にもヒットしています。詳細はこちらをクリックしてください。";
					}
					content += "  <a class=\"text-sm leading-tight text-primary\" href=\""+AppConfig.PageProdDisconHeadUrl + lang + "/" + kw +"\"><span class=\"fw5 hover-link-underline\">"+strDiscon+"</span><img class=\"inline-block vertical-align-text-bottom s16 ml4 object-fit-contain\" src=\"/assets/smcimage/common/blank-primary.svg\" alt=\"\" title=\"\"></a>\r\n";
				}
				content+= "</div>";
			} else {
				content+= "<p>"+ messagesource.getMessage("msg.search.keyword.hit", new Object[] { hitCnt}, locale) + "</p>\r\n<br>";
				if (isDisconHit(lang, kw)) {
					content +="<p class=\"search_result_discon\">\r\n"
							+ "<a href=\""+AppConfig.PageProdDisconUrl + lang +"/?kw=" + kw +"\">\r\n"+
							messagesource.getMessage("msg.search.discon.hit", null, locale) + "</a>\r\n"
									+ "</p><br>\r\n";
				}
			}
		}
		// ページ送り
		if (hitCnt > 10) {
			content += "<div class=\"navi\">\r\n";
			double db =  Math.ceil((double)hitCnt/10);
			int s = page - 5;
			int e = page + 5;
			if (s <= 0) {e -= s; s=1;}
			if (e > db) e = (int)db;
			if (tc.is2026()) {
				content = StringUtils.replace(content, "<div class=\"navi\">", "<div class=\"navi mb24\">");
				if (s < page) content += "<button class=\"button w30 h35 mx4 secondary \" type=\"button\" onclick=\"location.href='"+AppConfig.ProdRelativeUrl+lang+"/searchSite/?kw="+kw+"&page="+(page-1)+"'\">&lt;</button>\r\n";
				for(; s <= e; s++) {
					if (page != s) content += "<button class=\"button solid w30 h35 mx4 secondary \" type=\"button\" onclick=\"location.href='"+AppConfig.ProdRelativeUrl+lang+"/searchSite/?kw="+kw+"&page="+s+"'\">"+s+"</button>\r\n";
					else content += "<button class=\"button solid w30 h35 mx4 primary\" type=\"button\" >"+ page+"</button>\r\n";
				}
				if (e > page) content += "<button class=\"button w30 h35 mx4 secondary \" type=\"button\" onclick=\"location.href='"+AppConfig.ProdRelativeUrl+lang+"/searchSite/?kw="+kw+"&page="+(page+1)+"'\">&gt;</button>";
			} else {
				if (s < page) content += "<a href=\""+AppConfig.ProdRelativeUrl+lang+"/searchSite/?kw="+kw+"&page="+(page-1)+"\" class=\"back\">&lt;</a>\r\n";
				for(; s <= e; s++) {
					if (page != s) content += "<a href=\""+AppConfig.ProdRelativeUrl+lang+"/searchSite/?kw="+kw+"&page="+s+"\" class=\"pn\">"+s+"</a>\r\n";
					else content += "<span class=\"pn current\">" + page+"</span>\r\n";
				}
				if (e > page) content += "<a href=\""+AppConfig.ProdRelativeUrl+lang+"/searchSite/?kw="+kw+"&page="+(page+1)+"\" class=\"fw\">&gt;</a>\r\n";
			}
			content += "</div>\r\n";
		}

		if (list != null && list.size() > 0) {
			int cnt = 0;
			for (Series s : list) {
				if (isTest) {
					if (tc.is2026()) {
						content += sHtml.getGuide2026(s, null, null, pagePath, lang, false, true);
						if (cnt < list.size() -1) content += "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>";
					} else {
						content += sHtml.get(s, null, null, pagePath, lang, false, true);
					}
				} else {
					content += getFileFromHtml(lang + "/series/" + s.getModelNumber() + "/s.html");
				}
				cnt++;
			}
		} else {
			if (tc.is2026()) {
				String str = "<div class=\"mt48 mb24 s-mt36 s-mb8 s-mt36 m-mb8\">\r\n"
						+ "          <div class=\"f fm mb24 gap-16\">\r\n"
						+ "                    <div class=\"text-2xl fw6 leading-tight\">"+messagesource.getMessage("g.search.result", null, locale)+"</div>\r\n"
						+ "                    <div class=\"badge large filled\">"+messagesource.getMessage("msg.search.hit.count", new Object[] {0}, locale)+"</div>\r\n"
						+ "          </div>\r\n"
						+ "</div>\r\n";
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
				content = str  + content + tc.getProductsSupport();
			} else {
				content+= "<br>\r\n<p>" + messagesource.getMessage("msg.search.empty", null, locale) + "</p>\r\n";
			}
			isNoHit = true;
		}
		content = "<div class=\"p_block\">" + content + "</div>\r\n"; // <h2>の色付けに必要
		temp = StringUtils.replace(temp, "$$$content$$$", content);
		//PageProdDisconUrl
		// hilightKeyword()
		temp += "<script>kw = $(\"#kw\").text();"
				+ "  if(kw!=null&&kw!=\"\"){"
				+ "     kw = kw.replace(\"/[<>\\t\\s]/g\",\"\");\r\n" +
				"		kw = kw.replace(\"/(##s##)|(##e##)/g\",\"\");\r\n" +
				"		kw = kw.replace(\"/(###)/g\",\"\");"
				+ "     var k = kw.split(\",\"); var max=4;"
				+ "     if(k.length>0){" +
				"			for(i=0; i<k.length && i<max ;i++){"
				+ "            $(\".result\").highlight(k[i]);"
				+ "         }"
				+ "     }"
				+ "}</script>";
		if (isNoHit) {
			String head = t.getHeader();
			head = head.replaceFirst("<script src=\"/assets/js/switching.js\"></script>", ""); // 言語切り替えを外す
			ret = head + temp +  t.getFooter();
		} else {
			ret = t.getHeader() + temp + SeriesHtml._seriesCadModal + t.getFooter();
		}
		return ret;

	}

	public boolean deleteCategory(Category c1, Category c2, CategoryService service) {
		boolean ret = false;
		try {
			if (c2 != null) {
				// ディレクトリ配下のファイルを消してディレクトリを削除
				deleteFiles(htmlPath + c1.getLang()+"/"+c1.getSlug()+"/"+c2.getSlug() + "/");
			} else if (c1 != null) {
				ErrorObject err = new ErrorObject();
				Category c = service.get(c1.getParentId(), err);
				if (c.isRoot()) {
					deleteFiles(htmlPath + c1.getLang()+"/"+c1.getSlug()+"/");
				} else {
					deleteFiles(htmlPath + c1.getLang()+"/"+c.getSlug()+"/"+c1.getSlug()+"/");
				}
			}
		} catch(Exception e) {
			if (c2 != null) {
				log.error("c1 = " + c1.getId() + " c2 = " + c2.getId() + " path =" + htmlPath);
			} else {
				log.error("c1 = " + c1.getId() + " path = " + htmlPath);
			}
		}
		return ret;
	}
	public boolean deleteSeries(String lang, String c1, String c2, String seriesId) {
		boolean ret = false;
		try {
			if (c2 != null) {
				// ディレクトリ配下のファイルを消してディレクトリを削除
				deleteFiles(htmlPath + lang + "/" + c1+"/" + c2 + "/" + seriesId + "/");
			} else if (c1 != null) {
				deleteFiles(htmlPath + lang + "/" + c1+"/" + seriesId +"/");
			}
		} catch(Exception e) {
			if (c2 != null) {
				log.error("seriesId = "+ seriesId + "c1 = " + c1 + " c2 = " + c2 + " path =" + htmlPath);
			} else {
				log.error("seriesId = "+ seriesId + "c1 = " + c1 + " path = " + htmlPath);
			}
		}
		return ret;
	}
	public boolean deleteFiles(String path) {
		boolean ret = false;
		File dir = new File(path);
		File[] list = dir.listFiles();
		if (list != null) {
			for(int i=0; i<list.length; i++) {
				if (list[i] == null) {
					// IOエラー等あるかも。何もしない。
				} else if(list[i].isFile()) {
					list[i].delete();
				} else if(list[i].isDirectory()) {
					deleteFiles(path + list[i].getName() + "/");
					list[i].delete();
				}
			}
			dir.delete();
			ret = true;
		}
		return ret;
	}
	public boolean deleteCDN(String path) {
		boolean ret = false;
		return ret;
	}

	// 検索用生産終了確認
	public boolean isDisconHit(String lang, String kw) {
		boolean ret = false;
		ErrorObject err = new ErrorObject();
		if (kw != null && kw.isEmpty() == false) {
			if (kw != null && kw.isEmpty() == false) {
				ret = disconSeriesService.hitSearch(kw, lang, DiscontinuedModelState.PROD, true,  err);
			}
		}
		return ret;
	}

	// 頭文字検索用生産終了確認
	public boolean isDisconHeadHit(String lang, String h) {
		boolean ret = false;
		ErrorObject err = new ErrorObject();
		if (h != null && h.isEmpty() == false) {
			ret = disconSeriesService.hitIndexSearch(h, lang, DiscontinuedModelState.PROD, true,  err);
		}
		return ret;
	}

	/**
	 * /products/からの相対
	 * @param path
	 * @param html
	 * @return
	 * @notice エラーの場合は "" 必要に応じて対応して下さい。
	 */
	public String getFileFromHtml(String path)
	{
		String ret = "";
		BufferedWriter bw = null;
		try{
			if (path.indexOf("/") == 0) path = htmlPath + path.substring(1);
			else path = htmlPath + path;

			File file = new File(path);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String text;
			while ((text = br.readLine()) != null) {
				ret += text+"\r\n";
			}
			br.close();
        } catch(IOException e) {
            e.printStackTrace();
            log.error("path="+path);
            log.error("ERROR! "+e.getMessage());
        } finally {
            try {
                if(bw != null) {
                    bw.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
                log.error("ERROR! "+e.getMessage());
            }
        }
		return ret;
	}

	/**
	 * version切り替え
	 * @param html
	 * @param from en-jp
	 * @param to en-sg
	 * @return
	 */
	private Pattern rnPattern = Pattern.compile( "\r\n");
	private Pattern formPattern = Pattern.compile( "<form.*action=\"(.*)\"");
	private Pattern aPattern = Pattern.compile( "<a.*href=\"(.*)\"");
	private Pattern disconPattern = Pattern.compile( "submitDisconKw\\(\'(.*)\'\\);");

	public String changeLang(String html, String from, String to, String toHeader, String toFooter, boolean isDiscon) {
		String ret = "";

		log.debug("changeLang()  from="+from + " to="+to + " isDiscon="+isDiscon);

		String jsPattern = "'/"+from+"/')"; // 検索のJavascript等
		html = html.replace(jsPattern, "'/"+to+"/')");

		// <header><footer>を入れ替える。
		String s = changeHeaderAndFooter(html, toHeader, toFooter);
		if (to.indexOf("en-") == 0) {
			if (to.equals("en-jp") == false) {
				// マイリスト非表示
				s = s.replace("side_mylist", "side_mylist hide");
				s = s.replace("bt_area", "bt_area hide");
			}
		}

		ret = changeLang(s, from, to);
		if (isDiscon) ret = changeDisconLang(ret, from, to);
		return ret;
	}
	public String changeLang(String html, String from, String to ) {
		String ret = "";

		log.debug("changeLang()  from="+from + " to="+to );
	
		String jsPattern = "'/"+from+"/')"; // 検索のJavascript等
		html = html.replace(jsPattern, "'/"+to+"/')");
		String[] arr = rnPattern.split(html);
		String country = "";
		String shortLang = "en";
		if (to.indexOf("en-") == 0) country = to.replace("en-", "");
		if (to.indexOf("zh-") == 0) {
			country = to.replace("zh-", "");
			shortLang = "zh";
		}

		for(String tmp : arr) {
			Matcher m = aPattern.matcher(tmp);
			while(m.find()) {
				String str = m.group(1);
				if (str.indexOf("/catalog/") > -1) continue; // カタログ配下は対象外。pdf デジカタ
				if (str.indexOf("/products/en/urcaps/") > -1) continue; // TMComponentは対象外。
				if (str.indexOf(".jpg") > -1) continue; // <a>タグでhrefにjpg有り。
				if (str.indexOf(".pdf") > -1) continue; // <a>タグでhrefにpdf有り。
				if (str.indexOf("/customer/en/tologin.do") > -1) { // ログインはdstを付ける。
					tmp = tmp.replace("/customer/en/tologin.do", "/customer/en/tologin.do?dst=/webcatalog/en-"+country+"/");
				} else {
					String aft = str.replace("/"+from+"/", "/"+to+"/");
					aft = aft.replace("/"+shortLang+"/", "/"+shortLang+country + "/");
					tmp = tmp.replace(str, aft);
				}
			}
			Matcher m2 = formPattern.matcher(tmp);
			while(m2.find()) {
				String str = m2.group(1);
				String aft = str.replace("/"+from+"/", "/"+to+"/");
				aft = aft.replace("/"+shortLang+"/", "/"+shortLang+country + "/");
				tmp = tmp.replace(str, aft);
			}
			ret += tmp+"\r\n";
		}
		return ret;
	}
	private String changeDisconLang(String html, String from, String to) {
		String ret = "";

		String[] arr = rnPattern.split(html);
		String country = "";
		if (to.indexOf("en-") == 0) country = to.replace("en-", "");
		if (to.indexOf("zh-") == 0) country = to.replace("zh-", "");
		for(String tmp : arr) {
			Matcher m = disconPattern.matcher(tmp);
			while(m.find()) {
				String str = m.group(1);
				String aft = str.replace(from, to);
				aft = aft.replace("en-jp", "en-"+country );
				tmp = tmp.replace(str, aft);
			}
			ret += tmp+"\r\n";
		}
		return ret;
	}

	private String changeHeaderAndFooter(String html, String toHeader, String toFooter) {
		String ret = null;
		if (html != null && toHeader != null && toFooter != null) {
			String h = null;
			String f = null;
			String[] arr = toHeader.split("</header>");
			if (arr.length > 1) {
				h = arr[0]+"</header>";
			}
			arr = toFooter.split("<footer>");
			if (arr.length > 1) {
				f = "<footer>"+arr[1];
			}
			String[] tmp = html.split("</header>");
			if (tmp.length > 1) {
				tmp = tmp[1].split("<footer>");
				if (tmp.length > 1) {
					ret = h + tmp[0] + f;
				}
			}
		}
		return ret;
	}

	private String categoryStart = AppConfig.CategoryArea[1]+"\r\n";
	private String categoryEnd = "</ul>\r\n";
	public List<String> getCategoryMenu(Category category, Category c2, List<Category> cList) {
		String id = null;
		if (category != null) id = category.getId();
		String id2 = null;
		if (c2 != null) id2 = c2.getId();
		
		return getCategoryMenu(category.getLang(), id, id2, cList);
	}

	static List<String> selectGuideSlugList = new LinkedList<String>(
			Arrays.asList("directional-control-valves", "air-cylinders", "electric-actuators-cylinders", "rotary-actuators-air-grippers", "vacuum-equipment", "air-preparation-equipment",
					"switches-sensors-controllers", "fittings-and-tubing", "flow-control-equipment-speed-controllers", "process-valves", "temperature-control-equipment"));
	static List<String> selectGuideLinkList = new LinkedList<String>(
			Arrays.asList("solenoid_valve", "actuator", "e_actuator", "actuator", "vacuum", "airprep",
					"switch_sensor", "fitting", "fitting/kudou", "process", "temp"));
	/**
	 * 
	 * @param lang NotNull
	 * @param id Nullable
	 * @param id2 小カテゴリID null可
	 * @param cList
	 * @return
	 * @see cListにseriesListを設定しておくと、中カテゴリの下にシリーズが表示される。
	 */
	public List<String> getCategoryMenu(String lang, String id, String id2, List<Category> cList) {
		Category root = null;
		List<String> list = null;
		StringBuilder ret = new StringBuilder();
		ret.append(categoryStart);
		int cnt = 0;
		for(Category c : cList) {
			if (root == null && (c.getParentId() == null || c.getParentId().isEmpty()))
			{
				root = c;
				break;
			}
		}
		String parent = "";
		List<String> notActiveId = new LinkedList<String>();
		boolean isLabel = false; // 選定ガイドのラベル表示 大カテゴリの直下
		for(Category c : cList) {
			if (c.getParentId() == null || c.getParentId().isEmpty()) continue;
			else if (c.isActive() == false) {
				notActiveId.add(c.getId());
				continue;
			}
			else if (c.getParentId().equals(root.getId())) {
				if (cnt > 0) {
					ret.append("</ul>\r\n" );
					ret.append("    </li>\r\n");
					isLabel = false;
				}
				parent = c.getSlug();
				if (c.getName().indexOf("クリーン／") > -1 || c.getName().indexOf("洁净/") > -1
						|| c.getName().indexOf("Clean Series/") > -1 || c.getName().indexOf("低發塵") > -1) {
					ret.append(categoryEnd);
					list = new LinkedList<String>();
					list.add(ret.toString());
					
					ret = new StringBuilder(categoryStart);
				}
				ret.append("<li class=\"oya_1\">\r\n");
				if (id == null) {
					ret.append("<span class=\"child\">");
				} else if (c.getId().equals(id)) {
					ret.append("<span class=\"child open\">");
				} else {
					ret.append("<span class=\"child\">");
				}
				ret.append("       <a href=\"").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(c.getSlug()).append("/\" class=\"dr_link\">").append(c.getName()).append("</a>\r\n");
				ret.append(	"        </span>\r\n");
				ret.append(	"        <ul class=\"close_open\">\r\n");
				cnt++;
			} else if (notActiveId.contains(c.getParentId()) == false) {
				if (lang.equals("ja-jp")) {
					if (isLabel == false) {
						int count = 0;
						for(String slug : selectGuideSlugList) {
							if (slug.equals(parent)) {
								ret.append("<li class=\"select_guide\"><a target=\"_blank\" href=\"/products/select_guide/ja-jp/").append(selectGuideLinkList.get(count)).append("/\">選定ガイド(絞り込み)</a></li>");
								ret.append("<li class=\"oya_2\">");
								if (id2 == null) {
									ret.append( "<span class=\"child\">");
								} else if (c.getId().equals(id2)) {
									ret.append( "<span class=\"child open\">");
								} else {
									ret.append( "<span class=\"child\">");
								}
								isLabel = true;
								break;
							}
							count++;
						} 
						if (count == selectGuideSlugList.size()) {
							ret.append( "<li class=\"oya_2\">");
							if (id2 == null) {
								ret.append( "<span class=\"child\">");
							} else if (c.getId().equals(id2)) {
								ret.append( "<span class=\"child open\">");
							} else {
								ret.append( "<span class=\"child\">");
							}
						}
					} else {
						ret.append( "<li class=\"oya_2\">");
						if (id2 == null) {
							ret.append( "<span class=\"child\">");
						} else if (c.getId().equals(id2)) {
							ret.append( "<span class=\"child open\">");
						} else {
							ret.append( "<span class=\"child\">");
						}
					}
				} else {
					ret.append( "<li class=\"oya_2\">");
					if (id2 == null) {
						ret.append( "<span class=\"child\">");
					} else if (c.getId().equals(id2)) {
						ret.append( "<span class=\"child open\">");
					} else {
						ret.append( "<span class=\"child\">");
					}

				}
				ret.append( "<a href=\"").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(parent).append("/").append(c.getSlug()).append("/\">").append(c.getName()).append("</a></span>\r\n");
				// 2024/9/20 配下のシリーズすべてを表示
				ret.append( "<ul class=\"close_open \">\r\n");
				List<Series> sList = c.getSeriesList();
				if (sList != null) {
					for(Series s : sList) {
						if (s.isActive()) {
							String title = s.getNumber();
							if (title == null || title.isEmpty()) title = s.getName();
							ret.append( "<div class=\"category_series\"><a href=\"").append(AppConfig.ProdRelativeUrl).append(lang).append("/").append(parent).append("/").append(c.getSlug()).append("/").append(s.getModelNumber()).append("\">").append( title).append("</a></div>\r\n");
						}
					}
				}
				ret.append("</ul>\r\n");
				ret.append("</li>\r\n");
			}
		}
		ret.append("</ul>\r\n");
		ret.append("    </li>\r\n");
		ret.append(categoryEnd);

		if (list == null) list = new LinkedList<String>();
		list.add(ret.toString());

		return list;
	}

	/**
	 *
	 * @param lang
	 * @param basepath
	 * @param request
	 * @param root 大カテゴリの見極めに使う
	 * @param cList
	 * @return
	 */
	public String getOtherMenu(String lang, String basepath, String request, Category root, List<Category> cList) {
		String ret = "";
		ret += categoryStart;

		if (basepath.length() == 0 || basepath.lastIndexOf("/") != basepath.length()-1) { // /で終わっていなかったら/を付ける
			basepath += "/";
		}

		int cnt = 0;
		String parentSlug = "";
		List<String> notActiveId = new LinkedList<String>();
		for(Category c : cList) {
			if (c.getParentId() == null || c.getParentId().isEmpty()) continue;
			else if (c.getParentId().equals(root.getId())) {
				if (cnt > 0) {
					ret += "</ul>\r\n" +
							"    </li>";
				}
				parentSlug = c.getSlug();

				ret += "<li class=\"oya_1\">\r\n";
				if (request != null && request.indexOf(parentSlug) > -1) {
					ret += "<span class=\"child open\">";
				} else {
					ret += "<span class=\"child open\">"; // 少ないし全部Open！2026/2/24
				}
				ret += "       <a href=\""+basepath + c.getSlug()+"/\" class=\"dr_link\">"+c.getName()+"</a>\r\n";
				ret += 		"        </span>\r\n" +
						"        <ul class=\"close_open\">\r\n";
				cnt++;
			} else if (notActiveId.contains(c.getParentId()) == false) {
				if (parentSlug == null || parentSlug.isEmpty()) {
					ret += "<li><a href=\""+basepath + c.getSlug()+"/\">"+c.getName()+"</a></li>\r\n";
				} else {
					ret += "<li><a href=\""+basepath + parentSlug+"/"+c.getSlug()+"/\">"+c.getName()+"</a></li>\r\n";
				}
			}
		}
		ret += "</ul>\r\n" +
				"    </li>";
		ret += categoryEnd;

		return ret;
	}
	// SideMenuの絞り込み検索
	// 2025/11/25 GETパラメータを減らすため複数のnarrowKeyからnarrowCntへ
	public String getNarrowDown(String lang, Category category, Category c2, HttpServletRequest request) {
		String ret = "";
		if (c2 != null && c2.isNarrowdown()) {
			ErrorObject err = new ErrorObject();
			List<NarrowDownColumn> list = narrowDownService.getCategoryColumn(c2.getId(), true, err);
			if (list != null && list.size() > 0) { // 対象かどうか判定
				String title = "絞り込み条件";
				String button = "絞り込む";
				String cancel = "絞り込み条件をクリア";
				String selectEmpty = "お選びください";
				if (lang.equals("zh-tw")) {
					title = "過濾條件";
					button = "過濾";
					cancel = "清除";
					selectEmpty = "請選擇";
				} else if (lang.indexOf("zh-") > -1) {
					title = "过滤条件";
					button = "过滤";
					cancel = "清除";
					selectEmpty = "请选择";
				} else if (lang.indexOf("en-") > -1) {
					title = "Narrow Down";
					button = "Narrow search";
					cancel = "Clear";
					selectEmpty = "Please select";
				}
				ret += "<form method=\"get\" action=\""+"/webcatalog/"+category.getLang()+"/"+category.getSlug()+"/"+c2.getSlug()+"/\">\r\n";
				ret += "<h4>"+title+ "<input class=\"top_button\" type=\"submit\" value=\""+button+"\">"+"</h4>\r\n";
				ret += "<input type=\"hidden\" name=\"action\" value=\"narrowdown\">\r\n";
				ret += "<input type=\"hidden\" name=\"nCnt\" value=\""+list.size()+"\">\r\n";
				ret += "<ul class=\"narrow_down_area\">";
				int cnt = 0;
				for(NarrowDownColumn col : list) {
					String[] idxs = {};
					if (request != null) idxs = request.getParameterValues("nVal"+cnt);
					if (col.getValues() != null && col.getValues().length > 0) { // カラなら対象としない。
						ret += "<li>\r\n<span class=\"title\">"+col.getTitle() + "</span>\r\n";
						if (col.getSelect().equals("select")) {
							// 2026/3/10 ここではソートしない。DBの順番通りに。
							//String[] arr = col.getSortedValues(); // すべて数値ならソートして返してくれる。
							String[] arr = col.getValues();
							String selected = "";
							if (idxs != null && idxs.length > 0) selected = idxs[0];
//							ret+="<input type=\"hidden\" name=\"narrowKey\" value=\""+col.getTitle()+"\">";
							ret+="<select name=\"nVal"+cnt+"\" class=\"narrowdown_empty narrowdown_empty_true\">\r\n";
							ret+="<option value=''>---- "+selectEmpty+" ----</option>";
							int idx = 1;
							for(String opt : arr) {
								if (selected.isEmpty() == false && Arrays.asList(idxs).contains(String.valueOf(idx))) {
									ret += "<option value=\""+idx+"\" selected>";
								} else {
									ret += "<option value=\""+idx+"\">";
								}
								ret += opt + "</option>\r\n";
								idx++;
							}
							ret+="</select>\r\n";
						} else if (col.getSelect().equals("checkbox")) {
//							ret+="<input type=\"hidden\" name=\"narrowKey\" value=\""+col.getTitle()+"\">";
							String[] arr = col.getValues(); 
							int idx = 1;
							for(String opt : arr) {
								if (opt == null || opt.isEmpty()) continue;
								String strChk = "";
								if (idxs != null && opt.isEmpty() == false && Arrays.asList(idxs).contains(String.valueOf(idx))) strChk = "checked";
								ret += "<label class=\"control control--checkbox\"><input type=\"checkbox\" name=\"nVal"+cnt+"\" value=\""+idx+"\" "+strChk+"><div class=\"control__indicator\"></div>" + opt + "</label>\r\n";
								idx++;
							}
						} else if (col.getSelect().equals("range")) {
//							ret+="<input type=\"hidden\" name=\"narrowKey\" value=\""+col.getTitle()+"\">";
							String[] arr = col.getValues();
							if (arr.length >= 2) {
								int defaultValue = (int) Math.ceil((Integer.parseInt(arr[1]) + Integer.parseInt(arr[0])) / 2); // 中央値なので＋して2で割った数
								if (idxs != null && idxs.length > 0 ) defaultValue = Integer.parseInt(idxs[0]);
								ret += "<label>"+arr[0] + "&nbsp;<input type=\"range\" id=\""+col.getTitle()+"\" name=\"nVal"+cnt+"\" min=\""+arr[0]+"\" max=\""+arr[1]+"\" onchange=\"output_"+col.getTitle()+".value=this.value\"/>&nbsp;"+arr[1]+"&nbsp;<output name=\"output_"+col.getTitle()+"\" for=\""+col.getTitle()+"\">"+defaultValue+"</output></label>\r\n";
							}
						} else { // その他はradio
//							ret+="<input type=\"hidden\" name=\"narrowKey\" value=\""+col.getTitle()+"\">";
							String checkVal = "";
							if (idxs != null && idxs.length > 0) checkVal = idxs[0];
							String[] arr = col.getValues(); 
							int idx = 1;
							for(String opt : arr) {
								if (opt == null || opt.isEmpty()) {
									// 何もしない。
								} else {
									String strChk = "";
									if (checkVal.isEmpty() == false && checkVal.equals(String.valueOf(idx))) strChk = "checked";
									ret += "<input type=\"radio\" id=\""+col.getTitle()+"_"+opt+"\" name=\"nVal"+cnt+"\" value=\""+idx+"\" "+strChk+"><label for=\""+col.getTitle()+"_"+opt+"\">" + opt + "</label><br/>\r\n";
								}
								idx++;
							}
						}
						ret += "</li>\r\n";
					}
					cnt++;
				}
				// input type=resetだと初回のクリアは出来るが、検索後のクリアが出来ない。なので、butttonでform_clear()を読んでいる。
				ret += "<li class=\"bottom\"><input type=\"submit\" value=\""+button+"\"><br><input type=\"button\" value=\""+cancel+"\" onclick=\"form_clear();\"></li>\r\n";
				ret += "</ul>\r\n";
				ret += "</form>\r\n";
			}
			
		}
		// c2がカラならカラのまま。大カテゴリ時に$$$narrowdown$$$を消す。
		return ret;
	}
	public String getNarrowDown2026(String lang, Category category, Category c2, HttpServletRequest request) {
		String ret = "";
		if (c2 != null && c2.isNarrowdown()) {
			ErrorObject err = new ErrorObject();
			List<NarrowDownColumn> list = narrowDownService.getCategoryColumn(c2.getId(), true, err);
			if (list != null && list.size() > 0) { // 対象かどうか判定
				String title = "絞り込み条件";
				String button = "絞り込む";
				String cancel = "絞り込み条件をクリア";
				String selectEmpty = "お選びください";
				if (lang.equals("zh-tw")) {
					title = "過濾條件";
					button = "過濾";
					cancel = "清除";
					selectEmpty = "請選擇";
				} else if (lang.indexOf("zh-") > -1) {
					title = "过滤条件";
					button = "过滤";
					cancel = "清除";
					selectEmpty = "请选择";
				} else if (lang.indexOf("en-") > -1) {
					title = "Narrow Down";
					button = "Narrow search";
					cancel = "Clear";
					selectEmpty = "Please select";
				}
				ret += "<form id=\"ndForm\" method=\"get\" class=\"side_menu form\" action=\""+"/webcatalog/"+category.getLang()+"/"+category.getSlug()+"/"+c2.getSlug()+"/\">\r\n";
				ret += "<input type=\"hidden\" name=\"action\" value=\"narrowdown\">\r\n";
				ret += "<input type=\"hidden\" name=\"nCnt\" value=\""+list.size()+"\">\r\n";
				ret += "<div>\r\n"
						+ "     <div>\r\n"
						+ "        <div class=\"p16 text-primary text-lg leading-tight fw6 border-bottom bw2\"><span>"+title+"</span></div>\r\n"
						+ "      </div>\r\n";
				ret += "<ul>";
				int cnt = 0;
				for(NarrowDownColumn col : list) {
					String[] idxs = {};
					if (request != null) idxs = request.getParameterValues("nVal"+cnt);
					if (col.getValues() != null && col.getValues().length > 0) { // カラなら対象としない。
						ret +="<li class=\"border-bottom bw2\">\r\n"
								+ "        <div class=\"p16\">\r\n"
								+ "            <div class=\"text-sm leading-tight fw5 s-text-base m-text-base\">" + col.getTitle() + "</div>\r\n";
						if (col.getSelect().equals("select")) {
							ret += "            <div class=\"mt8\">";
							// 2026/3/10 ここではソートしない。DBの順番通りに。
							//String[] arr = col.getSortedValues(); // すべて数値ならソートして返してくれる。
							String[] arr = col.getValues();
							String selected = "";
							if (idxs != null && idxs.length > 0) selected = idxs[0];
							ret+="<select name=\"nVal"+cnt+"\" class=\"select\">\r\n";
							ret+="<option value='' >---- "+selectEmpty+" ----</option>";
							int idx = 1;
							for(String opt : arr) {
								if (selected.isEmpty() == false && Arrays.asList(idxs).contains(String.valueOf(idx))) {
									ret += "<option value=\""+idx+"\" selected>";
								} else {
									ret += "<option value=\""+idx+"\">";
								}
								ret += opt + "</option>\r\n";
								idx++;
							}
							ret += "</select>\r\n";
							ret += "</div>\r\n";
						} else if (col.getSelect().equals("checkbox")) {
							ret += "            <div class=\"f fclm fc gap-4 mt8\">";
							String[] arr = col.getValues(); 
							int idx = 1;
							for(String opt : arr) {
								if (opt == null || opt.isEmpty()) continue;
								String strChk = "";
								if (idxs != null && opt.isEmpty() == false && Arrays.asList(idxs).contains(String.valueOf(idx))) strChk = "checked";
								ret += "<label class=\"checkbox-container\">\r\n"
										+ "    <input class=\"checkbox-input\" type=\"checkbox\" name=\"nVal"+cnt+"\" value=\""+idx+"\" "+strChk+">\r\n"
										+ "    <div class=\"checkbox-border\">\r\n"
										+ "      <div class=\"checkbox-circle\"></div>\r\n"
										+ "    </div><span class=\"text-sm leading-tight fw5 text-base-foreground-muted\"><span class=\"text-base-foreground-default s-text-base m-text-base\">"+ opt +"</span></span>\r\n"
										+ "</label>";
								idx++;
							}
							ret += "</div>\r\n";
						} else if (col.getSelect().equals("range")) {
							ret += "            <div class=\"f fclm fc gap-4 mt8\">";
							String[] arr = col.getValues();
							if (arr.length >= 2) {
								int defaultValue = (int) Math.ceil((Integer.parseInt(arr[1]) + Integer.parseInt(arr[0])) / 2); // 中央値なので＋して2で割った数
								if (idxs != null && idxs.length > 0 ) defaultValue = Integer.parseInt(idxs[0]);
								ret += "<label>"+arr[0] + "&nbsp;<input type=\"range\" id=\""+col.getTitle()+"\" name=\"nVal"+cnt+"\" min=\""+arr[0]+"\" max=\""+arr[1]+"\" onchange=\"output_"+col.getTitle()+".value=this.value\"/>&nbsp;"+arr[1]+"&nbsp;<output name=\"output_"+col.getTitle()+"\" for=\""+col.getTitle()+"\">"+defaultValue+"</output></label>\r\n";
							}
							ret += "</div>\r\n";
						} else { // その他はradio
							ret += "            <div class=\"f fclm fc gap-4 mt8\">";
							String checkVal = "";
							if (idxs != null && idxs.length > 0) checkVal = idxs[0];
							String[] arr = col.getValues(); 
							int idx = 1;
							for(String opt : arr) {
								if (opt == null || opt.isEmpty()) {
									// 何もしない。
								} else {
									String strChk = "";
									if (checkVal.isEmpty() == false && checkVal.equals(String.valueOf(idx))) strChk = "checked";
									ret += "<label class=\"radio-container\">\r\n"
											+ "    <input class=\"radio-input sr-only\" type=\"radio\" name=\"nVal"+cnt+"\" value=\""+idx+"\" "+strChk+">\r\n"
											+ "    <div class=\"radio-border\">\r\n"
											+ "      <div class=\"radio-circle\"></div>\r\n"
											+ "    </div><span class=\"text-sm leading-tight s-text-base m-text-base\">" + opt + "</span>\r\n"
											+ "</label>";
								}
								idx++;
							}
							ret += "</div>\r\n";
						}
						ret += "</div>\r\n";
						ret += "</li>\r\n";
					}
					cnt++;
				} // for(NarrowDownColumn col : list)
				// input type=resetだと初回のクリアは出来るが、検索後のクリアが出来ない。なので、butttonでform_clear()を読んでいる。
				ret += "<div>\r\n"
						+ "    <button class=\"button large primary solid w-full mb8\" type=\"button\" onclick=\"this.form.submit();\">\r\n"
						+ "      <div class=\"f fm gap-8\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/narrow-down.svg\" alt=\"\">\r\n"
						+ "        <p class=\"text-sm text-base-foreground-on-fill leading-tight\">"+button+"</p>\r\n"
						+ "      </div>\r\n"
						+ "    </button>\r\n"
						+ "    <button class=\"button large secondary solid w-full\" type=\"button\" onclick=\"location.href=document.getElementById('ndForm').getAttribute('action');\">\r\n"
						+ "      <p class=\"text-sm text-primary leading-tight\">"+cancel+"</p>\r\n"
						+ "    </button>\r\n"
						+ "</div>";
				ret += "</ul>\r\n";
				ret += "</div>\r\n";
				ret += "</form>\r\n";
			}
			
		}
		// c2がカラならカラのまま。大カテゴリ時に$$$narrowdown$$$を消す。
		return ret;
	}

	// 検索窓下のリスト表示種別選択
	private String[] viewListJA = {"一覧", "写真", "仕様比較"};
	private final String[] viewListE = {"List", "Picture", "Specification comparison"};
	private final String[] viewListCN = {"列表", "照片", "规格对比"};
	private final String[] viewListTW = {"清單", "照片", "規格對比"};
	public String getListDisplaySelection(String lang, Category c2, String view, String action, int ndCnt, HttpServletRequest request) {
		String ret = "";
		String viewParam = "";
		{
			String[] viewList = viewListJA;
			if (lang.indexOf("en-") > -1) {
				viewList = viewListE;
			} else if (lang.equals("zh-tw")) {
				viewList = viewListTW;
			} else if (lang.indexOf("zh-") > -1) {
				viewList = viewListCN;
			}
			if (action != null ) {
				viewParam += "&action="+action;
			}
			if (ndCnt > 0) {
				viewParam+="&nCnt="+ndCnt;
				for(int cnt = 0; cnt < ndCnt; cnt++) {
					String[] arr = request.getParameterValues("nVal"+cnt);
					if (arr != null && arr.length > 0) {
						for(String tmp : arr) {
							if (tmp != null && tmp.isEmpty() == false) viewParam+="&nVal"+cnt+"="+tmp;
						}
					}
				}
			}
			if (view == null) view = "list";
			if (view.equals("compare") && c2 != null && c2.isCompare() == false) view = "picture";
			ret+="<div class=\"viewType\">";
			ret+="<ul>\r\n";
			if (view.equals("list")) {
				ret+="<li class=\"list active\">";
			} else {
				ret+="<li class=\"list\">";
			}
			ret+="<a class=\"narrow_down_list\" href=\"?view=list"+viewParam+"\"><img src=\"/assets/smc_img/web_cata/narrowdown/list.png\">"+viewList[0]+"</a></li>";
			if (view.equals("picture")) {
				if (c2.isCompare()) {
					ret+="<li class=\"picture active\">";
				} else {
					ret+="<li class=\"active\">"; // 後ろの線を消す
				}
			} else {
				if (c2.isCompare()) {
					ret+="<li class=\"picture\">";
				} else {
					ret+="<li class=\"\">"; // 後ろの線を消す
				}
			}
			ret+="<a class=\"narrow_down_picture\" href=\"?view=picture"+viewParam+"\"><img src=\"/assets/smc_img/web_cata/narrowdown/picture.png\">"+viewList[1]+"</a></li>";
			// 仕様比較が有効な場合のみ表示
			if (c2.isCompare()) {
				if (view.equals("compare")) {
					ret+="<li class=\"compare active\">";
				} else {
					ret+="<li class=\"compare\">";
				}
				ret+="<a class=\"narrow_down_compare\" href=\"?view=compare"+viewParam+"\"><img src=\"/assets/smc_img/web_cata/narrowdown/compare.png\">"+viewList[2]+"</a></li>";
			}
			ret+="</ul>\r\n";
			ret+="</div>\r\n";
		}

		return ret;
	}
	// 表示切り替えのHTML
	public String getListDisplaySelection2026(String lang, Category c2, String view, String action, int ndCnt, HttpServletRequest request) {
		String ret = "<div class=\"f fr gap-8 p8 mb24 s-mb36 m-mb36\">";
		String viewParam = "";
		{
			String[] viewList = AppConfig.ViewTitleList_ja;;
			if (lang.indexOf("en-") > -1) {
				viewList = AppConfig.ViewTitleList_en;
			} else if (lang.equals("zh-tw")) {
				viewList = AppConfig.ViewTitleList_tw;
			} else if (lang.indexOf("zh-") > -1) {
				viewList = AppConfig.ViewTitleList_zh;
			}
			if (action != null ) {
				viewParam += "&action="+action;
			}
			if (ndCnt > 0) {
				viewParam+="&nCnt="+ndCnt;
				for(int cnt = 0; cnt < ndCnt; cnt++) {
					String[] arr = request.getParameterValues("nVal"+cnt);
					if (arr != null && arr.length > 0) {
						for(String tmp : arr) {
							if (tmp != null && tmp.isEmpty() == false) viewParam+="&nVal"+cnt+"="+tmp;
						}
					}
				}
			}
			if (view == null) view = "picture";
			if (view.equals("compare") && c2 != null && c2.isCompare() == false) view = "picture";
			
			String pic = "<button class=\"button small secondary solid gap-8\" type=\"button\" onclick=\"location.href='?view=picture"+viewParam+"'\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/grid.svg\" alt=\"\"><span class=\"text-sm text-primary leading-tight\">"+viewList[1]+"</span></button>\r\n";
			String list = "<button class=\"button small secondary solid gap-8\" type=\"button\" onclick=\"location.href='?view=list"+viewParam+"'\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/list.svg\" alt=\"\"><span class=\"text-sm text-primary leading-tight\">"+viewList[0]+"</span></button>\r\n";
			String comp = "<button class=\"button small secondary solid gap-8\" type=\"button\" onclick=\"location.href='?view=compare"+viewParam+"'\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/table.svg\" alt=\"\">\r\n"
							+ "                  <div class=\"text-sm text-primary leading-tight\">"+viewList[2]+"</div>\r\n"
							+ "                </button>";

			if (view.equals("compare")) {
				comp = comp.replace("table.svg", "table-primary.svg");
				comp = comp.replace(" secondary ", " primary ");
				comp = comp.replace(" text-primary ", " text-base-foreground-on-fill ");
			} else if (view.equals("list")) {
				list = list.replace("list.svg", "list-primary.svg");
				list = list.replace(" secondary ", " primary ");
				list = list.replace(" text-primary ", " text-base-foreground-on-fill ");
			} else {
				pic = pic.replace("grid.svg", "grid-primary.svg");
				pic = pic.replace(" secondary ", " primary ");
				pic = pic.replace(" text-primary ", " text-base-foreground-on-fill ");
			}
			ret += pic + list;
			if (c2.isCompare()) {
				ret += comp;
				if (view != null && view.equals("compare")) {
					ret = ret.replace("f fr ", "f fr w90per s-w100per m-w100per text-right ");
				}
			}
		}
		ret += "</div>\r\n";
		return ret;
	}
	/**
	 * 
	 * @param lang NotNull
	 * @param selectedId Nullable
	 * @param selectedId2 小カテゴリID null可
	 * @param cList
	 * @return
	 * @see cListにseriesListを設定しておくと、中カテゴリの下にシリーズが表示される。
	 */
	public List<String> getCategoryMenu2026(String lang, String selectedId, String selectedId2, List<Category> cList) {
		Category root = null;
		List<String> list = null;
		StringBuilder ret = new StringBuilder();
		ret.append("<ul>\r\n");
		for(Category c : cList) {
			if (root == null && (c.getParentId() == null || c.getParentId().isEmpty()))
			{
				root = c;
				break;
			}
		}
		int cnt = 0;
		String parentSlug = "";
		List<String> notActiveId = new LinkedList<String>(); // notActiveを非表示
		boolean isLabel = false; // 選定ガイドのラベル表示 大カテゴリの直下
		boolean isCategory2 = false; 
		for(Category c : cList) {
			if (c.getParentId() == null || c.getParentId().isEmpty()) continue;
			else if (c.isActive() == false) {
				notActiveId.add(c.getId());
				continue;
			}
			else if (c.getParentId().equals(root.getId())) { // 大カテゴリ
				if (isCategory2 == true) {
					isCategory2 = false; 
					ret.append("</ul>\r\n")
						.append( "</div>\r\n")
						.append( "</div>\r\n");
				}
				if (cnt > 0) {
					ret.append("</details>\r\n");
					ret.append("</li>\r\n");
					isLabel = false;
				}
				parentSlug = c.getSlug();
				// テーマ・業種別カタログ
				if (c.getName().indexOf("クリーン／") > -1 || c.getName().indexOf("洁净/") > -1
						|| c.getName().indexOf("Clean Series/") > -1 || c.getName().indexOf("低發塵") > -1) {
					ret.append("</details>\r\n");
					ret.append("</li>\r\n");
					ret.append("</ul>\r\n");
					list = new LinkedList<String>();
					list.add(ret.toString());
					
					ret = new StringBuilder(); // クリーン／で初期化
					ret.append("<ul>\r\n");
				}
				ret.append("<li>\r\n");
				if (selectedId == null) {
					ret.append("<details class=\"accordion accordion--top has-children\">");
				} else if (c.getId().equals(selectedId)) {
					if (selectedId2 != null && selectedId2.isEmpty() == false) {
						ret.append("<details class=\"accordion accordion--top has-children\" open=\"\">");
					} else {
						ret.append("<details class=\"accordion accordion--top has-children is-current\" open=\"\">");
					}
				} else {
					ret.append("<details class=\"accordion accordion--top has-children\">");
				}
				ret.append("<summary class=\"accordion-title level-1\">")
					.append("  <span onclick=\"location.href='"+AppConfig.ProdRelativeUrl+lang+"/"+c.getSlug()+"/';\">"+c.getName()+"</span>")
					.append("  <img class=\"accordion-icon accordion-icon--toggle object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\">\r\n")
					.append( "</summary>\r\n");
				
				cnt++;
			} else if (notActiveId.contains(c.getParentId()) == false) {
				if (isCategory2 == false) {
					isCategory2 = true; 
					ret.append("<div>\r\n")
						.append( "      <div class=\"accordion-content\">\r\n")
						.append( "              <ul>\r\n");
				}
				ret.append("<li>\r\n");
				if (selectedId2 == null) {
					ret.append("<details class=\"accordion has-children\">");
				} else if (c.getId().equals(selectedId2)) {
					ret.append("<details class=\"accordion has-children is-current\" open=\"\">");
				} else {
					ret.append("<details class=\"accordion has-children\">");
				}

				ret.append("<summary class=\"accordion-title level-2\">")
					.append( "  <span onclick=\"location.href='"+AppConfig.ProdRelativeUrl+lang+"/"+parentSlug+"/"+c.getSlug()+"/';\">"+c.getName()+"</span><img class=\"accordion-icon accordion-icon--toggle object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\">\r\n")
					.append( "</summary>\r\n");
				// 配下のシリーズすべて
				List<Series> sList = c.getSeriesList();
				if (sList != null) {
					ret.append("<div>\r\n")
						.append("  <div class=\"accordion-content\">\r\n")
						.append("    <ul>");
					for(Series s : sList) {
						if (s.isActive()) {
							String title = s.getNumber();
							if (title == null || title.isEmpty()) title = s.getName();
							ret.append("<li>\r\n")
								.append( "      <details class=\"accordion\">\r\n")
								.append( "        <summary class=\"accordion-title level-3\"><span onclick=\"location.href='"+AppConfig.ProdRelativeUrl+lang+"/"+parentSlug+"/"+c.getSlug()+"/"+s.getModelNumber()+"';\">"+ title+"</span>"+"<img class=\"accordion-icon accordion-icon--right object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\">\r\n")
								.append( "        </summary>\r\n")
								.append( "        <div>\r\n")
								.append( "          <div class=\"accordion-content\">\r\n")
								.append( "          </div>\r\n")
								.append( "        </div>\r\n")
								.append( "      </details>\r\n")
								.append( "</li>");
						}
					}
					ret.append("</ul>\r\n");
					ret.append("</div>\r\n");
					ret.append("</div>\r\n");
				}
				ret.append("</details>\r\n");
				ret.append("</li>\r\n");
			}
		} //for(Category c : cList) {
		ret.append("</details>\r\n");
		ret.append("</li>\r\n");
		ret.append("</ul>\r\n");

		if (list == null) list = new LinkedList<String>();
		list.add(ret.toString());

		return list;
	}
	public String getCatpan2026(String lang, String catpanTemplate, List<String> titleList, List<String> slugList) {
		StringBuilder catpan = new StringBuilder();
		StringBuilder link = new StringBuilder("/webcatalog/"+lang+"/");
		int cnt = 0;
		for(String title : titleList) {
			link.append(slugList.get(cnt) + "/");
			catpan.append("<li class=\"breadcrumb-separator\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/slash.svg\" alt=\"\"></li>\r\n")
					.append("<li>\r\n")
					.append("  <a class=\"breadcrumb-item\" href=\"").append(link).append("\">").append(title).append("</a>\r\n")
					.append( "</li>\r\n");
			cnt++;
		}
		
		return catpanTemplate.replace("$$$catpan_title$$$", catpan);
	}
	public String getH1box2026(String h1box, String title) {
		String ret = h1box;
		String head = title.substring(0, 1);
		String aft = title.substring(1);
		ret = ret.replace("$$$title21$$$", head);
		ret = ret.replace("$$$title22$$$", aft);
		return ret;
	}
	/**
	 *
	 * @param lang
	 * @param basepath
	 * @param request
	 * @param root 大カテゴリの見極めに使う
	 * @param cList
	 * @return
	 */
	public String getOtherMenu2026(String lang, String basepath, String request, Category root, List<Category> cList) {
		String ret = "";
		ret += "<div>\r\n"
			+ "  <ul>\r\n";

		if (basepath.length() == 0 || basepath.lastIndexOf("/") != basepath.length()-1) { // /で終わっていなかったら/を付ける
			basepath += "/";
		}

		int cnt = 0;
		String parentSlug = "";
		String parentId = "";
		for(Category c : cList) {
			if (c.getParentId() == null || c.getParentId().isEmpty()) continue;
			else if (c.getParentId().equals(root.getId())) {
				if (cnt > 0) {
					ret += "    </ul>\r\n"
						 + "  </div>\r\n"
						 + "</div>\r\n";
					ret += "  </details>\r\n";
					ret += "</li>\r\n";
				}
				parentSlug = c.getSlug();
				parentId = c.getId();

				ret += "    <li>\r\n";
				if (request != null && request.indexOf(parentSlug) > -1) {
					ret += "<details class=\"accordion accordion--top has-children\" open>\r\n";
					ret += "<summary class=\"accordion-title level-1\">"
							+ "  <span onclick=\"location.href='"+basepath + c.getSlug()+"';\">"+c.getName()+"</span><img class=\"accordion-icon accordion-icon--toggle object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\" title=\"\">\r\n"
							+ "</summary>";
				} else {
					ret += "<details class=\"accordion accordion--top has-children\" open>\r\n"; // 少ないし全部Open！2026/2/24
					ret += "<summary class=\"accordion-title level-1\">"
							+ "  <span onclick=\"location.href='"+basepath + c.getSlug()+"';\">"+c.getName()+"</span><img class=\"accordion-icon accordion-icon--toggle object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\" title=\"\">\r\n"
							+ "</summary>";
				}
				ret += "  <div>\r\n"
						+ "      <div class=\"accordion-content\">\r\n"
						+ "              <ul>\r\n";
				cnt++;
			} else if (c.getParentId().equals(parentId)) {
				ret += "<li>\r\n"
					 + "  <details class=\"accordion has-children\">"
					 + "    <summary class=\"accordion-title level-2\">"
					 + "      <span onclick=\"location.href='"+basepath + parentSlug+ '/' + c.getSlug()+"';\">"+c.getName()+"</span><img class=\"accordion-icon accordion-icon--toggle object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" title=\"\">\r\n"
					 + "    </summary>\r\n"
					 + "  </details>\r\n"
					 + "</li>";
			}
		}
		ret += "      </details>\r\n";
		ret += "    </li>\r\n" +
				"  </ul>";
		ret += "</div>\r\n";

		return ret;
	}
	public String getSearchResultTitle(String lang) {
		String title = "検索結果";
		if (lang.indexOf("en-") > -1) {
			title = "Search result";
		} else if (lang.indexOf("zh-") > -1) {
			title = "搜索结果";
		}
		return title;
	}
	public Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		if (lang.equals("zh-tw")) loc = Locale.TRADITIONAL_CHINESE;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}

}