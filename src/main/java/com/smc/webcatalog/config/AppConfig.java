package com.smc.webcatalog.config;

import org.springframework.context.annotation.Configuration;


@Configuration
public class AppConfig  {

	// 2024/9/23 サーバー移転に伴い、http://ap2.smcworld.com はアクセス不可に。PageCDNUrlを利用。
	//public static String PageTestUrl = "http://ap2.smcworld.com";
	//public static String PageTestIdUrl = PageTestUrl + "/page.jsp?id=";

	public static String PageProdUrl = "https://www.smcworld.com";
	public static String PageProdUrlZH = "https://www.smc.com.cn";
	public static String PageCDNUrl = "https://cdn.smcworld.com";
	public static String PageCDNIdUrl = PageCDNUrl + "/page.jsp?id=";
	
	public static String BasicAuthCDNID = "dev1user";
	public static String BasicAuthCDNPW = "AL9db9au?";

	public static String ContextPath = "/webcatalog";
	public static String ProductsPath = "/products";
	public static String FaqPath = "/faq";

	public static String CDNClearUrl = "https://gslb.smcworld.com/proxy2/api/v1/deletecache";
	public static String CDNClearID = "cdncache";
	public static String CDNClearPW = "vHFQSTt3pFk3dv0RkA9XHRxrP3vs251F";
	
	public static String CDNCacheUrl = PageProdUrl;
//	public static String CDNCacheUrl = "https://www-tmp.smcworld.com";// for TEST

//	public static String Page2DCADUrl = "/products/ja/get.do?type=WIN2D&id=";
	public static String Page2DCADUrl = PageProdUrl + ContextPath+"/2dcad/ja-jp/"; // productRestController
	public static String Page2DCADUrlZH = PageProdUrlZH + ContextPath + "/2dcad/zh-cn/"; // productRestController
	// こちらに内包。2023/3/25
//	public static String PageProdCustomUrl = PageProdUrl + ProductsPath + "/ja/custom.do?result=CUSTOM&id=";
//	public static String PageProdOrderMadeUrl = PageProdUrl + ProductsPath + "/ja/custom.do?result=OM&id=";
	public static String PageProdManifoldUrl = PageProdUrl + "/specs/manifold/ja/s.do";
	public static String PageProdManualUrl = PageProdUrl + "/qssearchex.jsp?id=3899&searchresult=3900&indexname=C16man01j&lang=ja-jp&mode=ws&search=";
	public static String PageProdManualUrlIndex = PageProdUrl + "/qssearchex.jsp?id=3899&searchresult=3900&indexname=C16man01j&lang=ja-jp&mode=as&search=";
//	public static String PageProdCeUrl = PageProdUrl + "/overseas/international/ja-jp/ce/index.html?dec=";
	public static String PageProdCeUrl = PageProdUrl +"/overseas/international/ja-jp/ce/index.html";
	public static String PageProdDisconUrl = PageProdUrl + ContextPath +"/discontinued/searchKeyword/";
	public static String PageProdDisconHeadUrl = PageProdUrl + ContextPath+ "/discontinued/searchIndex/"; // 大文字

	public static String PageProdManualUrlE = PageProdUrl + "/qssearchex.jsp?id=5082&searchresult=3900&indexname=C16man01e&lang=en-jp&mode=ws&search=";
	public static String PageProdManualUrlIndexE = PageProdUrl + "/qssearchex.jsp?id=5082&searchresult=3900&indexname=C16man01e&lang=en-jp&mode=as&search=";

	public static String ImageProdUrl = PageProdUrl + "/upfiles/etc/series/";
	public static String ImageProdPath = "/upfiles/etc/series/";
	public static String ImageTestPath = "C:\\workspace\\smc-webcatalog\\src\\main\\resources\\static\\images\\";

	public static String ProdRelativeUrl = ContextPath + "/";
	public static String HtmlProdUrl = PageProdUrl + ContextPath + "/";

	public static String BasicAuthID = "dev1user";
	public static String BasicAuthPW = "AL9db9au?";

	// テンプレート取得用2026 TemplateDivは不要。<main </main>で分割
	public static String CatpanArea2026 = "<!-- catpan -->";
	public static String SidebarArea2026 = "<!-- sidebar -->";
	public static String CategoryArea2026 = "<!-- category -->";
	public static String NarrowDownArea2026 = "<!-- narrowdown -->";
	public static String FormboxArea2026 = "<!-- formbox -->";
	public static String H1boxArea2026 = "<!-- h1box -->";
	public static String ContentArea2026 = "<!-- content -->";
	
	public static String[] ViewTitleList_ja = {"一覧", "画像", "仕様比較"};
	public static String[] ViewTitleList_en = {"List", "Picture", "Specification comparison"};
	public static String[] ViewTitleList_zh = {"列表", "照片", "规格对比"};
	public static String[] ViewTitleList_tw = {"清單", "照片", "規格對比"};
	
	// テンプレート取得用
	public static String[] TemplateDiv = new String[] {"<div id=\"content\">", "<p id=\"pageTop\" style=\"display:block;\">"};
	public static String[] CatpanArea = new String[] {"<div class=\"catpan\">", "</div>"};
	public static String[] SidebarArea = new String[] {"<div id=\"side_bar\" class=\"side_nav\">", "</div><!--side_bar-->"};
	public static String[] CategoryArea = new String[] {"<h4>", "<ul class=\"open_close side_menu\">", "\r\n</div><!--side_menu-->\r\n</div><!--side_bar-->"}; // <h4>を残すため３つ
	public static String[] FormboxArea = new String[] {"<div class=\"form_box\">", "</form>\r\n</div>"};
	public static String[] H1boxArea = new String[] {"<div class=\"h1_box\">", "</div>\r\n</div><!--h1_box-->"};
	public static String[] ContentArea = new String[] {"<div class=\"cont_inner\">", "</div><!--cont_inner-->"};
	public static String[] DispItemTitleArr = new String[] {
			"種類", "型式", "リリーフ機構", "最大冷却能力", "形式", // ja-jp
			"シリーズ", "サイズ", "ボディサイズ", "作動方式", "適用チューブ外径",
			"駆動方式", "タイプ", "チューブ内径 （mm）", "ノズル径<br/>(mm)", "エジェクタシリーズ",
			"Series","Type","size","Body size","Action",  // en-jp
			"Applicable tubing O.D.","Model","Relief mechanism","Cooling capacity","Bore size (mm)",
			"Nozzle diameter<br>(mm)","Ejector series",
			"系列","形式","尺寸","主体尺寸","动作", // zh-cn
			"适合管的（外径）","系列","溢流机构","冷却能力","管子内径(mm)",
			"喷嘴口径(mm)","真空发生器系列",
			"2021/11/19現状はen", // zh-tw
			};
	/**
	 * psItem検索結果のタイトル
	 */
	public 	static String[][] PsItemSearchResultTableTh = {{"分類", "名称", "シリーズ", "型式"}, {"Category", "Product name", "Series", "Type"}, {"分类", "名称", "系列", "型式"}};

	public static String JingSocialInc = "<script src=\"https://appcdn.jingsocial.com/js/trak-asyn_v4.2.2.js?v=20210907\"></script>";
	public static String JingSocial = "<script>trak.custom_event (\"SearchProduct\",{\"keyword\":\"$XXX$\",\"search_result\":$YYY$,\"current_page\":\"homepage\",});</script>";
	public static String[] LinkJSONHead = new String[] {"[2DCAD]","[Manual]","[Manifold]", "[DoC]"}; // Seriesのedit.htmlにもある。要同期
	public static String CookieListState = "WebCatalogListState";
}
