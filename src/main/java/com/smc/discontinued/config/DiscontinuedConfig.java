package com.smc.discontinued.config;

import org.springframework.context.annotation.Configuration;

import com.smc.webcatalog.config.AppConfig;

@Configuration
public class DiscontinuedConfig {

	public static String oldImageUrl = "/discon/prodimg/";
	public static String oldPDFUrl = "/discon/ja/oldpdf/";
	public static String oldCompareUrl = "/discon/ja/compare/";
	public static String webCatalogUrl = AppConfig.ProdRelativeUrl+"api/ja-jp/guide/";

	public static String[] SidebarArea = new String[] {"<ul class=\"open_close side_menu\">", "</ul><!--side_menu-->"};

	public static String contentStart = "<div class=\"cont_inner\">\r\n" +
			"<h2>$$$title$$$</h2>\r\n" +
			"\r\n" +
			"<div class=\"js-scrollable mobile-scroll scroll-hint\" style=\"position: relative; overflow: auto;\">\r\n" +
			"\r\n" +
			"<div class=\"links_on_table\">\r\n" +
			"\r\n" +
			"\r\n" +
			"<div class=\"prod\" id=\"\">\r\n" +
//			"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl table_stripe table_fixed\" style=\"table-layout:auto;\">\r\n" +
			"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"table_stripe table_fixed\" style=\"table-layout:auto;\">\r\n" +
			"<tbody><tr>\r\n" +
			"<th colspan=\"3\" class=\"ul\">$$$endtitle$$$</th>\r\n" +
			"<th colspan=\"2\" class=\"ul last\">$$$replacetitle$$$</th>\r\n" +
			"</tr>\r\n" +
			"<tr>\r\n" +
			"<th width=\"35%\">$$$colname$$$</th>\r\n" +
			"<th width=\"20%\">$$$colseries$$$</th>\r\n" +
			"<th width=\"15%\">$$$coldate$$$</th>\r\n" +
			"<th width=\"19%\">$$$colseries$$$</th>\r\n" +
			"<th width=\"11%\" class=\"last\">$$$colcompair$$$</th>\r\n" +
			"</tr>\r\n" +
			"\r\n";
	public static String contentEnd = "</table>\r\n" +
			"</div><!--prod-->\r\n" +
			"\r\n" +
			"</div><!--js-scrollable mobile-scroll-->\r\n" +
			"</div><!--links_on_table-->\r\n" +
			"\r\n" +
			"</div><!--cont_inner-->\r\n";
	public static String contentSearch = "<script>function submitDisconKw(lang){\r\n" +
			"  location.href=location.protocol+\"//\"+location.host+\""+AppConfig.ProdRelativeUrl+"discontinued/searchKeyword/\"+lang+\"/?kw=\"+document.getElementById('k').value;\r\n" +
			"}</script>";
	public static String detailStart = "<div class=\"cont_inner\">\r\n" +
			"<h2>$$$title$$$</h2>\r\n" +
			"\r\n" +
			"<div class=\"js-scrollable mobile-scroll scroll-hint\" style=\"position: relative; overflow: auto;\">\r\n" +
			"\r\n" +
			"<div class=\"links_on_table\">\r\n" +
			"<h3>$$$name$$$&nbsp;$$$series$$$</h3>" +
			"\r\n" +
			"<div class=\"prod\" id=\"\">\r\n" +
			"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\" table_fixed\" style=\"table-layout:auto;\">\r\n" +
			"<tbody>\r\n";
}
