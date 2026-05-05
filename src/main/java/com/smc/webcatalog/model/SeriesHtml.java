package com.smc.webcatalog.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.smc.omlist.model.Omlist;
import com.smc.omlist.service.OmlistServiceImpl;
import com.smc.webcatalog.config.AppConfig;
import com.smc.webcatalog.dao.SeriesFaqRepository;
import com.smc.webcatalog.util.LibOkHttpClient;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SeriesHtml {

    private MessageSource messagesource;
    private OmlistServiceImpl omlistService;
    private SeriesFaqRepository faqRepo;
    private Map<String, String> _manualHtmlMap = null;
    private Map<String, String> _manualHtmlMapE = null;
    private Map<String, List<String>> _ceSeriesMap = null;
    private Map<String, List<String>> _ceSeriesMapE = null;
    private Map<String, List<String>> _ceSeriesMapZH = null;
    private Map<String, List<String>> _ceSeriesFileMap = null;
    private Map<String, List<String>> _ceSeriesFileMapE = null;
    private Map<String, List<String>> _ceSeriesFileMapZH = null;

	private Locale _locale = Locale.JAPANESE;

	public SeriesHtml() {

	}

	public SeriesHtml(Locale loc, MessageSource msg, OmlistServiceImpl om, SeriesFaqRepository fr) {
		_locale = loc;
		messagesource = msg;
		omlistService = om;
		faqRepo = fr;
	}

	// リスト表示用。
	// 取説、自己宣言書はHeartCore。
	public static String _seriesListTemplate = "<!--list01-->\r\n" +
			"<div class=\"result\">\r\n" +
			"<h2>$$$title$$$</h2>\r\n" +
			"$$$notice$$$\r\n" +
			"    \r\n" +
			"<div class=\"pro_details\">\r\n" +
			"<div class=\"pro_left\">\r\n" +
			"    $$$imageTop$$$\r\n" +
			"    $$$advantage$$$\r\n" +
			"    $$$imageBottom$$$\r\n" +
			"</div>\r\n" +
			"<div class=\"pro_right\">\r\n" +
			"\r\n" +
			"<div class=\"bt_area\">\r\n"+
			"$$$rohs$$$\r\n" +
			"$$$mylist$$$\r\n" +
			"</div>\r\n" +
			"    \r\n" +
			"<ul class=\"pro_details_text\">\r\n" +
			"    $$$details$$$\r\n" +
			"</ul>\r\n" +
			"\r\n" +
			"<ul class=\"pro_service_bt\">\r\n"+

			"$$$button$$$"+

			"</ul>\r\n" +
			"\r\n" +
			"</div>\r\n" +
			"</div><!--pro_details-->\r\n" +
			"\r\n" +
			"\r\n" +

			"$$$tabs$$$"+

			"$$$other$$$\r\n" +
			"\r\n" +
			"\r\n" +

			"$$$spec$$$" +
			"\r\n" +

			"</div><!--result-->\r\n" +
			"<!--/list01-->";
	// リスト表示用。
	// 取説、自己宣言書はHeartCore。
	public static String _seriesListTemplate2026 = "<div class=\"w-full js-tab-container mb24\" id=\"product-card-999\">\r\n" +
			"<div class=\"f fm fbw gap-8 mb48 s-mb36 m-mb36 l-pr16\">\r\n" +
			"     <div class=\"f ft gap-8\">\r\n" +
			"          <div class=\"f fb h24 flex-fixed s-fm m-fm\"><img class=\"s20 object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" aria-hidden=\"true\"></div>\r\n"+ 
			"          <div><a class=\"leading-tight text-primary text-2xl fw6 hover-primary-underline m-text-base s-text-base\" href=\"$$$titleLink$$$\">$$$title$$$</a></div>\r\n"+ 
			"     </div>\r\n"+ 
			"     $$$mylist$$$\r\n"+ 
			"</div>" +
			"$$$notice$$$\r\n" +
			"<div class=\"f w-full s-fclm m-fclm gap-48 mb48 s-mb36 m-mb36\">\r\n"+ 
			"     <div class=\"f fclm fc\">"+
			"       $$$imageTop$$$\r\n"+
			"       <div class=\"relative f fc\">\r\n"+ 
			"         $$$image$$$\r\n"+
			"       </div>\r\n"+
			"       $$$advantageButton$$$"+
			"       $$$imageBottom$$$"+
			"     </div>\r\n"+
			"     <div class=\"f fclm w-full\">\r\n"+
			"          <ul class=\"li-marker mb40 text-sm\">\r\n"+
			"            $$$details$$$\r\n" +
			"          </ul>\r\n" +
			"          $$$isLoginButton$$$\r\n" +
			"          <div class=\"w-full h1 bg-base-stroke-default my24\"></div>\r\n"+ 
			"          <div class=\"w-full\">\r\n"+ 
			"               <div class=\"text-sm leading-tight fw5 mb16\">$$$buttonTitle$$$</div>\r\n"+ 
			"               <div class=\"g gap-8 w-full s-gcol1 m-gcol1 tools-grid_w\">\r\n"+
			"                 $$$button$$$\r\n"+
			"               </div>\r\n" +
			"          </div>\r\n" +
			"     </div>\r\n" +
			"</div>\r\n" +
			"<div class=\"g\">\r\n"+ 
			"     <div class=\"mb24\">"+
			"         $$$other$$$\r\n" +
			"     </div>\r\n" +
			"     <div class=\"overflow-auto border-bottom\">\r\n"+ 
			"          <div class=\"min-w-500\">\r\n"+ 
			"               <div class=\"f\">"+
			"                  $$$tabs_title$$$"+
			"               </div>\r\n" +
			"          </div>\r\n" +
			"     </div>\r\n" +
			"     <div class=\"overflow-auto\">\r\n"+ 
			"          <div class=\"mt20\">"+
			"               $$$tabs$$$"+
			"          </div>\r\n" +
			"     </div>\r\n" +
			"</div>\r\n" +

			"</div><!--#product-card-->\r\n";
	// 全体表示用
	// 上記List + $$$cate2$$$ $$$advantageBody$$$ $$$backUrl$$$
	public static String _seriesTemplate = "<div class=\"h1_detail\">\r\n" +
			"<p>$$$cate2$$$</p>\r\n" +
			"<h1>$$$title$$$</h1>\r\n" +
			"</div>"+
			"<div class=\"cont_inner\">\r\n" +
			"\r\n" +
			"<div class=\"p_block\">" +
			"$$$notice$$$\r\n" +
			"    \r\n" +
			"<div class=\"pro_details\">\r\n" +
			"<div class=\"pro_left\">\r\n" +
			"    $$$imageTop$$$\r\n" +
			"    $$$advantage$$$\r\n" +
			"    $$$imageBottom$$$\r\n" +
			"</div>\r\n" +
			"<div class=\"pro_right\">\r\n" +
			"\r\n" +
			"<div class=\"bt_area\">\r\n"+
			"$$$rohs$$$\r\n" +
			"$$$mylist$$$\r\n" +
			"</div>\r\n" +
			"    \r\n" +
			"<ul class=\"pro_details_text\">\r\n" +
			"    $$$details$$$\r\n" +
			"</ul>\r\n" +
			"\r\n" +
			"<ul class=\"pro_service_bt\">\r\n"+

			"$$$button$$$"+

			"</ul>\r\n" +
			"\r\n" +
			"</div>\r\n" +
			"</div><!--pro_details-->\r\n" +
			"\r\n" +
			"\r\n" +

			"$$$tabs$$$" +
			"\r\n" +
			"\r\n" +
			"$$$other$$$\r\n" +
			"\r\n" +
			"\r\n" +

			"$$$spec$$$" +
			"\r\n" +
			"<div id=\"gnreco-detail\"></div>\r\n" +
			"<a name=\"detail\"></a>\r\n" +
			"<div id=\"detail\" class=\"features\">\r\n" +
			"$$$advantageTitle$$$" +
			"$$$advantageBody$$$" +
			"\r\n" +
			"\r\n" +
			"</div><!--detail-->\r\n" +
			"\r\n" +
			"</div><!--p_block-->\r\n" +
			"\r\n" +
			"</div><!--cont_inner-->"
			+ "$$$backUrl$$$\r\n";
	// 全体表示用 2026
	// 上記List + $$$cate2$$$ $$$advantageBody$$$ $$$backUrl$$$ + "<div id=\"gnreco-detail\"></div>
	public static String _seriesTemplate2026 = "<div class=\"w-full\">\r\n"
			+ "            <h2 class=\"text-6xl leading-tight fw5 s-fw6 s-text-3xl m-fw6 m-text-3xl\"><span class=\"text-primary\">$$$title21$$$</span><span class=\"text-base-foreground-default\">$$$title22$$$</span></h2>\r\n"
			+ "            <div class=\"w-full mt80 js-tab-container\">\r\n"
			+ "                          $$$notice$$$\r\n"
			+ "                          <div class=\"w-full js-tab-container\" id=\"product-card-999\">\r\n"
			+ "                            <div class=\"f w-full s-fclm m-fclm gap-36 mb48\">\r\n"
			+ "                              <div class=\"f fclm fc\">\r\n"
			+ "                                $$$imageTop$$$\r\n"
			+ "                                <div class=\"relative f fc\">\r\n"
			+ "                                  $$$image$$$\r\n"
			+ "                                  $$$mylist$$$\r\n"
			+ "                                </div>\r\n"
			+ "                                $$$imageBottom$$$\r\n"
			+ "                              </div>\r\n"
			+ "                              \r\n"
			+ "                              <div class=\"f fclm w-full\">\r\n"
			+ "                                <ul class=\"li-marker mb40 text-sm\">\r\n"
			+ "                                  $$$details$$$\r\n"
			+ "                                </ul>\r\n"
			+ "                                $$$isLoginButton$$$\r\n"
			+ "                                <div class=\"w-full h1 bg-base-stroke-default my24\"></div>\r\n"
			+ "                                <div class=\"w-full\">\r\n"
			+ "                                  <div class=\"text-sm leading-tight fw5 mb16\">$$$buttonTitle$$$</div>\r\n"
			+ "                                  <div class=\"g grid-autofit-180 gap-8 w-full s-gcol1 m-gcol1 tools-grid_w\">\r\n"
			+ "                                    $$$button$$$\r\n"
			+ "                                  </div>\r\n"
			+ "                                </div>\r\n"
			+ "                              </div>\r\n"
			+ "                            </div>\r\n"
			+ "                          </div>\r\n"
			+ "                          <div id=\"gnreco-detail\"></div>\r\n"
			+ "                          <div class=\"mb24\">\r\n"
			+ "                            $$$other$$$\r\n"
			+ "                          </div>\r\n"
			+ "                          <div class=\"g mb48\">\r\n"
			+ "                            <div class=\"overflow-auto border-bottom\">\r\n"
			+ "                              <div class=\"min-w-500\">\r\n"
			+ "                                <div class=\"f\">"
			+ "                                   $$$tabs_title$$$\r\n"
			+ "                                </div>\r\n"
			+ "                              </div>\r\n"
			+ "                            </div>\r\n"
			+ "                            <div class=\"overflow-auto\">\r\n"
			+ "                              <div class=\"mt20\">\r\n"
			+ "                                $$$tabs$$$\r\n"
			+ "                              </div>\r\n"
			+ "                            </div>\r\n"
			+ "                          </div>\r\n"
// 特長、製品サポートへのページ内リンク。2026/4/16削除決定
//			+ "                          <div class=\"mb48\">\r\n"
//			+ "                            <div class=\"f fm p24 gap-24 border border-stroke-subtle s-fclm s-ft s-py24 s-px16 s-gap-16 m-fclm m-ft m-py24 m-px16 m-gap-16\"><a class=\"f fm gap-4\" href=\"#features\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\" aria-hidden=\"true\" title=\"\">\r\n"
//			+ "                                <div><span class=\"text-sm leading-tight fw5 hover-link-underline\">$$$advantageTitle$$$</span></div></a><a class=\"f fm gap-4\" href=\"#support\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-bottom.svg\" alt=\"\" aria-hidden=\"true\" title=\"\">\r\n"
//			+ "                                <div><span class=\"text-sm leading-tight fw5 hover-link-underline\">$$$supportTitle$$$</span></div></a>\r\n"
//			+ "                            </div>\r\n"
//			+ "                          </div>\r\n"
			+ "                          <div class=\"mb48\" id=\"features\"><span class=\"text-2xl leading-tight fw5\">$$$advantageTitle$$$</span>\r\n"
			+ "                            $$$advantageBody$$$\r\n"
			+ "                          </div>\r\n"
			+ "                          $$$backUrl$$$\r\n"
			+ "            </div>\r\n"
			+ "</div>\r\n";
	public static String _tabHead =
			"<div class=\"web_tabs\">\r\n" +
					"<ul class=\"w_tabs\">\r\n" +

					"$$$tabTitle$$$"+

					"</ul>\r\n" +
					"\r\n" +
					"\r\n" +
					"<div class=\"accordion-wrapper\">\r\n" +
					"<div class=\"ac_arrow ac_arrow01\"></div>\r\n" +
					"<div class=\"ac_cont hide\">\r\n" +
					"\r\n" +
					"\r\n" +

					"<ul class=\"w_panels\">\r\n" ;
	public static String _tabFoot = "</ul>\r\n" +
			"\r\n" +
			"\r\n" +

			"</div><!--ac_cont-->\r\n" +
			"</div><!--accordion-wrapper-->\r\n" +
			"\r\n" +
			"\r\n" +
			"</div><!--web_tabs-->\r\n" +
			"\r\n" +
			"\r\n" ;

// 2022/11/21 new 3dCad start
/*	public static String _static_seriesCad3d =
			"    <form action=\"https://webassistants.partcommunity.com/23d-libs/smc_jp_assistant/index.html\" method=\"get\" class=\"sform\" target=\"_blank\">\r\n" +
			"    <label>フル品番検索</label>\r\n" +
			"    <input type=\"\" name=\"info\" class=\"k\">\r\n" +
			"    <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"this.form.submit();return false;\">\r\n" +
			"    <input type=\"hidden\" name=\"operation\" value=\"cns\">\r\n" +
			"    <input type=\"hidden\" name=\"language\" value=\"japanese\">\r\n" +
			"    </form>\r\n" +
			"    \r\n" +
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D/3D CAD</h5>\r\n" +
			"    <p>型式検索により2D/3D CADデータを様々なデータ形式で出力できます。</p>\r\n" +
			"    <p>オプション、マニホールド等搭載した状態で出力できます。</p>\r\n" +
			"<div style=\"border: 1px solid red;margin: 5px 0 5px; padding: 10px;\">2026年3月より2D/3D CADデータがSMCのユーザー登録でご利用可能になります。利用にはユーザー登録が必須となりますので、2026年2月末までにユーザー登録をお願いいたします。\r\n"
			+ "<br>\r\n"
			+ "ユーザー登録がお済みでない方は、<a href=\"https://www.smcworld.com/customer/ja/tologin.do\" target=\"_blank\" style=\"color:#0072c1; \">こちら</a></div>"+
			"    <a title=\"/webcatalog/3dcad/ja-jp/?id=$$$se_id$$$\" class=\"tx_link js-modal-open\">2D/3D CADデータはこちら</a>\r\n" +
			"    <div class=\"tx_box\">\r\n" +
			"    <p>\r\n" +
			"    ※2D/3D CADデータはキャデナス・ウェブ・ツー・キャド株式会社のシステムに移動します。<br>CADデータダウンロード時にはキャデナス・ウェブ・ツー・キャドの PART community のユーザー登録が必要になります。\r\n" +
			"    </p>\r\n" +
			"    </div>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n" ;*/
	public static String _static_seriesCad3d_202603 =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D/3D CAD</h5>\r\n" +
			"    <form action=\"/products/cad/ja-jp/\" method=\"get\" class=\"sform isLoginTrue\" style=\"display:none;\" target=\"_blank\">\r\n" +
			"    <label>フル品番検索</label>\r\n" +
			"    <input type=\"\" name=\"partNumber\" class=\"k\">\r\n" +
			"    <input type=\"button\" value=\"\" class=\"sbt\" onclick=\"this.form.submit();return false;\">\r\n" +
			"    </form>\r\n" +
			"    \r\n" +
			"    <p>型式検索により2D/3D CADデータを様々なデータ形式で出力できます。</p>\r\n" +
			"    <p>オプション、マニホールド等搭載した状態で出力できます。</p>\r\n" +
			"<div class=\"isLoginFalse w-full\" style=\"display: block; margin-bottom: 5px;\">\r\n"
			+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
			+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">2D/3D CADデータは\r\n"
			+ "ユーザ登録者限定サービスです。\r\n"
			+ "ログインしてご利用ください</div>\r\n"
			+ "<a href=\"javascript:void(0)\" class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">ログイン</span></a>\r\n"
			+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
			+ "\r\n"
			+ "</div>\r\n"
			+ "</div>\r\n"
			+ "</div>"+
			"    <a title=\"/webcatalog/3dcad/ja-jp/?id=$$$se_id$$$\" class=\"tx_link js-modal-open isLoginTrue\" style=\"display: none;\">2D/3D CADデータはこちら</a>\r\n" +
			"    <div class=\"tx_box\">\r\n" +
			"    <span>" +
			"    ※2D/3D CADデータはキャデナス・ウェブ・ツー・キャド株式会社のシステムに移動します。\r\n" +
			"    </span>\r\n" +
			"    </div>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n" ;
/*	public static String _static_seriesCad3d_E =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D/3D CAD</h5>\r\n" +
			"    <p>The new SMC CAD SYSTEM, CADENAS, allows you to output 2D/3D CAD data with full part numbers in various data formats. Responses to part number selection has been greatly improved with the newly developed system.</p>\r\n" +
			"<div style=\"border: 1px solid red;margin: 5px 0 5px; padding: 10px;\">Starting in March 2026, 2D/3D CAD data will be available to registered SMC users.\r\n"
			+ "<br>\r\n"
			+ "To acccess this service, user registration is required. We kindly ask that you complete your registration by the end of February 2026.\r\n"
			+ "<br>If you have not yet completed your user registration, please register &nbsp;<a href=\"https://www.smcworld.com/customer/en/tologin.do\" target=\"_blank\" style=\"color:#0072c1; \">HERE</a></div>"+
			"    <a title=\"/webcatalog/3dcad/en-jp/?id=$$$se_id$$$\" class=\"tx_link js-modal-open\">Click here for 2D/3D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n" ;*/
	public static String _static_seriesCad3d_202603_E =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D/3D CAD</h5>\r\n" +
			"    <span>The new SMC CAD SYSTEM, CADENAS, allows you to output 2D/3D CAD data with full part numbers in various data formats. Responses to part number selection has been greatly improved with the newly developed system.</span>\r\n" +
			"<div class=\"isLoginFalse\" style=\"display:block;\">\r\n"
			+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
			+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">2D/3D CAD\r\n"
			+ "are services available only to registered users.\r\n"
			+ "Please log in to use these services.</div>\r\n"
			+ "<a href=\"javascript:void(0)\" \r\n"
			+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Login</span></a>\r\n"
			+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
			+ "</div>\r\n"
			+ "</div>\r\n"
			+ "</div>"+
			"    <a title=\"/webcatalog/3dcad/en-jp/?id=$$$se_id$$$\" class=\"tx_link js-modal-open isLoginTrue\" style=\"display: none;\">Click here for 2D/3D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n" ;
	public static String _static_seriesCad3d_ZH =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D/3D CAD</h5>\r\n" +
			"    <p>能以各种数据格式输出支持所有型号的2D/3D CAD数据。<br>" +
			"能在安装了选购件、歧管的状态下输出。</p>\r\n" +
			"    <a title=\"/webcatalog/3dcad/en-jp/?id=$$$se_id$$$\" class=\"tx_link js-modal-open\">查看  2D/3D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n" ;
	public static String _static_seriesCad3d_ZHTW =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D/3D CAD</h5>\r\n" +
			"    <p>能以各種數據格式輸出支持所有型號的2D/3D CAD數據。<br>" +
			"    能在安裝了選購件、歧管的狀態下輸出。</p>\r\n" +
			"    <a title=\"/webcatalog/3dcad/en-jp/?id=$$$se_id$$$\" class=\"tx_link js-modal-open\">查看  2D/3D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n" ;
// 2022/11/21 new 3dCad end

	// 置き換え文字列 $$$cad2dLink$$$ -2026 
	public static String _seriesCad2d =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D CAD</h5>\r\n" +
			"    <a title=\"$$$cad2dLink$$$\" class=\"tx_link js-modal-open\">2D CADデータはこちら</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n";
	public static String _seriesCad2d_E =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D CAD</h5>\r\n" +
			"    <a title=\"$$$cad2dLink$$$\" class=\"tx_link js-modal-open\">Click here for 2D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n";
	public static String _seriesCad2d_ZH =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D CAD</h5>\r\n" +
			"    <a title=\"$$$cad2dLink$$$\" class=\"tx_link js-modal-open\">查看 2D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n";
	public static String _seriesCad2d_ZHTW =
			"    <div class=\"cad_item\">\r\n" +
			"    <h5>2D CAD</h5>\r\n" +
			"    <a title=\"$$$cad2dLink$$$\" class=\"tx_link js-modal-open\">查看 2D CAD</a>\r\n" +
			"    </div><!--.cad_item-->\r\n" +
			"    \r\n";
	// ページ作成、表示の最後に読み込み popup 2026もこれを使う
	public static String _seriesCadModal =
			"<script>"+
			"$(function () {\r\n" +
			"    $(\".js-modal-open\").click(function(e){\r\n" +
			"        e.preventDefault();\r\n" +
			"        var target = $(this).attr('title');\r\n" +
			"        $('#modalContent').html(\"<iframe frameborder='0' hspace='0' src=\"+target+\" style='width:900px;height:512px;'></iframe>\");\r\n" +
			"        $('.js-modal').fadeIn();\r\n" +
			"        return false;\r\n" +
			"    }); \r\n" +
			"    $(\".js-modal-close\").click(function(){\r\n" +
			"        $('.js-modal').fadeOut();\r\n" +
			"        return false;\r\n" +
			"    });\r\n" +
			"});"+
			"</script>"+
			"<style>"+
			".modal{\r\n" +
			"    display: none;\r\n" +
			"    height: 100vh;\r\n" +
			"    position: fixed;\r\n" +
			"    top: 0;\r\n" +
			"    z-index:900;\r\n" +
			"    width: 100%;\r\n" +
			"}\r\n" +
			".modal__bg{\r\n" +
			"    background: rgba(0,0,0,0.8);\r\n" +
			"    height: 100vh;\r\n" +
			"    position: fixed;\r\n" +
			"    left:0; top:0; "+
			"    width: 100%;\r\n" +
			"    z-index:909;\r\n" +
			"}\r\n" +
			".modal__content{\r\n" +
			"    background: #fff;\r\n" +
			"    left: 50%;\r\n" +
			"    padding: 20px;\r\n" +
			"    position: fixed;\r\n" +
			"    top: 50%;\r\n" +
			"    transform: translate(-50%,-50%);\r\n" +
			"    z-index:999;\r\n" +
			"    width: 950px;\r\n" +
			"}</style>\r\n" +
			"<div id=\"modal01\" class=\"modal js-modal\">\r\n" +
			"        <div class=\"modal__bg js-modal-close\"></div>\r\n" +
			"        <div class=\"modal__content\">\r\n" +
			"          <div style=\"width:100%;text-align:right\"><a href=\"#\" class=\"js-modal-close\"><img src=\"/webcatalog/images/ja/bt_close.jpg\"></a></div>" +
			"            <p id=\"modalContent\"></p>\r\n" +
			"        </div><!--modal__content-->\r\n" +
			"    </div>";

	// 以下 -2026 cadのiframe用HTML
	public static String _3dCadFrameHtml =  "" +
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\">\r\n" +
			"\r\n" +
			"<head>\r\n" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\r\n" +
			"<meta http-equiv=\"content-script-type\" content=\"text/javascript\" />\r\n" +
			"<meta http-equiv=\"content-style-type\" content=\"text/css\" />\r\n" +
			"<title>SMC-3DCAD</title>\r\n" +
			"<link href=\"/css/common.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/header/header.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"\r\n" +
			"<link href=\"/webcatalog/css/cad3d.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/webcatalog/css/products.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"\r\n" +
			"<script language=\"javascript\" type=\"text/javascript\" src=\"/js/jquery-1.6.1.min.js\"></script>\r\n" +
			"<script language=\"javascript\" type=\"text/javascript\" src=\"/js/global.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/js/dropdown/jquery.dropdownPlain.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/webcatalog/js/cad3d.js\"></script>\r\n" +
			"\r\n" +
			"<link rel=\"apple-touch-icon\" href=\"/sp/smc-icon.png\" />\r\n" +
			"<link rel=\"shortcut icon\" href=\"/images/favicon-smc.ico\" />"+
			"<!-- Global site tag (gtag.js) - Google Analytics -->\r\n" +
			"<script async=\"\" src=\"https://www.googletagmanager.com/gtag/js?id=UA-25847919-2\"></script>\r\n" +
			"<script>\r\n" +
			"  window.dataLayer = window.dataLayer || [];\r\n" +
			"  function gtag(){dataLayer.push(arguments);}\r\n" +
			"  gtag('js', new Date());\r\n" +
			"\r\n" +
			"  gtag('config', 'UA-25847919-2');\r\n" +
			"</script>\r\n" +
			"\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/js/ga_track.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/js/ja.js\"></script>\r\n" +
			"\r\n" +
			"<link href=\"/home/css/common2.css\" rel=\"stylesheet\" type=\"text/css\">\r\n" +
			"<link href=\"/header/megadrop.css\" rel=\"stylesheet\" type=\"text/css\">\r\n" +
			"\r\n" +
			"\r\n" +
			"\r\n" +
			"<script>\r\n" +
			"$(function(){\r\n" +
			"	highlightKw();\r\n" +
			"});\r\n" +
			"</script>\r\n" +
			"\r\n" +
			"\r\n" +
			"\r\n" +
			"</head>\r\n" +
			"<body>\r\n" +
			"\r\n" +
			"<div class=\"wintitle p_3dcad\">2D/3DCAD</div>\r\n" +
			"<center>\r\n" +
			"<div id=\"center_main\">";
	public static String _3dCadFrameHtmlEND = "<div class=\"clear\"></div>\r\n" +
			"<!--end-->\r\n" +
			"\r\n" +
			"<!--end-->\r\n" +
			"\r\n" +
			"<p>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"<br>\r\n" +
			"</p>\r\n" +
			"\r\n" +
			"\r\n" +
			"</div><!--main-->\r\n" +
			"</center>\r\n" +
			"\r\n" +
			"\r\n" +
			"<!--main-->\r\n" +
			"\r\n" +
			"\r\n" +
			"</body></html>";

	public static String _2dCadFrameHtml = "" +
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"ja-JP\" xml:lang=\"ja-JP\">\r\n" +
			"\r\n" +
			"<head>\r\n" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\r\n" +
			"<meta http-equiv=\"content-script-type\" content=\"text/javascript\" />\r\n" +
			"<meta http-equiv=\"content-style-type\" content=\"text/css\" />\r\n" +
			"\r\n" +
			"\r\n" +
			"<title>SMC-製品検索</title>\r\n" +
			"<meta name=\"keywords\" content=\"\"/> \r\n" +
			"<meta name=\"description\" content=\"\"/>\r\n" +
			"<link href=\"/css/normalize.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/css/common.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/header/header.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
//			"<link href=\"/products/css/style.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/webcatalog/css/products.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"\r\n" +
			"<script language=\"javascript\" type=\"text/javascript\" src=\"/js/jquery-1.6.1.min.js\"></script>\r\n" +
			"<script language=\"javascript\" type=\"text/javascript\" src=\"/js/global.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/js/dropdown/jquery.dropdownPlain.js\"></script>\r\n" +
//			"<script type=\"text/javascript\" language=\"javascript\" src=\"/products/js/products.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/webcatalog/js/products.js\"></script>\r\n" +
			"\r\n" +
			"<!--[if gte IE 9]>\r\n" +
			"  <style type=\"text/css\">\r\n" +
			"    .gradient {\r\n" +
			"       filter: none;\r\n" +
			"    }\r\n" +
			"  </style>\r\n" +
			"<![endif]-->\r\n" +
			"\r\n" +
			"<link rel=\"apple-touch-icon\" href=\"/sp/smc-icon.png\" />\r\n" +
			"<link rel=\"shortcut icon\" href=\"/images/favicon-smc.ico\" />\r\n" +
			"\r\n" +
			"<!-- Global site tag (gtag.js) - Google Analytics -->\r\n" +
			"<script async src=\"https://www.googletagmanager.com/gtag/js?id=UA-25847919-2\"></script>\r\n" +
			"<script>\r\n" +
			"  window.dataLayer = window.dataLayer || [];\r\n" +
			"  function gtag(){dataLayer.push(arguments);}\r\n" +
			"  gtag('js', new Date());\r\n" +
			"\r\n" +
			"  gtag('config', 'UA-25847919-2');\r\n" +
			"</script>\r\n" +
			"\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/js/ga_track.js\"></script>\r\n" +
			"<script type=\"text/javascript\" language=\"javascript\" src=\"/js/ja.js\"></script>\r\n" +
			"\r\n" +
			"<link href=\"/home/css/common2.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"<link href=\"/header/megadrop.css\" rel=\"stylesheet\" type=\"text/css\" />\r\n" +
			"\r\n" +
			"\r\n" +
			"</head>\r\n" +
			"<body>\r\n" +
			"\r\n" +
			"<div id=\"winmain\" style=\"width:880px;margin:10px auto\">\r\n" +
			"\r\n" +
			"\r\n" +
			"<div class=\"wintitle\">2D CAD</div>\r\n" +
			"<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" ;


	public static String _2dCadFrameHtmlEND = "</table>\r\n" +
			"</div><!--winmain-->\r\n" +
			"</body>\r\n" +
			"</html>";
	// 以上 -2026
	// 以下 2026～ cadのiframe用HTML 2Dも3Dと兼用
	public static String _3dCadFrameHtml_2026 = "<!DOCTYPE html>\r\n"
			+ "<html lang=\"ja\">\r\n"
			+ "<head>\r\n"
			+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n"
			+ "<meta name=\"author\" content=\"\">\r\n"
			+ "<meta name=\"description\" content=\"\">\r\n"
			+ "<meta name=\"keywords\" content=\"\">\r\n"
			+ "<title>SMC-3DCAD</title>"
			+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\r\n"
			+ "\r\n"
			+ "<meta property=\"og:image\" content=\"/assets/smcimage/common/ogp.jpg\">\r\n"
			+ "\r\n"
			+ "<link rel=\"apple-touch-icon\" href=\"/sp/smc-icon.png\" />\r\n"
			+ "<link rel=\"icon\" href=\"/assets/smcimage/common/favicon.ico\">"
			+ "<!-- Google Tag Manager -->\r\n"
			+ "<script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':\r\n"
			+ "new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],\r\n"
			+ "j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src='https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);\r\n"
			+ "})(window,document,'script','dataLayer','GTM-KL3RV2TD');</script>\r\n"
			+ "<!-- End Google Tag Manager -->"
			+ "<link rel=\"stylesheet\" href=\"/assets/smccss/common/meltline.css\">\r\n"
			+ "<link rel=\"stylesheet\" href=\"/assets/smccss/common/common.css\">"
			+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"/smc_css/lightcase.css\">\r\n"
			+ "<link rel=\"stylesheet\" href=\"/smc_css/scroll-hint.css\">"
			+ "</head>\r\n"
			+ "<body>"
			+ "<h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">$$$TITLE$$$</h2>";
	public static String _3dCadFrameHtmlEND_2026 = "</body></html>";

	public static String _OrderMadeJa = "<div class=\"resulttbl\" style=\"border:#0074bf 1px solid;text-align:center;padding:8px 0 5px 0;margin:5px 0;\">\r\n" +
			"    <p style=\"font-size:16px;font-weight:bold;\">\r\n" +
			"    <img src=\"/images/common/arrow_r.jpg\" style=\"vertical-align: middle;\">オーダーメイド組合せ型式表示方法は<a href=\"/upfiles/etc/custom/OM2-P1727.pdf\" target=\"_blank\">こちら</a>をご参照ください。\r\n" +
			"    </p>\r\n" +
			"</div>\r\n" +
			"\r\n" +
			"<span class=\"pt12\" style=\"font-weight:normal\">※製品個別オーダーメイド仕様については、各製品カタログをご参照ください。</span>\r\n" ;
	public static String _OrderMadeJa2026 = "<div class=\"w-full\">\r\n"
			+ "<div class=\"f fclm fh gap-16 mb16 p16 bg-base-container-default border border-base-stroke-subtle\">\r\n"
			+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6\">\r\n"
			+ "オーダーメイド組合せ型式表示方法は<a class=\"ml8 gap-4\" href=\"/upfiles/etc/custom/OM2-P1727.pdf\" target=\"_blank\"><span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">こちら</span>    <img class=\"s14 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\">  </a>をご参照ください。"
			+ "</div>\r\n"
			+ "<span class=\"text-secondary pd24 text-sm leading-tight \">※製品個別オーダーメイド仕様については、各製品カタログをご参照ください。</span>"
			+ "</div>\r\n"
			+ "</div>";
	public static String _CustomJa = "<div class=\"resulttbl\" style=\"border:#0074bf 1px solid;text-align:center;padding:8px 0 5px 0;margin:5px 0;\">\r\n" +
			"ご注文の際はホームページ簡易特注システムより「簡易特注品仕様書」を" +
			"ダウンロードのうえ手配をお願いします。<br/>\r\n" +
			"簡易特注システムは<a href=\"/specs/customize/\" target=\"_blank\">こちら</a>\r\n" +
			"</div>\r\n" +
			"\r\n" ;
	public static String _CustomJa2026 = "<div class=\"w-full\">\r\n"
			+ "<div class=\"f fclm fh gap-16 mb16 p24 bg-base-container-default border border-base-stroke-subtle\">\r\n"
			+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">\r\n"
			+ "ご注文の際はホームページ簡易特注システムより「簡易特注品仕様書」をダウンロードのうえ手配をお願いします。\r\n"
			+ "簡易特注システムは"
			+ "<a class=\"gap-4\" target=\"_blank\" href=\"/specs/customize/\">    <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">こちら</span>    <img class=\"s14 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\"\">  </a>"
			+ "</div>\r\n"
			+ "</div>\r\n"
			+ "</div>";
	// 品番確認のログインチェック // 3/1 
	public static String _CheckLoginJa = "<div class=\"isLoginFalse w-full\" style=\"display:block;margin-bottom: 5px;\">\r\n"
			+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
			+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">品番確認・カタログは\r\n"
			+ "ユーザ登録者限定サービスです。\r\n"
			+ "ログインしてご利用ください</div>\r\n"
			+ "<a href=\"javascript:void(0)\" \r\n"
			+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">ログイン</span></a>\r\n"
			+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
			+ "<button class=\"button neutral solid disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">品番確認</span></button>\r\n"
			+ "<button class=\"button neutral outline disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">カタログダウンロード（PDF）</span></button>\r\n"
			+ "<button class=\"button neutral outline disabled large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">デジタルカタログ</span></button>\r\n"
			+ "</div>\r\n"
			+ "</div>\r\n"
			+ "</div>";
	public static String _CheckLoginEn = "<div class=\"isLoginFalse w-full\" style=\"display:block;margin-bottom: 5px;\">\r\n"
			+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
			+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">Product number check, Catalogs and Digital catalog\r\n"
			+ "are services available only to registered users.\r\n"
			+ "Please log in to use these services.</div>\r\n"
			+ "<a href=\"javascript:void(0)\" \r\n"
			+ "   class=\"button large primary gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none doOauthLogin\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Login</span></a>\r\n"
			+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
			+ "<button class=\"button neutral solid disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Product number check</span></button>\r\n"
			+ "<button class=\"button neutral outline disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Catalogs</span></button>\r\n"
			+ "<button class=\"button neutral outline disabled small-s\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/file.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">Digital catalog</span></button>\r\n"
			+ "</div>\r\n"
			+ "</div>\r\n"
			+ "</div>";
	// 品番確認のログインチェック // 2026 
	public static String _CheckLoginJa2026 = "<div class=\"isLoginFalse w-full\" style=\"display:block;margin-bottom: 5px;\">\r\n"
				+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
				+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">品番確認・カタログは\r\n"
				+ "ユーザ登録者限定サービスです。\r\n"
				+ "ログインしてご利用ください</div>\r\n"
				+ "<button class=\"doOauthLogin button large primary solid gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none\" type=\"button\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/login.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">ログイン</span></button>\r\n"
				+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
				+ "  <button class=\"button solid gap-8 w-full primary large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">品番確認</span></button>\r\n"
				+ "  <button class=\"button solid gap-8 w-full secondary large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">カタログダウンロード（PDF）</span></button>\r\n"
				+ "  <button class=\"button secondary solid large gap-8 w-full large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/book.svg\" alt=\"\"/><span class=\"flex-fixed text-sm leading-tight\">デジタルカタログ</span></button>\r\n"
				+ "</div>\r\n"
				+ "</div>\r\n"
				+ "</div>"
				+ "<div class=\"isLoginTrue w-full\" style=\"display:none;margin-bottom: 5px;\">"
				+ "  <div class=\"f fclm fh gap-16\">\r\n"
				+ "    <div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">"
				+ "    $$$specialButton$$$"
				+ "    </div>\r\n"
				+ "  </div>\r\n"
				+ "</div>";
	public static String _CheckLoginEn2026 = "<div class=\"isLoginFalse w-full\" style=\"display:block;margin-bottom: 5px;\">\r\n"
				+ "<div class=\"f fclm fh gap-16  p24 bg-base-container-accent border border-base-stroke-subtle\">\r\n"
				+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw6 white-space-pre-wrap\">Product number check, Catalogs and Digital catalog\r\n"
				+ "are services available only to registered users.\r\n"
				+ "Please log in to use these services.</div>\r\n"
				+ "<button class=\"doOauthLogin button large primary solid gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none\" type=\"button\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/login.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">Login</span></button>\r\n"
				+ "<div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">\r\n"
				+ "  <button class=\"button solid gap-8 w-full primary large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Product number check</span></button>\r\n"
				+ "  <button class=\"button solid gap-8 w-full secondary large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\"\"/><span class=\"text-sm s-leading-tight m-leading-tight\">Catalogs</span></button>\r\n"
				+ "  <button class=\"button secondary solid large gap-8 w-full large\" type=\"button\" disabled=\"disabled\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/book.svg\" alt=\"\" title=\"\"><span class=\"flex-fixed text-sm leading-tight\">Digital catalog</span></button>\r\n"
				+ "</div>\r\n"
				+ "</div>\r\n"
				+ "</div>"
				+ "<div class=\"isLoginTrue w-full\" style=\"display:none;margin-bottom: 5px;\">"
				+ "  <div class=\"f fclm fh gap-16\">\r\n"
				+ "    <div class=\"g grid-autofit-160 w-full gap-8 m-gcol1 s-gcol1\">"
				+ "    $$$specialButton$$$"
				+ "    </div>\r\n"
				+ "  </div>\r\n"
				+ "</div>";
	/**
	 * シリーズ一覧の区切り線
	 */
	public static String _DivSeriesList2026 = "<div class=\"w-full h1 bg-base-stroke-default my36\"></div>";
	/**
	 * 一覧へ戻るボタン
	 */
	public static String _BackToList = "<div class=\"f fclm fh gap-16\">\r\n"
			+ "                      <div class=\"g gap-8 m-gcol1 s-gcol1\">\r\n"
			+ "                        <button class=\"button solid gap-8 primary large\" type=\"button\" onclick=\"location.href='$$$url$$$'\">"
			+ "                            <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-left-white.svg\" alt=\"\" title=\"\">"
			+ "                            <span class=\"text-sm s-leading-tight m-leading-tight\">$$$message$$$</span>"
			+ "                        </button>\r\n"
			+ "                      </div>\r\n"
			+ "                    </div>";
	/**
	 *
	 * @param series
	 * @param itemCnt リスト内の順番。1スタート
	 * @note itemCnt中止。検索結果で同一IDが発生する。modelNumberに変更(/は_に置き換え）2022/10/25
	 * @param c null OK (nullの場合はbreadClumbを全部出す。)
	 * @param c2 null OK
	 * @param url  for mylist
	 * @param lang
	 * @param isAdvantage true:製品特長を表示
	 * @param isGuide true:ガイド用、リスト表示＋ページ＋カテゴリ全表示
	 * @return
	 */
	public String get(Series series, Category c, Category c2, String url, String lang, boolean isAdvantage, boolean isGuide)
	{
		String ret = "";
		if  (isAdvantage) ret = _seriesTemplate;
		else if  (isGuide) ret = _seriesListTemplate;
		else  ret = _seriesListTemplate;

		// 画像 製品特長button
		String img = null;
		String adv = "";
		if (series.getImage() != null && series.getImage().isEmpty() == false) {
			img = AppConfig.ImageProdUrl + series.getLang() + "/" + series.getImage();
			if (lang != null && lang.equals("zh-cn")) img = AppConfig.ImageProdPath + series.getLang() + "/" + series.getImage();
			adv = "    <img src=\"" + img + "\" class=\"result_img\">\r\n" ;
		}
		if (isGuide == false && series.getAdvantage().isEmpty() == false) {
			String advText = "製品特長";
			if (lang.indexOf("en-") > -1) advText = "Features";
			else if (lang.equals("zh-tw")) advText = "產品特點";
			else if (lang.indexOf("zh-") > -1) advText = "产品特点";

			if (isAdvantage == false) {
				if (c2 != null) {
					adv += "    <a href=\""+AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/" + c2.getSlug() + "/"+ series.getModelNumber() + "/#detail\" class=\"specBt\">"+advText+"</a>\r\n" ;
				} else {
					adv += "    <a href=\""+AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/series/"+ series.getModelNumber() + "/#detail\" class=\"specBt\">"+advText+"</a>\r\n" ;
				}
			}
			ret = StringUtils.replace(ret, "$$$advantage$$$", adv);
			ret = StringUtils.replace(ret, "$$$advantageTitle$$$", "<h3>"+ advText + "</h3>");
		} else {
			ret = StringUtils.replace(ret, "$$$advantage$$$", adv);
			ret = StringUtils.replace(ret, "$$$advantageTitle$$$", "");
			isAdvantage = false; // 要求があっても無ければfalseに。タイトルを表示させない。
		}

		String spec = "";
		List<String> specList = new LinkedList<String>(); // manifold調査用
		if (series.getSpec().isEmpty() == false) {

			spec+="<table class=\"web_tb spec_tb\">\r\n";
			spec+= "<tbody><tr>\r\n";

			int cnt = 0;
			int seriesIndex = 0;
			int cadIndex = -1;
			//ObjectMapper mapper = new ObjectMapper();
			try {
				String tmp = series.getSpec();
				JSONArray result = new JSONArray(tmp.replace("\r\n", "").replace("\t", ""));
				//JSONObject jsonObject = new JSONObject(tmp.replace("\r\n", "").replace("\t", ""));
				//Map<String, Object> result = jsonObject.toMap();
				//List<String[]> result = mapper.readValue(tmp.replace("\r\n", "").replace("\t", "").replace("[", "").replace("]", ""),
				//		  new TypeReference<List<String[]>>() {});
				for (Object obj : result) {
					JSONArray arr = (JSONArray)obj;
					String celS= "<td>";
					String celE = "</td>";
					if (cnt == 0) {
						celS= "<th>";
						celE = "</th>";
					}
					int colCnt = 0;
					for(Object str: arr) {
						if (str != null && str.equals("[2DCAD]")) {
							cadIndex = colCnt;
							break;
						}
						if (colCnt == cadIndex) break;
						spec += celS+str+ celE;
						if (str.equals("シリーズ") || str.equals("Model") || str.equals("系列")) {
							seriesIndex = colCnt;
							spec = spec.replace("<th>", "<th nowrap>");
						}
						if (cnt != 0 && colCnt == seriesIndex) specList.add(str.toString()); // cnt==0は「シリーズ」の文字
						colCnt++;
					}
					cnt++;
					if (cnt < result.length()) {
						if (cnt%2 == 0) spec+="        </tr>\r\n<tr class=\"gr\">" ;
						else spec+="        </tr>\r\n<tr>" ;
					}
				}
			} catch (Exception e) {

			}
			spec+="        </tr>\r\n" ;
			spec+="    </tbody>\r\n" ;
			spec+="</table>\r\n";
			spec+="\r\n";
		}
		ret = StringUtils.replace(ret, "$$$spec$$$", spec);

		// 品番確認、カタログPDFをログイン必須に。
		// 品番確認の右はカタログPDF。FAQは通常の先頭へ

		// button tab
		String button = "";
		List<SeriesLink> list = series.getLink();
		if (list != null && list.size() > 0) {
			String buttonClass = "downloadBt";
			if (lang != null && lang.indexOf("en-") > -1) buttonClass += "_E";
			List<String> tabTitles = new LinkedList<String>();
			List<String> tabBodyList = new LinkedList<String>();

			List<SeriesLink> cad2DList = new LinkedList<SeriesLink>();
			List<SeriesLink> manualList = new LinkedList<SeriesLink>();
			List<SeriesLink> manifoldList = new LinkedList<SeriesLink>();
			List<SeriesLink> ceList = new LinkedList<SeriesLink>();

			// 3S
			button+="";
			// /webcatalog/s3s/ja-jp/check/JSY-P
			if (lang.equals("zh-tw")) {
				// 台湾は中国が繁体語はやらなそうなので、とりあえず、簡体語。2023/6/8
				String sid = series.getModelNumber().trim();
				if (sid != null && sid.isEmpty() == false) sid = sid.replace("ZHTW", "ZH");
				button += "<li><a href=\""+AppConfig.PageProdUrl+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+sid+"\" class=\"bt_3s\" target=\"_blank\" style=\"margin-bottom: 5px\">型號確認</a></li>\r\n";
			} else if (lang.indexOf("zh-") > -1) {
				button += "<li><a href=\""+AppConfig.PageProdUrlZH+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+series.getModelNumber().trim()+"\" class=\"bt_3s\" target=\"_blank\" style=\"margin-bottom: 5px\">型号确认</a></li>\r\n";
			} else if  (lang.indexOf("en-") > -1) {
				button += "<li class='isLoginTrue'><a href=\""+AppConfig.PageProdUrl+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+series.getModelNumber().trim()+"\" class=\"bt_3s\" target=\"_blank\" style=\"margin-bottom: 5px\">Product Number Check</a></li>\r\n";
			} else {
				button += "<li class='isLoginTrue'><a href=\""+AppConfig.PageProdUrl+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+series.getModelNumber().trim()+"\" class=\"bt_3s\" target=\"_blank\" style=\"margin-bottom: 5px\">品番確認（型式表示方法）</a></li>\r\n";
			}
			// カタログPDF Add 20260130
			boolean isCatalogPDF = false;
			SeriesLink linkCatalogPDF = null;
			for(SeriesLink sl : list) {
				String title = sl.getLinkMaster().getTitle();
				if (title != null && title.equals("カタログ閲覧")
						&& sl.getUrl().isEmpty() == false) {
					isCatalogPDF = true;
					linkCatalogPDF = sl;
					break;
				}
			}
			if (isCatalogPDF && linkCatalogPDF != null) {
				// 品番確認の次に表示
				String strUrl = linkCatalogPDF.getUrl();
				String title = linkCatalogPDF.getLinkMaster().getName();
				if (lang.equals("zh-tw")) {
					button += "<li class=\"cat_pdf\"><a href=\"" + strUrl + "\" target=\"_blank\" style=\"margin-bottom: 5px\">"+title+"</a></li>\r\n";
				} else if (lang.indexOf("zh-") > -1) {
					button += "<li class=\"cat_pdf\"><a href=\"" + strUrl + "\" target=\"_blank\" style=\"margin-bottom: 5px\">"+title+"</a></li>\r\n";
				} else if  (lang.indexOf("en-") > -1) { // 3/1 display:noneへ
					button += "<li class=\"cat_pdf isLoginTrue\" style=\"display:none;\"><a href=\"" + strUrl + "\" target=\"_blank\" style=\"margin-bottom: 5px\">"+title+"</a></li>\r\n";
				} else {
					title = "カタログダウンロード(PDF)"; // 3/1 display:noneへ
					button += "<li class=\"cat_pdf isLoginTrue\" style=\"display:none;\"><a href=\"" + strUrl + "\" target=\"_blank\" style=\"margin-bottom: 5px\">"+title+"</a></li>\r\n";
				}
				if (lang.equals("ja-jp") || lang.equals("en-jp")) { // デジタルカタログは日本語、英語のみ
					String catalog = "デジタルカタログ";
					if (lang.equals("en-jp")) catalog="Digital catalog";
					String href = linkCatalogPDF.getUrl();
					int start = href.indexOf("/data/");
					if (start > -1) {
						href = href.substring(0,start) + "/index.html";
						button+= "    <li class=\"digi_cat isLoginTrue\"><a href=\""+href+"\" target=\"_blank\">"+catalog+"</a></li>\r\n";
					} else {
						start = href.indexOf("/6-");
						if (start > -1) {
							href = href.replace("/index.pdf", "/index.html");
							href = href.replace("/pdf/catalog/", "/catalog/");
							button+= "    <li class=\"digi_cat isLoginTrue\"><a href=\""+href+"\" target=\"_blank\">"+catalog+"</a></li>\r\n";
						}
					}
				}
			} else {
				// カタログPDFが無ければ上の3Sボタンの右には何も表示しない。
				button += "<br>\r\n";
			}
			// マニホールドコンフィグレータ Add 20240314
			boolean isManifoldConfigrator = false;
			SeriesLink linkManifoldConfigrator = null;
			for(SeriesLink sl : list) {
				String title = sl.getLinkMaster().getTitle();
				if (title != null && title.equals("マニホールドコンフィグレータ")
						&& sl.getUrl().isEmpty() == false) {
					isManifoldConfigrator = true;
					linkManifoldConfigrator = sl;
					break;
				}
			}
			// FAQ 2023/8/3 Add
			// FAQは通常の先頭へ 2026/1/30
			Optional<SeriesFaq> oFaq = faqRepo.findBySeriesId(series.getId());
			if (oFaq.isPresent()) {
				SeriesFaq faq = oFaq.get();
				if (faq.getFaq() != null && faq.getFaq().isEmpty() == false) {
					// 2023/8/17 フルパスのリンクも可能に。数字で始まらない場合はフルパスリンク
					String strFaq = faq.getFaq().trim();
					char ch = strFaq.charAt(0);
					if (ch >= '0' && ch <= '9') {
						if (lang.equals("zh-tw")) {
							button += "<li><a href=\""+AppConfig.PageProdUrl+AppConfig.FaqPath+"/"+lang+"/list/" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">FAQ</a></li>\r\n";
						} else if (lang.indexOf("zh-") > -1) {
							button += "<li><a href=\""+AppConfig.PageProdUrlZH+AppConfig.FaqPath+"/"+lang+"/list/" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">常见问题解答(FAQ)</a></li>\r\n";
						} else if  (lang.indexOf("en-") > -1) {
							button += "<li><a href=\""+AppConfig.PageProdUrl+AppConfig.FaqPath+"/"+lang+"/list/" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">FAQ</a></li>\r\n";
						} else {
							button += "<li><a href=\""+AppConfig.PageProdUrl+AppConfig.FaqPath+"/"+lang+"/list/" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">FAQ ～よくあるご質問～</a></li>\r\n";
						}
					} else {
						if (lang.equals("zh-tw")) {
							button += "<li><a href=\"" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">FAQ</a></li>\r\n";
						} else if (lang.indexOf("zh-") > -1) {
							button += "<li><a href=\"" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">常见问题解答(FAQ)</a></li>\r\n";
						} else if  (lang.indexOf("en-") > -1) {
							button += "<li><a href=\"" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">FAQ</a></li>\r\n";
						} else {
							button += "<li><a href=\"" + strFaq + "\" class=\"bt_faq2\" target=\"_blank\" style=\"margin-bottom: 5px\">FAQ ～よくあるご質問～</a></li>\r\n";
						}
					}
				}
			}
			if (isManifoldConfigrator && linkManifoldConfigrator != null) {
				// FAQが無ければ品番確認の次に表示
				String strUrl = linkManifoldConfigrator.getUrl();
				if (lang.equals("zh-tw")) {
					button += "<li><a href=\"" + strUrl + "\" class=\"bt_manifold\" target=\"_blank\" style=\"margin-bottom: 5px\">Manifold Configrator</a></li>\r\n";
				} else if (lang.indexOf("zh-") > -1) {
					button += "<li><a href=\"" + strUrl + "\" class=\"bt_manifold\" target=\"_blank\" style=\"margin-bottom: 5px\">Manifold Configrator</a></li>\r\n";
				} else if  (lang.indexOf("en-") > -1) {
					button += "<li><a href=\"" + strUrl + "\" class=\"bt_manifold\" target=\"_blank\" style=\"margin-bottom: 5px\">Manifold Configrator</a></li>\r\n";
				} else {
					button += "<li><a href=\"" + strUrl + "\" class=\"bt_manifold\" target=\"_blank\" style=\"margin-bottom: 5px\">マニホールド品番確認</a></li>\r\n";
				}
			}

			int tabCount = 1;
			for(SeriesLink s : list) {
				
				String icon = s.getLinkMaster().getIconClass();
				String title = s.getLinkMaster().getTitle();
				
				if (title != null && title.equals("マニホールドコンフィグレータ")) continue;
				if (title != null && title.equals("カタログ閲覧")) continue;
				
				if (s.getLinkMaster().getType() == SeriesLinkType.ICON ) {
					if (icon == null || icon.isEmpty()) {
						button+="    <li class=\"cat_iodd\"><a href=\""+s.getUrl()+"\">"+s.getLinkMaster().getName()+"</a></li>\r\n" ;
					} else if (icon.equals("cat_pdf")) {
						// 2026/3/5 デジタルカタログもログイン必須へ
						/*if (lang.equals("ja-jp") || lang.equals("en-jp")) { // デジタルカタログは日本語、英語のみ
							String catalog = "デジタルカタログ";
							if (lang.equals("en-jp")) catalog="Digital catalog";
							String href = s.getUrl();
							int start = href.indexOf("/data/");
							if (start > -1) {
								href = href.substring(0,start) + "/index.html";
								button+= "    <li class=\"digi_cat\"><a href=\""+href+"\" target=\"_blank\">"+catalog+"</a></li>\r\n";
							} else {
								start = href.indexOf("/6-");
								if (start > -1) {
									href = href.replace("/index.pdf", "/index.html");
									href = href.replace("/pdf/catalog/", "/catalog/");
									button+= "    <li class=\"digi_cat\"><a href=\""+href+"\" target=\"_blank\">"+catalog+"</a></li>\r\n";
								}
							}
						}*/
						/*String pdf =s.getUrl();
						String catalogs = "カタログダウンロード(PDF)";
						if (lang.indexOf("en-") > -1) catalogs = "Catalogs";
						else if (lang.equals("zh-tw")) catalogs = "目錄瀏覽";
						else if (lang.indexOf("zh-") > -1) catalogs = "产品样本";
						if (pdf.indexOf("/index.pdf") > -1 || pdf.indexOf("/index.html") > -1) {
							pdf = pdf.replace("/index.pdf", "");
							pdf = pdf.replace("/index.html", "");
							pdf = pdf.replace("/pdf/catalog/", "/catalog/");
							String[] arr = pdf.split("/");
							String tmp = arr[arr.length-1];
							pdf = "";
							for(String str : arr) {
								if (str.isEmpty() == false) pdf += "/" + str;
							}
							pdf += "/data/" + tmp+".pdf";
						}
						button+= "    <li class=\""+icon+"\"><a href=\""+pdf+"\" target=\"_blank\">"+catalogs+"</a></li>\r\n" ;
						*/
					} else if (icon.equals("cat_appli")) {
						String principle = "作動原理";
						if (lang.indexOf("en-") > -1) principle = "Working principle";
						else if (lang.indexOf("zh-") > -1) principle = "操作原理";
						if (series.getModelNumber().equals("ZH") || series.getModelNumber().equals("ZH-E") || series.getModelNumber().equals("ZH-ZH") || series.getModelNumber().equals("ZH-TW")) {
							button+="    <li class=\""+icon+"\"><a href=\""+s.getUrl()+"\" target=\"_blank\">"+principle+"</a></li>\r\n" ;
						} else {
							button+="    <li class=\""+icon+"\"><a href=\""+s.getUrl()+"\" target=\"_blank\">"+s.getLinkMaster().getName()+"</a></li>\r\n" ;
						}
					} else if (icon.isEmpty() == false){
						button+="    <li class=\""+icon+"\"><a href=\""+s.getUrl()+"\" target=\"_blank\" >"+s.getLinkMaster().getName()+"</a></li>\r\n" ;
					} else {
						button+="    <li class=\"cat_iodd\"><a href=\""+s.getUrl()+"\" target=\"_blank\">"+s.getLinkMaster().getName()+"</a></li>\r\n" ;
					}
				} else {
					// DOWNLOAD series_linkが複数あるので、リストで保持。
					// 取説とCEはHeartCoreなので、１つでもあれば取得処理
					if (title.indexOf("2DCAD") > -1) {
						cad2DList.add(s);

					} else if (title.equals("取扱説明書")) { // HeartCore
						manualList.add(s);

					} else if (title.equals("マニホールド仕様書")) {
						manifoldList.add(s);

					} else if (title.equals("自己宣言書")) { // HeartCore
						ceList.add(s);

					} else {
						button+="    <li class=\"cat_iodd\"><a href=\""+s.getUrl()+"\">"+s.getLinkMaster().getName()+"</a></li>\r\n" ;
					}
				}
			}
			// === 2D or 2D/3DCAD ===
			List<String[]> links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 0);
			if (links != null && links.size() > 0 || series.isCad3d()) {
			//if (cad2DList.size() > 0) {
				tabTitles.add("2DCAD");
				String tab ="";
				if (series.isCad3d()) {
					if (lang.equals("ja-jp")) {
//3/1						tab+= _static_seriesCad3d.replace("$$$se_id$$$", series.getModelNumber());
						tab+= _static_seriesCad3d_202603.replace("$$$se_id$$$", series.getModelNumber());
						
					} else if (lang.indexOf("en-") > -1) {
//3/1						tab+= _static_seriesCad3d_E.replace("$$$se_id$$$", series.getModelNumber());
						tab+= _static_seriesCad3d_202603_E.replace("$$$se_id$$$", series.getModelNumber());
					} else if (lang.equals("zh-tw")) {
						String sid = series.getModelNumber();
						if (sid != null && sid.isEmpty() == false) sid = sid.replace("-ZHTW", "-E");
						tab+= _static_seriesCad3d_ZHTW.replace("$$$se_id$$$", sid);
					} else if (lang.indexOf("zh-") > -1) {
						tab+= _static_seriesCad3d_ZH.replace("$$$se_id$$$", series.getModelNumber());
					}

				}
				// 2DCADはJSONで保持
				// ページは/webcatalog/2dcad/ja-jp/{se_id}で描画。ProductsRestController
				if (links != null && links.size() > 0) {
					if (lang.equals("ja-jp")) {
						tab += _seriesCad2d.replace("$$$cad2dLink$$$",AppConfig.Page2DCADUrl+series.getModelNumber());
					} else if (lang.indexOf("en-") > -1) {
						tab += _seriesCad2d_E.replace("$$$cad2dLink$$$",AppConfig.Page2DCADUrl.replace("/ja-jp/", "/"+lang+"/")+series.getModelNumber());
					} else if (lang.equals("zh-tw")) {
						String sid = series.getModelNumber();
						if (sid != null && sid.isEmpty() == false) sid = sid.replace("-ZHTW", "-E");
						tab += _seriesCad2d_ZHTW.replace("$$$cad2dLink$$$",AppConfig.Page2DCADUrl.replace("/ja-jp/", "/"+lang+"/")+sid);
					} else if (lang.indexOf("zh-") > -1) {
						tab += _seriesCad2d_ZH.replace("$$$cad2dLink$$$",AppConfig.Page2DCADUrlZH.replace("/zh-cn/", "/"+lang+"/")+series.getModelNumber());
					}
				}
				/*if (lang.equals("ja-jp")) {
					tab+= _seriesCad2d.replace("$$$cad2dLink$$$", AppConfig.Page2DCADUrl+series.getModelNumber());
				} else if (lang.indexOf("en-") > -1) {
					tab+= _seriesCad2d_E.replace("$$$cad2dLink$$$", AppConfig.Page2DCADUrl.replace("/ja/", "/en/") +series.getModelNumber());
				} else if (lang.indexOf("zh-") > -1) {
					tab+= _seriesCad2d_ZH.replace("$$$cad2dLink$$$", AppConfig.Page2DCADUrl.replace("/ja/", "/zh/") +series.getModelNumber());
				}*/
				tab = getTabString(series.getModelNumber(), tabCount, tab);
				tabBodyList.add(tab);
				tabCount++;
			}

			// === 取説 ===
			links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 1);
			if (links != null && links.size() > 0) {
//			if (manualList.size() > 0) { // HeartCore
				String manual = "取扱説明書";
				if (lang.indexOf("en-") > -1) manual = "Manual";
				else if (lang.equals("zh-tw")) manual = "說明書";
				else if (lang.indexOf("zh-") > -1) manual = "说明书";
				String searchresult = "検索結果を表示";
				if (lang.indexOf("en-") > -1) searchresult = "View search result";
				else if (lang.indexOf("zh-tw") > -1) searchresult = "查看搜尋結果";
				else if (lang.indexOf("zh-") > -1) searchresult = "查看搜索结果";
				tabTitles.add(manual);

				String tab = "<h5>" + series.getName()+ "&nbsp;"+ series.getNumber() +  "</h5>\r\n" +
					"<h5>"+manual+"</h5>\r\n" +
					"<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
					"<tbody>\r\n" ;

				// 取説はJSONで保持
				tab += "<tr>\r\n" ;
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					if (cnt == titles.length-1) {
						tab +=  "<th class=\"last\">"+ title + "</th>\r\n" ;
					} else {
						tab +=  "<th scope=\"col\">"+ title + "</th>\r\n" ;
					}
					cnt++;
				}
				tab += "</tr>\r\n";
				for (int i = 1; i < links.size(); i++) {
					tab += "<tr>\r\n";
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							tab += "<td>";
							if (val != null && val.isEmpty() == false) {
								if (val.indexOf("/manual/") > -1) { // 2023/6/22manuals本番アップで要置き換え
									val = val.replace("/manual/", "/manuals/");
									if (lang.indexOf("en-") > -1) {
										val = val.replace("/en/", "/" + lang + "/");
									} else if (lang.indexOf("zh-") > -1) {
										val = val.replace("/zh/", "/" + lang + "/");
										val = val.replace("/en/", "/" + lang + "/");
									} else {
										val = val.replace("/ja/", "/ja-jp/");
									}
									val = val.replace("/s.do?k=", "/search?query=");
									val = val.replace("/?k=", "/search?query=");
								}
								tab += "<a class=\""+buttonClass+"\" target=\"_blank\" href=\""+val+"\">"+searchresult+"</a>";
							}
							tab +="</td>";
						} else {
							tab += "<td>" + val + "</td>";
						}
						cnt++;
					}
				}
				tab += "</tr>\r\n";

				tab += "</tbody></table>";
				tab = getTabString(series.getModelNumber(),tabCount, tab);
				tabBodyList.add(tab);
				tabCount++;
			}

			// === CE ===
			links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 3);
			if (links != null && links.size() > 0) {
				String doc = "自己宣言書";
				if (lang.indexOf("en-") > -1) doc = "DoC/IM";
				else if (lang.indexOf("zh-tw") > -1) doc = "自我宣告書";
				else if (lang.indexOf("zh-") > -1) doc = "自我宣告书";
				String searchresult = "検索結果を表示";
				if (lang.indexOf("en-") > -1) searchresult = "View search result";
				else if (lang.indexOf("zh-tw") > -1) searchresult = "查看搜尋結果";
				else if (lang.indexOf("zh-") > -1) searchresult = "查看搜索结果";

				tabTitles.add(doc);
				String tab = "<h5>" + series.getName()+ "&nbsp;"+ series.getNumber().trim() +  "</h5>\r\n" +
						"<h5>"+doc+"</h5>\r\n" +
						"<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
						"<tbody>";
				tab += "<tr>\r\n" ;
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					if (cnt == titles.length-1) {
						tab +=  "<th class=\"last\">"+ title + "</th>\r\n" ;
					} else {
						tab +=  "<th scope=\"col\">"+ title + "</th>\r\n" ;
					}
					cnt++;
				}
				tab += "</tr>\r\n";
				for (int i = 1; i < links.size(); i++) {
					tab += "<tr>\r\n";
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							tab += "<td>";
							if (StringUtils.isEmpty(val) == false) {
								tab += "<a class=\""+buttonClass+"\" target=\"_blank\" href=\""+val+"\">"+searchresult+"</a>";
							}
							tab +="</td>";
						} else {
							tab += "<td>" + val + "</td>";
						}
						cnt++;
					}
				}
				tab += "</tr>\r\n";
//			// 同じカテゴリの一覧では同じ頭文字のシリーズが多いため、一覧を利用
//			if (ceList.size() > 0) { // HeartCore
/*				tabTitles.add(messagesource.getMessage("tab.title.doc", null,  locale));
				String sNumber = series.getNumber().trim();
				String sModelNumber = series.getModelNumber().trim();
				String tab = "<h5>" + series.getName()+ "&nbsp;"+ sNumber +  "</h5>\r\n" +
						"<h5>"+messagesource.getMessage("tab.title.doc", null,  locale)+"</h5>\r\n" +
						"<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
						"<tbody><tr>\r\n" ;
				if (specList.size() > 0) tab+="<th >"+messagesource.getMessage("g.series", null,  locale)+"</th>\r\n" ;
				tab+="<th >"+messagesource.getMessage("button.download", null,  locale)+"</th>\r\n" +
						"</tr>\r\n";

				SeriesLink sl = ceList.get(0);
				tab+=getCESpecListHtml(sNumber, sModelNumber, sl.url, lang, specList);
*/
				tab += "</tbody></table>";
				tab = getTabString(series.getModelNumber(),tabCount, tab);
				tabBodyList.add(tab);
				tabCount++;
			}

			// === マニホールド ===
			links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 2);
			if (links != null && links.size() > 0) {
			//if (manifoldList.size() > 0) {

				String manifold = "マニホールド仕様書";
				if (lang.indexOf("en-") > -1) manifold = "Manifold";
				else if (lang.indexOf("zh-tw") > -1) manifold = "歧管規格";
				else if (lang.indexOf("zh-") > -1) manifold = "集装底座";
				String searchresult = "検索結果を表示";
				if (lang.indexOf("en-") > -1) searchresult = "View search result";
				else if (lang.indexOf("zh-tw") > -1) searchresult = "查看搜尋結果";
				else if (lang.indexOf("zh-") > -1) searchresult = "查看搜索结果";

				tabTitles.add(manifold);
				String tab = "<h5>" + series.getName()+ "&nbsp;"+ series.getNumber() +  "</h5>\r\n" +
					"<h5>"+manifold+"</h5>\r\n" +
					"<table cellpadding=\"0\" cellspacing=\"0\" class=\"resulttbl\">\r\n" +
					"<tbody>\r\n" ;
				tab += "<tr>\r\n" ;
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					if (cnt == titles.length-1) {
						tab +=  "<th class=\"last\">"+ title + "</th>\r\n" ;
					} else {
						tab +=  "<th scope=\"col\">"+ title + "</th>\r\n" ;
					}
					cnt++;
				}
				tab += "</tr>\r\n";
				for (int i = 1; i < links.size(); i++) {
					tab += "<tr>\r\n";
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							tab += "<td>";
							if (StringUtils.isEmpty(val) == false) {
								tab += "<a class=\""+buttonClass+"\" target=\"_blank\" href=\""+val+"\">"+searchresult+"</a>";
							}
							tab +="</td>";
						} else {
							tab += "<td>" + val + "</td>";
						}
						cnt++;
					}
				}
				tab += "</tr>\r\n";
/*				if (specList.size() > 0) tab+="<th >"+messagesource.getMessage("g.series", null,  locale)+"</th>\r\n" ;
				tab+="<th >"+messagesource.getMessage("button.download", null,  locale)+"</th>\r\n" +
				"</tr>\r\n";
				for (String sp : specList) {
					boolean isFind  = false;
					for(SeriesLink mani : manifoldList) {
						if (sp.trim().equals(mani.getModelNumber().trim()) ) {
							isFind = true;
							tab+="<tr>\r\n<td>" + sp + "</td><td>";
							tab+="<a class=\""+buttonClass+"\" href=\""+mani.url+"\" target=\"_blank\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
							tab+="</td></tr>\r\n";
							break;
						} else if (mani.getUrl().indexOf("="+sp) > 0) {
							String[] arr = mani.getUrl().split("=");
							if (arr.length == 2 && arr[1].equals(sp.trim())) {
								isFind = true;
								tab+="<tr>\r\n<td>" + sp + "</td><td>";
								tab+="<a class=\""+buttonClass+"\" href=\""+mani.url+"\" target=\"_blank\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
								tab+="</td></tr>\r\n";
								break;
							}
						}
					}
					if (isFind == false) {
						// 何も引っ掛からなかった場合は入力されたURLを表示
						SeriesLink sl = manifoldList.get(0);
						tab+="<tr>\r\n<td>" + sp + "</td><td>";
						tab+="<a class=\""+buttonClass+"\" href=\""+sl.url+"\" target=\"_blank\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
						tab+="</td></tr>\r\n";
					}
				}
*/
				tab += "</tbody></table>";
				tab = getTabString(series.getModelNumber(),tabCount, tab);
				tabBodyList.add(tab);
				tabCount++;
			}

			// === 簡易特注 ===
			if (series.isCustom()) {
				String str = "簡易特注";
				if (lang.indexOf("en-") > -1) str = "Simple Specials";
				else if (lang.equals("zh-tw")) str = "簡易特注";
				else if (lang.indexOf("zh-") > -1) str = "简易特注";

				tabTitles.add(str);

				String div = "簡易特注";
				if (lang.indexOf("en-") > -1) div = "Simple Specials";
				// TODO 中国語
				List<Omlist> omlist = omlistService.searchKeyword(null, null, div, series.getModelNumber(), lang);
				String tab = omlistService.getTableHtml(omlist, lang);
				if (lang.equals("ja-jp")) tab = _CustomJa+tab;
				tab = "<h5>" + series.getName()+ "&nbsp;"+ series.getNumber() +  "</h5>" + tab ;
				tab = getTabString(series.getModelNumber(),tabCount, tab);
				tabBodyList.add(tab);
				tabCount++;

			}
			// === オーダーメイド ===
			if (series.isOrderMade()) { // オーダーメイド
				String str = "オーダーメイド";
				if (lang.indexOf("en-") > -1) str = "Made to Order";
				else if (lang.indexOf("zh-") > -1) str = "订制规格";

				tabTitles.add(str);

				String div = "オーダーメイド";
				if (lang.indexOf("en-") > -1) div = "Made to Order Common Specifications";
				else if (lang.indexOf("zh-") > -1) div = "订制规格";

				List<Omlist> omlist = omlistService.searchKeyword(null, null, div, series.getModelNumber(), lang);
				String tab = omlistService.getTableHtml(omlist, lang);
				if (lang.equals("ja-jp")) {
					tab = "<h5>" + series.getName()+ "&nbsp;"+ series.getNumber() +  "</h5>" + _OrderMadeJa + tab ;
				} else {
					tab = "<h5>" + series.getName()+ "&nbsp;"+ series.getNumber() +  "</h5>" + tab ;
				}
				tab = getTabString(series.getModelNumber(),tabCount, tab);
				tabBodyList.add(tab);
				tabCount++;
			}
			if (tabCount > 1) {
				String title = "";
				String mn = series.getModelNumber();
				if (mn != null && mn.indexOf("/") > -1) mn = mn.replace("/", "_");
				if (mn != null && mn.indexOf("(") > -1) mn = mn.replace("(", "_").replace(")", "_");
				for(int i = 1; i < tabCount; i++) {
					String tabTitle = tabTitles.get(i-1);
					if (tabTitle.equals("2DCAD")) tabTitle = "CAD";
					if (i == 1) title += "    <li id=\"tab"+i+"\" class=\"w_tab active\"><a href=\"#cont"+mn+"_"+String.format("%02d", i)+"\" class=\"tab\">"+tabTitle+"</a></li>\r\n";
					else title += "    <li id=\"tab"+i+"\" class=\"w_tab\"><a href=\"#cont"+mn+"_"+String.format("%02d", i)+"\" class=\"tab\">"+tabTitle+"</a></li>\r\n";
				}
				String body = "";
				for(String str : tabBodyList) {
					body += str;
				}

				ret = StringUtils.replace(ret, "$$$tabs$$$", _tabHead.replace("$$$tabTitle$$$", title) + body + _tabFoot);
			} else {
				ret = StringUtils.replace(ret, "$$$tabs$$$","");
			}
			ret = StringUtils.replace(ret, "$$$button$$$", button);
		} else {
			ret = StringUtils.replace(ret, "$$$tabs$$$","");
			ret = StringUtils.replace(ret, "$$$button$$$", "");
		}

		String other = series.getOther();
		// 20210924 削除指示
		/*
		if (other.indexOf("href=\"/etc/rohs") > 0){

			if (lang.equals("ja-jp")) {
				ret = StringUtils.replace(ret, "$$$rohs$$$", "<a href=\"/etc/rohs/ja-jp/search.html\" target=\"_blank\" class=\"rohsBt\">RoHS対応製品</a>\r\n");
			} else if (lang.indexOf("en-") > -1) {
				ret = StringUtils.replace(ret, "$$$rohs$$$", "<a href=\"/etc/rohs/en-jp/search.html\" target=\"_blank\" class=\"rohsBt\" style=\"font-size:10px;\">Green Procurement (RoHS)</a>\r\n");
			} else if (lang.indexOf("zh-") > -1) {
				ret = StringUtils.replace(ret, "$$$rohs$$$", "<a href=\"/etc/rohs/zh-cn/search.html\" target=\"_blank\" class=\"rohsBt\">洁净对应（RoHS）</a>\r\n");
			}
		} else {
			ret = StringUtils.replace(ret, "$$$rohs$$$","");
		}
		*/
		ret = StringUtils.replace(ret, "$$$rohs$$$","");

		// /assets/js/global.jsのgetLoginData()でログイン描画させる。
		// 中国語はマイリスト無し。ja-jp en-jpのみ (en-sg等も要らない。)
		if (isGuide) {
			ret = StringUtils.replace(ret, "$$$mylist$$$", "");
		}
		else if (lang.equals("ja-jp") ) {
			// ログアウト時
			String mylist = "<div class=\"logout_state hidden\" ><a href=\"/customer/ja/tologin.do?dst="+url+"\" class=\"myList set_back_url\"><span>マイリストに追加</span></a></div>\r\n";
			// ログイン時
			mylist += "<div class=\"login_state hidden\" >\r\n"+
			"<a href=\"/mylist/ja/add.do?mode=ADD&sid=$$$sid$$$&KeepThis=true&TB_iframe=true&height=190&width=500\" onclick=\"return false;\" id=\"mylist_$$$sid$$$\" class=\"thickbox to_mylist bt_show_mylistwin hidden to_mylist_$$$sid$$$\"><span>マイリストに追加</span></a>\r\n" +
			"    <a href=\"#\" onclick=\"add_mylist_sid('$$$sid$$$', 'ja');return false;\" class=\"myList bt_hide_mylistwin hidden to_mylist_$$$sid$$$\"><span>マイリストに追加</span></a>\r\n" +
			"	<a href=\"#\" onclick=\"return false;\" class=\"end_mylist hidden\" id=\"end_mylist_$$$sid$$$\"><span>マイリスト追加済</span></a>" +
			"</div>\r\n";
			ret = StringUtils.replace(ret, "$$$mylist$$$", mylist.replace("$$$sid$$$", series.getModelNumber()));
		} else if (lang.equals("en-jp")) {
			// ログアウト時
			String mylist = "<div class=\"logout_state hidden\" ><a href=\"/customer/en/tologin.do?dst="+url+"\" class=\"myList set_back_url\"><span>Add to My List</span></a></div>\r\n";
			// ログイン時
			mylist += "<div class=\"login_state hidden\" >\r\n"+
			"<a href=\"/mylist/en/add.do?mode=ADD&sid=$$$sid$$$&KeepThis=true&TB_iframe=true&height=190&width=500\" onclick=\"return false;\" id=\"mylist2_$$$sid$$$\" class=\"thickbox to_mylist bt_show_mylistwin hidden to_mylist_$$$sid$$$\"><span>Add to My List</span></a>\r\n" +
			"    <a href=\"#\" onclick=\"add_mylist_sid('$$$sid$$$', 'en');return false;\" class=\"myList bt_hide_mylistwin hidden to_mylist_$$$sid$$$\"><span>Add to My List</span></a>\r\n" +
			"	<a href=\"#\" onclick=\"return false;\" class=\"end_mylist hidden\" id=\"end_mylist_$$$sid$$$\"><span>Added to My List</span></a>" +
			"</div>\r\n";
			ret = StringUtils.replace(ret, "$$$mylist$$$", mylist.replace("$$$sid$$$", series.getModelNumber()));
		} else {
			ret = StringUtils.replace(ret, "$$$mylist$$$", "");
		}

		ret = StringUtils.replace(ret, "$$$other$$$", other);

		ret = StringUtils.replace(ret, "$$$title$$$", series.getName()+"&nbsp;"+series.getNumber());

		String tmp = "";
		if (isGuide) {
			// ガイドはカテゴリの一覧をパンくずで表示
			List<String> catpan = series.getCatpansHtml(series.getLang());
			tmp = "<ul  class=\"catpan_box\">";
			for(String str : catpan) {
				tmp+= "<li>"+str + "</li>";
			}
			tmp += "</ul>";

		}
		else if (StringUtils.isEmpty(series.getNotice()) == false)
		{
			tmp = "<p class=\"note\">" + series.getNotice() + "</p>";
		}
		ret = StringUtils.replace(ret, "$$$notice$$$", tmp);
		tmp = "";
		if (StringUtils.isEmpty(series.getImageTop()) == false) tmp = series.getImageTop();
		ret = StringUtils.replace(ret, "$$$imageTop$$$",tmp);
		tmp = "";
		if (StringUtils.isEmpty(series.getImageBottom()) == false) tmp = series.getImageBottom();
		ret = StringUtils.replace(ret, "$$$imageBottom$$$",tmp);

		// ・中黒の<li>化中止。2022/3/3
/*
		String[] arr = series.getDetail().split("・");
		tmp = "";
		int cnt  = 0;
		for(String s : arr) {
			if (cnt != 0) {
				tmp += "<li>"+ s.trim().replace("\r\n", "<br>").replace("　", "") + "</li>\r\n";
			}
			cnt++;
		}
		ret = StringUtils.replace(ret, "$$$details$$$", tmp);
*/
		String detail = series.getDetail().replace("\r\n", "<br>");
		ret = StringUtils.replace(ret, "$$$details$$$", detail);

		// 製品特長 $$$cate2$$$ $$$advantageBody$$$ $$$backUrl$$$
		if  (isGuide == false && isAdvantage) {
			if (c2 != null) {
				ret = StringUtils.replace(ret, "$$$cate2$$$", c2.getName());
			} else {
				ret = StringUtils.replace(ret, "$$$cate2$$$","");
			}
			if (series.getAdvantage().isEmpty() == false) {

				adv = series.getAdvantage();
				if (adv.trim().indexOf("@@@") == 0) {
					String[] arr2 = adv.split("@@@");
					if (arr2.length >= 2) {
						try {
							int page = Integer.parseInt(arr2[1]);
							adv = LibOkHttpClient.getHttpsHtml(AppConfig.PageCDNIdUrl + page, AppConfig.BasicAuthCDNID, AppConfig.BasicAuthCDNPW);
							String[] arr3 = StringUtils.splitByWholeSeparator(adv, "<body>");
							if (arr3.length >= 2) {
								String[] arr4 = StringUtils.splitByWholeSeparator(arr3[1], "</body>");
								adv = arr4[0];
							}
						} catch (Exception e) {
							log.error("@@@xxx@@@ parse error. advantage = " + adv.toString());
							adv = "";
						}
					}
				} else if (adv.trim().indexOf("/") == 0) {
					String h = LibOkHttpClient.getHttpsHtml(AppConfig.PageCDNIdUrl + adv.trim());
					if (h == null || h.isEmpty()) {
						adv = LibOkHttpClient.getHtml(AppConfig.PageProdUrl + adv.trim());
					} else {
						adv = h;
					}
				} else {
					adv = LibOkHttpClient.getHtml( adv.trim());
				}
				ret = StringUtils.replace(ret, "$$$advantageBody$$$", adv);
			} else {
				ret = StringUtils.replace(ret, "$$$advantageBody$$$", "");
			}
			//ret = StringUtils.replace(ret, "$$$backUrl$$$", "/"); // そのまま置いておく。表示時に選択

		} else {
			if (c2 != null) {
				ret = StringUtils.replace(ret, "$$$cate2$$$", c2.getName());
			} else {
				ret = StringUtils.replace(ret, "$$$cate2$$$","");
			}
			ret = StringUtils.replace(ret, "$$$advantageBody$$$", "");
		}
		String str = "一覧へ戻る";
		if (lang.indexOf("en-") > -1) str = "Back to list";
		else if (lang.indexOf("zh-") > -1) str = "返回";

		ret = StringUtils.replace(ret, "$$$backUrlMessage$$$", str);
		
		// <ul class="pro_service_bt">
		if (lang.equals("ja-jp")) {
			String message = _CheckLoginJa; // ログイン必須 // 3/1
			ret = ret.replace("<ul class=\"pro_service_bt\">", message+"<br><ul class=\"pro_service_bt\">");
		} else if (lang.indexOf("en-") > -1)  {
			String message = _CheckLoginEn; // ログイン必須 // 3/1
			ret = ret.replace("<ul class=\"pro_service_bt\">", message+"<br><ul class=\"pro_service_bt\">");
		}

		return ret;
	}

	private static final String _advBtn = "<div class=\"f fc mt16\">\r\n"
			+ "      <a class=\"button large primary solid w264 gap-8 s-w-full m-w-full\" href=\"###\">\r\n"
			+ "        <span>$$$advText$$$</span>\r\n"
			+ "        <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-right-white.svg\" alt=\"\" title=\"\">\r\n"
			+ "      </a>\r\n"
			+ "    </div>";
	/**
	 *
	 * @param series
	 * @param c null OK (nullの場合はbreadClumbを全部出す。)
	 * @param c2 null OK
	 * @param url  for mylist
	 * @param lang
	 * @param isAdvantage true:製品特長を表示
	 * @param isGuide true:ガイド用、リスト表示＋ページ＋カテゴリ全表示
	 * @return guide HTML
	 */
	public String getGuide2026(Series series, Category c, Category c2, String url, String lang, boolean isAdvantage, boolean isGuide)
	{
		String ret = "";
		if  (isAdvantage) ret = _seriesTemplate2026;
		else if  (isGuide) ret = _seriesListTemplate2026; // InternalApiRestController.getHeartCoreGuideでreplace有り。変更時は注意！
		else  ret = _seriesListTemplate2026;

		// 画像 製品特長button
		String img = "";
		String adv = "";
		if (series.getImage() != null && series.getImage().isEmpty() == false) {
			img = AppConfig.ImageProdUrl + series.getLang() + "/" + series.getImage();
			if (lang != null && lang.equals("zh-cn")) img = AppConfig.ImageProdPath + series.getLang() + "/" + series.getImage();
			if  (isAdvantage) img = "    <img src=\"" + img + "\" class=\"flex-fixed object-fit-contain w-full\" >\r\n" ;
			else img = "    <img src=\"" + img + "\" class=\"flex-fixed object-fit-contain w264\" >\r\n" ;
		}
		ret = StringUtils.replace(ret, "$$$image$$$", img);
		
		if (isGuide == false && series.getAdvantage().isEmpty() == false) {
			String advText = "製品特長";
			if (lang.indexOf("en-") > -1) advText = "Features";
			else if (lang.equals("zh-tw")) advText = "產品特點";
			else if (lang.indexOf("zh-") > -1) advText = "产品特点";

			String advBtn = _advBtn;
			if (isAdvantage == false) {
				String advLink = "";
				if (c2 != null) {
					advLink = AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/" + c2.getSlug() + "/"+ series.getModelNumber();
					advBtn = advBtn.replace("###",  advLink + "/#detail\"");
					advBtn = advBtn.replace("$$$advText$$$",  advText);
				} else if (c != null) {
					advLink = AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/series/" + series.getModelNumber();
					advBtn = advBtn.replace("###", advLink + "/#detail\"");
					advBtn = advBtn.replace("$$$advText$$$",  advText);
				} else {
					// 両方Nullは検索結果等。catpanから取得
					
				}
				ret = StringUtils.replace(ret, "$$$titleLink$$$", advLink);
			} else {
				String advLink = "";
				if (c2 != null) {
					advLink = AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/" + c2.getSlug() + "/"+ series.getModelNumber();
				} else if (c != null) {
					advLink = AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/series/" + series.getModelNumber();
				}
				ret = StringUtils.replace(ret, "$$$titleLink$$$", advLink);
			}
			ret = StringUtils.replace(ret, "$$$advantageButton$$$", advBtn);
			ret = StringUtils.replace(ret, "$$$advantage$$$", adv);
			ret = StringUtils.replace(ret, "$$$advantageTitle$$$", advText);
		} else {
			ret = StringUtils.replace(ret, "$$$advantageButton$$$", "");
			ret = StringUtils.replace(ret, "$$$advantage$$$", adv);
			ret = StringUtils.replace(ret, "$$$advantageTitle$$$", "");
			String advLink = "";
			if (c2 != null) {
				advLink = AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/" + c2.getSlug() + "/"+ series.getModelNumber();
			} else if (c != null) {
				advLink = AppConfig.ProdRelativeUrl+ series.getLang()+"/"+ c.getSlug() + "/series/" + series.getModelNumber();
			}
			ret = StringUtils.replace(ret, "$$$titleLink$$$", advLink);
			isAdvantage = false; // 要求があっても無ければfalseに。タイトルを表示させない。
		}
		// Title Replace
		if (series.getName() != null) {
			String head = series.getName().substring(0, 1);
			String aft = series.getName().substring(1) + "&nbsp;" + series.getNumber();
			ret = ret.replace("$$$title21$$$", head);
			ret = ret.replace("$$$title22$$$", aft);
			ret = StringUtils.replace(ret, "$$$title$$$", series.getName()+"&nbsp;"+series.getNumber());
		}

		// 特長、製品サポートへのページ内リンク。2026/4/16削除決定。コメントアウト
//		String supportTitle = "製品サポート";
//		if (lang.indexOf("en-") > -1) supportTitle = "product support";
//		else if (lang.equals("zh-tw")) supportTitle = "产品支持";
//		else if (lang.indexOf("zh-") > -1) supportTitle = "產品支援";
//		ret = ret.replace("$$$supportTitle$$$", supportTitle);
		
		// 品番確認、カタログPDFをログイン必須に。
		// 品番確認の右はカタログPDF。FAQは通常の先頭へ

		// button tab
		String button = "";
		String strLoginButton = _CheckLoginJa2026; // ログイン必須 // 3/1
		if (lang.indexOf("en-") > -1)  {
			strLoginButton = _CheckLoginEn2026; // ログイン必須 // 3/1
		}
		
		List<SeriesLink> list = series.getLink();
		if (list != null && list.size() > 0) {
			String buttonClass = "downloadBt";
			if (lang != null && lang.indexOf("en-") > -1) buttonClass += "_E";
			List<String> tabTypes = new LinkedList<String>(); //  tab切り替え用data-id
			List<String> tabTitles = new LinkedList<String>();
			List<String> tabHeadList = new LinkedList<String>();
			List<String> tabBodyList = new LinkedList<String>();

			List<SeriesLink> cad2DList = new LinkedList<SeriesLink>();
			List<SeriesLink> manualList = new LinkedList<SeriesLink>();
			List<SeriesLink> manifoldList = new LinkedList<SeriesLink>();
			List<SeriesLink> ceList = new LinkedList<SeriesLink>();

			// 3S
			String specialButton = "";
			// /webcatalog/s3s/ja-jp/check/JSY-P
			if (lang.equals("zh-tw")) {
				// 台湾は中国が繁体語はやらなそうなので、とりあえず、簡体語。2023/6/8
				String sid = series.getModelNumber().trim();
				if (sid != null && sid.isEmpty() == false) sid = sid.replace("ZHTW", "ZH");
				specialButton += "<button class=\"button solid gap-8 w-full primary  medium\" type=\"button\" onclick=\"window.open('"+AppConfig.PageProdUrl+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+sid+"');\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check-white.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">型號確認</span></button>";
			} else if (lang.indexOf("zh-") > -1) {
				specialButton += "<button class=\"button solid gap-8 w-full primary  medium\" type=\"button\" onclick=\"window.open('"+AppConfig.PageProdUrlZH+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+series.getModelNumber().trim()+"');\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check-white.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">型号确认</span></button>";
			} else if  (lang.indexOf("en-") > -1) {
				specialButton += "<button class=\"button solid gap-8 w-full primary  medium\" type=\"button\" onclick=\"window.open('"+AppConfig.PageProdUrl+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+series.getModelNumber().trim()+"');\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check-white.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">Product Number Check</span></button>";
			} else {
				specialButton += "<button class=\"button solid gap-8 w-full primary  medium\" type=\"button\" onclick=\"window.open('"+AppConfig.PageProdUrl+AppConfig.ContextPath+"/s3s/"+lang+"/list/"+series.getModelNumber().trim()+"');\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file-check-white.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">品番確認</span></button>";
			}
			// カタログPDF Add 20260130
			boolean isCatalogPDF = false;
			SeriesLink linkCatalogPDF = null;
			for(SeriesLink sl : list) {
				String title = sl.getLinkMaster().getTitle();
				if (title != null && title.equals("カタログ閲覧")
						&& sl.getUrl().isEmpty() == false) {
					isCatalogPDF = true;
					linkCatalogPDF = sl;
					break;
				}
			}
			if (isCatalogPDF && linkCatalogPDF != null) {
				// 品番確認の次に表示
				String strUrl = linkCatalogPDF.getUrl();
				String title = linkCatalogPDF.getLinkMaster().getName();
				if (lang.equals("zh-tw")) {
					specialButton += "<button class=\"button solid gap-8 w-full secondary  medium\" type=\"button\" onclick=\"window.open('"+strUrl+"')\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\""+title+"\" title=\""+title+"\"><span class=\"text-sm s-leading-tight m-leading-tight\">"+title+"</span></button>";
				} else if (lang.indexOf("zh-") > -1) {
					specialButton += "<button class=\"button solid gap-8 w-full secondary  medium\" type=\"button\" onclick=\"window.open('"+strUrl+"')\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\""+title+"\" title=\""+title+"\"><span class=\"text-sm s-leading-tight m-leading-tight\">"+title+"</span></button>";
				} else if  (lang.indexOf("en-") > -1) { 
					specialButton += "<button class=\"button solid gap-8 w-full secondary  medium\" type=\"button\" onclick=\"window.open('"+strUrl+"')\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\""+title+"\" title=\""+title+"\"><span class=\"text-sm s-leading-tight m-leading-tight\">"+title+"</span></button>";
				} else {
					title = "カタログダウンロード(PDF)";
					specialButton += "<button class=\"button solid gap-8 w-full secondary  medium\" type=\"button\" onclick=\"window.open('"+strUrl+"')\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/file.svg\" alt=\""+title+"\" title=\""+title+"\"><span class=\"text-sm s-leading-tight m-leading-tight\">"+title+"</span></button>";
				}
				if (lang.equals("ja-jp") || lang.equals("en-jp")) { // デジタルカタログは日本語、英語のみ
					String catalog = "デジタルカタログ";
					if (lang.equals("en-jp")) catalog="Digital catalog";
					String href = linkCatalogPDF.getUrl();
					int start = href.indexOf("/data/");
					if (start > -1) {
						href = href.substring(0,start) + "/index.html";
						specialButton += "<button class=\"button secondary solid large gap-8 w-full medium\" onclick=\"window.open('"+href+"')\"><img class=\"object-fit-contain s16\" src=\"/assets/smcimage/common/book.svg\" alt=\"\" title=\"\"><span class=\"flex-fixed text-sm leading-tight\">"+catalog+"</span></button>";
					} else {
						start = href.indexOf("/6-");
						if (start > -1) {
							href = href.replace("/index.pdf", "/index.html");
							href = href.replace("/pdf/catalog/", "/catalog/");
							specialButton += "<button class=\"button secondary solid large gap-8 w-full medium\" onclick=\"window.open('"+href+"')\"><img class=\"object-fit-contain s16\" src=\"/assets/smcimage/common/book.svg\" alt=\"\" title=\"\"><span class=\"flex-fixed text-sm leading-tight\">"+catalog+"</span></button>";
						}
					}
				}
			} else {
				// カタログPDFが無ければ上の3Sボタンの右には何も表示しない。
				specialButton += "<br>\r\n";
			}
			if (lang.indexOf("zh-") > -1) {
				strLoginButton = specialButton;
			} else {
				strLoginButton = strLoginButton.replace("$$$specialButton$$$", specialButton);
			}
			ret = ret.replace("$$$isLoginButton$$$", strLoginButton);

			// マニホールドコンフィグレータ Add 20240314
			boolean isManifoldConfigrator = false;
			SeriesLink linkManifoldConfigrator = null;
			for(SeriesLink sl : list) {
				String title = sl.getLinkMaster().getTitle();
				if (title != null && title.equals("マニホールドコンフィグレータ")
						&& sl.getUrl().isEmpty() == false) {
					isManifoldConfigrator = true;
					linkManifoldConfigrator = sl;
					break;
				}
			}
			// FAQ 2023/8/3 Add
			// FAQは通常の先頭へ 2026/1/30
			Optional<SeriesFaq> oFaq = faqRepo.findBySeriesId(series.getId());
			if (oFaq.isPresent()) {
				SeriesFaq faq = oFaq.get();
				if (faq.getFaq() != null && faq.getFaq().isEmpty() == false) {
					// 2023/8/17 フルパスのリンクも可能に。数字で始まらない場合はフルパスリンク
					String strFaq = faq.getFaq().trim();
					String strTitle = "FAQ ～よくあるご質問～";
					if (lang.equals("zh-tw")) {
						strTitle = "FAQ";
					} else if (lang.indexOf("zh-") > -1) {
						strTitle = "常见问题解答(FAQ)";
					} else if  (lang.indexOf("en-") > -1) {
						strTitle = "FAQ";
					}
					char ch = strFaq.charAt(0);
					if (ch >= '0' && ch <= '9') {
						button += "<button class=\"button danger ghost large fh gap-8 w-full border border-destructive medium\" onclick=\"window.open('"+AppConfig.PageProdUrl+AppConfig.FaqPath+"/"+lang+"/list/" +strFaq+"')\">"
								+ "<img class=\"object-fit-contain s16\" src=\"/assets/smcimage/common/fqa.svg\" alt=\"\" title=\"\">"
								+ "<span class=\"text-sm leading-tight\">"+strTitle+"</span>"
								+ "</button>";
					} else {
						button += "<button class=\"button danger ghost large fh gap-8 w-full border border-destructive medium\" onclick=\"window.open('"+strFaq+"')\">"
								+ "<img class=\"object-fit-contain s16\" src=\"/assets/smcimage/common/fqa.svg\" alt=\"\" title=\"\">"
								+ "<span class=\"text-sm leading-tight\">"+strTitle+"</span>"
								+ "</button>";
					}
				}
			}
			if (isManifoldConfigrator && linkManifoldConfigrator != null) {
				// FAQが無ければ品番確認の次に表示
				String strUrl = linkManifoldConfigrator.getUrl();
				if (lang.indexOf("zh-") > -1 || lang.indexOf("en-") > -1) {
					button += "<button class=\"button solid large gap-8 medium s-w-full m-w-full button-manifold\" type=\"button\" onclick=\"window.open('"+strUrl+"')\">\r\n"
							+ "  <span class=\"flex-fixed text-sm leading-tight\">Manifold Configrator</span>\r\n"
							+ "</button>";
				} else {
					button += "<button class=\"button solid large gap-8 medium s-w-full m-w-full button-manifold\" type=\"button\" onclick=\"window.open('"+strUrl+"')\">\r\n"
							+ "  <span class=\"flex-fixed text-sm leading-tight\">マニホールド品番確認</span>\r\n"
							+ "</button>";
				}
			}

			// 汎用ボタン
			for(SeriesLink s : list) {
				
				String icon = s.getLinkMaster().getIconClass();
				String title = s.getLinkMaster().getTitle();
				
				// 以下ボタンは上部で特別処理。
				if (title != null && title.equals("マニホールドコンフィグレータ")) continue;
				if (title != null && title.equals("カタログ閲覧")) continue;
				
				// 2026/4/17 icon CSS classがカラの場合、cat_ioddを出力していたが、「何も出力しない」に変更
				if (s.getLinkMaster().getType() == SeriesLinkType.ICON ) {
					if (icon == null || icon.isEmpty()) {
						button += "<button class=\"button secondary solid large gap-8 w-full medium\" onclick=\"window.open('"+s.getUrl()+"')\"><span class=\"flex-fixed text-sm leading-tight\">"+s.getLinkMaster().getName()+"</span></button>";
					} else if (icon.equals("cat_pdf")) {
						// PDFは上部で対応
					} else if (icon.isEmpty() == false){
						button += "<button class=\""+icon+" button secondary solid large gap-8 w-full medium\" onclick=\"window.open('"+s.getUrl()+"')\"><span class=\"flex-fixed text-sm leading-tight\">"+s.getLinkMaster().getName()+"</span></button>";
					} else {
						button += "<button class=\"button secondary solid large gap-8 w-full medium\" onclick=\"window.open('"+s.getUrl()+"')\"><span class=\"flex-fixed text-sm leading-tight\">"+s.getLinkMaster().getName()+"</span></button>";
					}
				} else {
					// DOWNLOAD series_linkが複数あるので、リストで保持。
					// 取説とCEはHeartCoreなので、１つでもあれば取得処理
					if (title.indexOf("2DCAD") > -1) {
						cad2DList.add(s);

					} else if (title.equals("取扱説明書")) { // HeartCore
						manualList.add(s);

					} else if (title.equals("マニホールド仕様書")) {
						manifoldList.add(s);

					} else if (title.equals("自己宣言書")) { // HeartCore
						ceList.add(s);

					} else {
						button += "<button class=\"button secondary solid large gap-8 w-full medium\" onclick=\"window.open('"+s.getUrl()+"')\"><span class=\"flex-fixed text-sm leading-tight\">"+s.getLinkMaster().getName()+"</span></button>";
					}
				}
			}
			
			// ===== tabs =====
			int tabCount = 1;
			// === Spec ===
			StringBuilder spec = new StringBuilder();
			List<String> specList = new LinkedList<String>(); // manifold調査用
			if (series.getSpec().isEmpty() == false && series.getSpec().equals("[]") == false) {
				tabTypes.add("spec");
				String title = "仕様";
				if  (lang.indexOf("en-") > -1) title = "Spec";
				else if  (lang.indexOf("zh-") > -1) title = "規格";
				tabTitles.add(title);
				
				StringBuilder sb = new StringBuilder();
				sb.append("<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"spec\">\r\n")
					.append( "    <div class=\"text-sm leading-tight fw5 white-space-nowrap text-primary\">").append(title).append("</div>\r\n")
					.append( "    <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: block;\"></div>\r\n")
					.append( "</button>");
				tabHeadList.add(sb.toString() ); // Specがデフォルト display: block;
				spec.append("<div class=\"js-tab-panel\" data-id=\"spec\" style=\"display: block;\">\r\n")
					.append( "  <div class=\"min-w-900\">\r\n")
					.append( "    <table class=\"table\">\r\n")
					.append( "      <thead>\r\n")
					.append( "        <tr>\r\n");

				int cnt = 0;
				int seriesIndex = 0;
				int cadIndex = -1;
				//ObjectMapper mapper = new ObjectMapper();
				try {
					String tmp = series.getSpec();
					JSONArray result = new JSONArray(tmp.replace("\r\n", "").replace("\t", ""));
					for (Object obj : result) {
						JSONArray arr = (JSONArray)obj;
						String celS= "<td class=\"td text-xs\">";
						String celE = "</td>";
						if (cnt == 0) {
							celS= "<th class=\"th\">";
							celE = "</th>";
						}
						int colCnt = 0;
						for(Object str: arr) {
							if (str != null && str.equals("[2DCAD]")) {
								cadIndex = colCnt;
								break;
							}
							if (colCnt == cadIndex) break;
							spec.append( celS).append(str).append(celE);
							if (str.equals("シリーズ") || str.equals("Model") || str.equals("系列")) {
								seriesIndex = colCnt;
								String temp = spec.toString().replace("<th>", "<th nowrap>"); 
								spec = new StringBuilder(temp);
							}
							if (cnt != 0 && colCnt == seriesIndex) specList.add(str.toString()); // cnt==0は「シリーズ」の文字
							colCnt++;
						}
						cnt++;
						if (cnt < result.length()) {
							if (cnt == 1) {
								spec.append( "    </tr>\r\n")
									.append( "</thead>")
									.append( "<tbody>")
									.append( "    <tr>\r\n" );
							} else {
								spec.append( "    </tr>\r\n<tr>" );
							}
						}
					}
				} catch (Exception e) {
					log.error("exception!! series="+series.getModelNumber() + " e.message="+e.getMessage());
				}

				spec.append("        </tr>\r\n" );
				spec.append("    </tbody>\r\n" );
				spec.append("</table>\r\n");
				spec.append("</div>\r\n");
				spec.append("</div>\r\n");
				
				tabBodyList.add(spec.toString());
				tabCount++;
			}

			// === 2D or 2D/3DCAD ===
			List<String[]> links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 0);
			if (links != null && links.size() > 0 || series.isCad3d()) {
				tabTypes.add("cad");
				tabTitles.add("CAD");
				
				StringBuilder sb = new StringBuilder();
				sb.append("<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"cad\">\r\n")
				.append( "    <div class=\"text-sm leading-tight fw5 white-space-nowrap text-base-foreground-muted\">CAD</div>\r\n")
				.append( "    <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: none;\"></div>\r\n")
				.append( "</button>");
				tabHeadList.add(sb.toString());
				
				String tab = "<div class=\"js-tab-panel\" data-id=\"cad\" style=\"display: none;\">\r\n"
						+ "      <div class=\"min-w-525\">\r\n"
						+ "        <section class=\"bg-base-container-accent p32 border border-base-stroke-subtle s-px16 s-py24 m-px16 m-py24\">\r\n"
						+ "          <h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">2D/3D CAD</h2>\r\n"
						+ "          <div class=\"info-separator h1 w-full bg-base-stroke-default\"></div>\r\n"
						+ "          <div class=\"info-description text-sm leading-normal mb24\">型式検索により2D/3DCADデータを様々なデータ形式で出力できます。<br>オプション、マニホールド等搭載した状態で出力できます。</div>";
				if (series.isCad3d()) {
					if (lang.equals("ja-jp")) {
						// ログイン後
						tab += "<div class=\"isLoginTrue f fclm gap-16 p24 bg-base-container-default border s-px16 m-px16\" style=\"display: none;\">\r\n"
								+ "    <div class=\"leading-tight fw5 l-hide\">フル品番検索</div>\r\n"
								+ "    <form action=\"/products/cad/ja-jp/\" method=\"get\" class=\"f gap-12 mb12\">\r\n"
								+ "      <input class=\"input ellipsis k\" name=\"partNumber\" type=\"text\" placeholder=\"フル品番・シリーズ・製品名で検索\">\r\n"
								+ "      <button class=\"button secondary large solid gap-8 s-is-square m-is-square\" onclick=\"this.form.submit();return false;\"><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/search.svg\" alt=\"\" title=\"\"><span class=\"text-sm leading-tight s-hide m-hide\">検索</span></button>\r\n"
								+ "    </form>\r\n"
								+ "    <div class=\"g grid-autofit-200 gap-12 s-fclm m-fclm s-gap-4 m-gap-4 mb12\">";
						if (series.isCad3d()) {
							tab+= "      <a title=\"$$$3DCAD$$$\" class=\"button secondary large solid w-auto js-modal-open\" href=\"#\"><span class=\"text-sm leading-tight\">2D ⁄ 3D CADデータはこちら</span><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" title=\"\"></a>";
							tab = tab.replace("$$$3DCAD$$$", "/webcatalog/3dcad/ja-jp/?id="+series.getModelNumber()+"&version=2026");
						}
						if (links != null && links.size() > 0) { // 2DCADの有無
							tab += "      <a title=\"$$$2DCAD$$$\" class=\"button secondary large solid w-auto js-modal-open\" href=\"#\"><span class=\"text-sm leading-tight\">2DCADデータはこちら</span><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" title=\"\"></a>";
							tab = tab.replace("$$$2DCAD$$$", AppConfig.Page2DCADUrl+series.getModelNumber()+"?version=2026");
						}
						tab +=		"  </div>\r\n"
								+ "    <div class=\"text-base-foreground-muted text-xs leading-normal\">※2D/3D CADデータはキャデナス・ウェブ・ツー・キャド株式会社のシステムに移動します。</div>\r\n"
								+ "</div>";
						// ログイン前
						tab += "<div class=\"isLoginFalse\" style=\"display:block;\">\r\n"
								+ "<div class=\"f fclm gap-16 p24 bg-base-container-default border s-px16 m-px16\">\r\n"
								+ "<div class=\"leading-tight fw5 l-hide\">フル品番検索</div>\r\n"
								+ "<div class=\"text-base-foreground-default text-center text-sm leading-tight fw5 white-space-pre-wrap\">2D/3D CADデータはユーザ登録者限定サービスです。\r\n"
								+ "ログインしてご利用ください。\r\n"
								+ "\r\n"
								+ "<a href=\"javascript:void(0)\" class=\"doOauthLogin\" data-dst=\"\"><button class=\"button large primary solid gap-8 w-full max-w-284 s-w-full s-max-w-none m-w-full m-max-w-none l-px64\" type=\"button\"><img class=\"s16 object-fit-contain\" src=\"/assets/common/re/login.svg\" alt=\"\" title=\"\"><span class=\"text-sm s-leading-tight m-leading-tight\">ログイン</span></button></a>\r\n"
								+ "</div>\r\n"
								+ "<div class=\"g grid-autofit-200 gap-12 s-fclm m-fclm s-gap-4 m-gap-4\">\r\n"
								+ "<button class=\"button secondary solid large gap-8 w-full large\" type=\"button\" disabled=\"disabled\"><span class=\"flex-fixed text-sm leading-tight\">2D ⁄ 3D CADデータはこちら</span><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" title=\"\"></button>\r\n"
								+ "<button class=\"button secondary solid large gap-8 w-full large\" type=\"button\" disabled=\"disabled\"><span class=\"flex-fixed text-sm leading-tight\">2DCADデータはこちら</span><img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" title=\"\"></button>\r\n"
								+ "</div>\r\n"
								+ "<div class=\"text-base-foreground-muted text-xs leading-normal\">※2D/3D CADデータはキャデナス・ウェブ・ツー・キャド株式会社のシステムに移動します。</div>\r\n"
								+ "</div>\r\n"
								+ "</div>";
					// TODO 2026/4 renewal 対応まだ
					} else if (lang.indexOf("en-") > -1) {
//3/1						tab+= _static_seriesCad3d_E.replace("$$$se_id$$$", series.getModelNumber());
						tab+= _static_seriesCad3d_202603_E.replace("$$$se_id$$$", series.getModelNumber());
					} else if (lang.equals("zh-tw")) {
						String sid = series.getModelNumber();
						if (sid != null && sid.isEmpty() == false) sid = sid.replace("-ZHTW", "-E");
						tab+= _static_seriesCad3d_ZHTW.replace("$$$se_id$$$", sid);
					} else if (lang.indexOf("zh-") > -1) {
						tab+= _static_seriesCad3d_ZH.replace("$$$se_id$$$", series.getModelNumber());
					}

				}
				
				tab += "</selection>\r\n";
				tab += "</div>\r\n";
				tab += "</div>\r\n";
				tabBodyList.add(tab);
				tabCount++;
			}

			// === 取説 ===
			links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 1);
			if (links != null && links.size() > 0) {
				tabTypes.add("manual");
				String manual = "取扱説明書";
				if (lang.indexOf("en-") > -1) manual = "Manual";
				else if (lang.equals("zh-tw")) manual = "說明書";
				else if (lang.indexOf("zh-") > -1) manual = "说明书";
				tabTitles.add(manual);

				String searchresult = "検索結果を表示";
				if (lang.indexOf("en-") > -1) searchresult = "View search result";
				else if (lang.indexOf("zh-tw") > -1) searchresult = "查看搜尋結果";
				else if (lang.indexOf("zh-") > -1) searchresult = "查看搜索结果";
				
				StringBuilder sb = new StringBuilder();
				sb.append("<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"manual\">\r\n")
				.append(  "    <div class=\"text-sm leading-tight fw5 white-space-nowrap text-base-foreground-muted\">").append(manual).append("</div>\r\n")
				.append(  "    <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: none;\"></div>\r\n")
				.append(  "</button>");
				tabHeadList.add(sb.toString() );

				StringBuilder manualTab = new StringBuilder();
				manualTab.append("<div class=\"js-tab-panel\" data-id=\"manual\" style=\"display: none;\">\r\n")
						.append( "      <div class=\"min-w-525\">\r\n")
						.append( "        <section class=\"bg-base-container-accent p32 border border-base-stroke-subtle s-px16 s-py24 m-px16 m-py24\">\r\n")
						.append( "          <h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">").append(manual).append("</h2>\r\n")
						.append( "          <div class=\"info-separator h1 w-full bg-base-stroke-default\"></div>\r\n")
						.append( "          <div class=\"w-full overflow-x-auto\">\r\n")
						.append( "             <table class=\"table-hover s-full border-bottom border-right border-base-stroke-default border-collapse-collapse\">\r\n")
						.append( "               <thead>\r\n");
				// 取説はJSONで保持
				manualTab.append( "<tr>\r\n" );
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					manualTab.append("<th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">").append(title).append("</th>\r\n" );
				}
				manualTab.append( "</tr>\r\n");
				manualTab.append("</thead>\r\n");
				manualTab.append("<tbody>\r\n");
				for (int i = 1; i < links.size(); i++) {
					manualTab.append("<tr>\r\n");
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							manualTab.append( "<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">");
							if (val != null && val.isEmpty() == false) {
								if (val.indexOf("/manual/") > -1) { // 2023/6/22manuals本番アップで要置き換え
									val = val.replace("/manual/", "/manuals/");
									if (lang.indexOf("en-") > -1) {
										val = val.replace("/en/", "/" + lang + "/");
									} else if (lang.indexOf("zh-") > -1) {
										val = val.replace("/zh/", "/" + lang + "/");
										val = val.replace("/en/", "/" + lang + "/");
									} else {
										val = val.replace("/ja/", "/ja-jp/");
									}
									val = val.replace("/s.do?k=", "/search?query=");
									val = val.replace("/?k=", "/search?query=");
								}
								manualTab.append( "<div class=\"f fc\">")
										.append( "  <a class=\"f fm gap-4\" target=\"_blank\" href=\"").append(val).append("\">")
										.append( "    <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">").append(searchresult).append("</span>")
										.append( "    <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\"").append(searchresult).append("\">")
										.append( "  </a>")
										.append( "</div>");
							}
							manualTab.append("</td>");
						} else {
							manualTab.append( "<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">").append( val ).append("</td>");
						}
						cnt++;
					}
				}
				manualTab.append( "</tr>\r\n");

				manualTab.append( "</tbody></table>\r\n");
				manualTab.append( "</div>\r\n");
				manualTab.append( "</selection>\r\n");
				manualTab.append( "</div>\r\n");
				manualTab.append( "</div>\r\n");
				tabBodyList.add(manualTab.toString());
				tabCount++;
			}

			// === CE ===
			links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 3);
			if (links != null && links.size() > 0) {
				tabTypes.add("doc");
				String doc = "自己宣言書";
				if (lang.indexOf("en-") > -1) doc = "DoC/IM";
				else if (lang.indexOf("zh-tw") > -1) doc = "自我宣告書";
				else if (lang.indexOf("zh-") > -1) doc = "自我宣告书";
				tabTitles.add(doc);
				
				StringBuilder sb = new StringBuilder();
				sb.append( "<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"doc\">\r\n")
					.append( "   <div class=\"text-sm leading-tight fw5 white-space-nowrap text-base-foreground-muted\">").append(doc).append("</div>\r\n")
					.append( "   <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: none;\"></div>\r\n")
					.append( "</button>");
				tabHeadList.add(sb.toString());
				String searchresult = "検索結果を表示";
				if (lang.indexOf("en-") > -1) searchresult = "View search result";
				else if (lang.indexOf("zh-tw") > -1) searchresult = "查看搜尋結果";
				else if (lang.indexOf("zh-") > -1) searchresult = "查看搜索结果";

				String tab = "<div class=\"js-tab-panel\" data-id=\"doc\" style=\"display: none;\">\r\n"
						+ "                                  <div class=\"min-w-525\">\r\n"
						+ "                                                <section class=\"bg-base-container-accent p32 border border-base-stroke-subtle s-px16 s-py24 m-px16 m-py24\">\r\n"
						+ "                                                  <h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">"+doc+"</h2>\r\n"
						+ "                                                  <div class=\"info-separator h1 w-full bg-base-stroke-default\"></div>\r\n"
						+ "                                                  <div class=\"w-full overflow-x-auto\">\r\n"
						+ "                                                    <table class=\"table-hover s-full border-bottom border-right border-base-stroke-default border-collapse-collapse\">\r\n"
						+ "                                                      <thead>";
				tab += "<tr>\r\n" ;
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					tab +=  "<th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">"+ title + "</th>\r\n" ;
					cnt++;
				}
				tab += "</tr>\r\n";
				tab += "</thead>\r\n";
				tab += "<tbody>\r\n";
				for (int i = 1; i < links.size(); i++) {
					tab += "<tr>\r\n";
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							tab += "<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">";
							if (StringUtils.isEmpty(val) == false) {
								tab += "<div class=\"f fc\">"
										+ "<a class=\"f fm gap-4\" target=\"_blank\" href=\""+val+"\">"
										+ "<span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">"+searchresult+"</span>"
										+ "<img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\""+searchresult+"\">"
										+ "</a>"
										+"</div>";
							}
							tab +="</td>";
						} else {
							tab += "<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">" + val + "</td>";
						}
						cnt++;
					}
				}
				tab += "</tr>\r\n";

				tab += "</tbody></table>\r\n";
				tab += "</div>\r\n";
				tab += "</selection>\r\n";
				tab += "</div>\r\n";
				tab += "</div>\r\n";
				tabBodyList.add(tab);
				tabCount++;
			}

			// === マニホールド ===
			links = getGuideIDLinks(series.getModelNumber(), series.getSpec(), 2);
			if (links != null && links.size() > 0) {
				tabTypes.add("manifold");

				String manifold = "マニホールド仕様書";
				if (lang.indexOf("en-") > -1) manifold = "Manifold";
				else if (lang.indexOf("zh-tw") > -1) manifold = "歧管規格";
				else if (lang.indexOf("zh-") > -1) manifold = "集装底座";
				tabTitles.add(manifold);
				
				tabHeadList.add( "<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"manifold\">\r\n"
						+ "                                                <div class=\"text-sm leading-tight fw5 white-space-nowrap text-base-foreground-muted\">"+manifold+"</div>\r\n"
						+ "                                                <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: none;\"></div>\r\n"
						+ "                                              </button>");
				String searchresult = "検索結果を表示";
				if (lang.indexOf("en-") > -1) searchresult = "View search result";
				else if (lang.indexOf("zh-tw") > -1) searchresult = "查看搜尋結果";
				else if (lang.indexOf("zh-") > -1) searchresult = "查看搜索结果";

				String tab = "<div class=\"js-tab-panel\" data-id=\"manifold\" style=\"display: none;\">\r\n"
						+ "                                  <div class=\"min-w-525\">\r\n"
						+ "                                                <section class=\"bg-base-container-accent p32 border border-base-stroke-subtle s-px16 s-py24 m-px16 m-py24\">\r\n"
						+ "                                                  <h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">"+manifold+"</h2>\r\n"
						+ "                                                  <div class=\"info-separator h1 w-full bg-base-stroke-default\"></div>\r\n"
						+ "                                                  <div class=\"w-full overflow-x-auto\">\r\n"
						+ "                                                    <table class=\"table-hover s-full border-bottom border-right border-base-stroke-default border-collapse-collapse\">\r\n"
						+ "                                                      <thead>";
				tab += "<tr>\r\n" ;
				String[] titles = links.get(0);
				int cnt = 0;
				for (String title : titles) {
					tab +=  "<th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">"+ title + "</th>\r\n" ;
				}
				tab += "</tr>\r\n";
				tab += "</thead>\r\n";
				tab += "<tbody>\r\n";
				for (int i = 1; i < links.size(); i++) {
					tab += "<tr>\r\n";
					String[] arr = links.get(i);
					cnt = 0;
					for (String val : arr) {
						if (cnt == arr.length-1) {
							tab += "<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">";
							if (StringUtils.isEmpty(val) == false) {
								tab += "<div class=\"f fc\">"
										+ "<a class=\"f fm gap-4\" target=\"_blank\" href=\""+val+"\">"
										+ "<span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">"+searchresult+"</span>"
										+ "<img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\""+searchresult+"\">"
										+ "</a>"
										+"</div>";
							}
							tab +="</td>";
						} else {
							tab += "<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">" + val + "</td>";
						}
						cnt++;
					}
				}
				tab += "</tr>\r\n";

				tab += "</tbody></table>";
				tab += "</div>\r\n";
				tab += "</selection>\r\n";
				tab += "</div>\r\n";
				tab += "</div>\r\n";
				tabBodyList.add(tab);
				tabCount++;
			}

			// === 簡易特注 ===
			if (series.isCustom()) {
				tabTypes.add("easy");
				String str = "簡易特注";
				if (lang.indexOf("en-") > -1) str = "Simple Specials";
				else if (lang.equals("zh-tw")) str = "簡易特注";
				else if (lang.indexOf("zh-") > -1) str = "简易特注";
				tabTitles.add(str);
				
				tabHeadList.add( "<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"easy\">\r\n"
						+ "                                                <div class=\"text-sm leading-tight fw5 white-space-nowrap text-base-foreground-muted\">"+str+"</div>\r\n"
						+ "                                                <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: none;\"></div>\r\n"
						+ "                                              </button>");

				String tab = "<div class=\"js-tab-panel\" data-id=\"easy\" style=\"display: none;\">\r\n"
						+ "                                  <div class=\"min-w-525\">\r\n"
						+ "                                                <section class=\"bg-base-container-accent p32 border border-base-stroke-subtle s-px16 s-py24 m-px16 m-py24\">\r\n"
						+ "                                                  <h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">"+str+"</h2>\r\n"
						+ "                                                  <div class=\"info-separator h1 w-full bg-base-stroke-default\"></div>\r\n"
						+ "                                                  <div class=\"w-full overflow-x-auto\">\r\n";
				String div = "簡易特注";
				if (lang.indexOf("en-") > -1) div = "Simple Specials";
				// TODO 中国語
				List<Omlist> omlist = omlistService.searchKeyword(null, null, div, series.getModelNumber(), lang);
				tab += omlistService.getTableHtml2026(omlist, lang);
				if (lang.equals("ja-jp")) tab = tab.replace("<table ", _CustomJa2026+"<table ");
				tab += "</div>\r\n";
				tab += "</selection>\r\n";
				tab += "</div>\r\n";
				tab += "</div>\r\n";
				tabBodyList.add(tab);
				tabCount++;
			}
			// === オーダーメイド ===
			if (series.isOrderMade()) { // オーダーメイド
				tabTypes.add("om");
				String str = "オーダーメイド";
				if (lang.indexOf("en-") > -1) str = "Made to Order";
				else if (lang.indexOf("zh-") > -1) str = "订制规格";
				tabTitles.add(str);
				
				tabHeadList.add( "<button class=\"relative f fh flex-fixed p12 js-tab-btn hover-accent-inverse-90 min-w-136 s-w-auto s-min-w-100 m-w-auto m-min-w-100\" type=\"button\" data-target=\"om\">\r\n"
						+ "                                                <div class=\"text-sm leading-tight fw5 white-space-nowrap text-base-foreground-muted\">"+str+"</div>\r\n"
						+ "                                                <div class=\"absolute b0 l0 h2 w-full bg-primary js-tab-line\" style=\"display: none;\"></div>\r\n"
						+ "                                              </button>");

				String tab = "<div class=\"js-tab-panel\" data-id=\"om\" style=\"display: none;\">\r\n"
						+ "                                  <div class=\"min-w-525\">\r\n"
						+ "                                                <section class=\"bg-base-container-accent p32 border border-base-stroke-subtle s-px16 s-py24 m-px16 m-py24\">\r\n"
						+ "                                                  <h2 class=\"text-base-foreground-default text-medium leading-normal fw5\">"+str+"</h2>\r\n"
						+ "                                                  <div class=\"info-separator h1 w-full bg-base-stroke-default\"></div>\r\n"
						+ "                                                  <div class=\"w-full overflow-x-auto\">\r\n";
				String div = "オーダーメイド";
				if (lang.indexOf("en-") > -1) div = "Made to Order Common Specifications";
				else if (lang.indexOf("zh-") > -1) div = "订制规格";

				List<Omlist> omlist = omlistService.searchKeyword(null, null, div, series.getModelNumber(), lang);
				tab += omlistService.getTableHtml2026(omlist, lang);
				if (lang.equals("ja-jp")) tab = tab.replace("<table ", _OrderMadeJa2026+"<table ");
				tab += "</div>\r\n";
				tab += "</selection>\r\n";
				tab += "</div>\r\n";
				tab += "</div>\r\n";
				tabBodyList.add(tab);
				tabCount++;
			}
			if (tabCount > 1) {
				String title = "";
				if (tabHeadList.size() > 0) {
					for(String head : tabHeadList) {
						title += head;
					}
					ret = StringUtils.replace(ret, "$$$tabs_title$$$", title);
				} else {
					ret = StringUtils.replace(ret, "$$$tabs_title$$$", title);
					ret = StringUtils.replace(ret, "border-bottom", "");
				}

				String body = "";
				for(String str : tabBodyList) {
					body += str;
				}
				ret = StringUtils.replace(ret, "$$$tabs$$$", body);
			} else {
				ret = StringUtils.replace(ret, "$$$tabs_title$$$","");
				ret = StringUtils.replace(ret, "border-bottom", "");
				ret = StringUtils.replace(ret, "$$$tabs$$$","");
			}
			if (button != null && button.isEmpty() == false) {
				String otherTitle = "その他の資料・ツール";
				if (lang.indexOf("en-") > -1) otherTitle = "Other information / tools";
				else if (lang.equals("zh-tw")) otherTitle = "其他信息・工具";
				else if (lang.indexOf("zh-") > -1) otherTitle = "其他資訊/工具";
				ret = ret.replace("$$$buttonTitle$$$", otherTitle);
				
				ret = StringUtils.replace(ret, "$$$button$$$", button);
			} else {
				ret = ret.replace("$$$buttonTitle$$$", "");
				ret = StringUtils.replace(ret, "$$$button$$$", "");
			}
		} else {
			ret = StringUtils.replace(ret, "$$$tabs$$$","");
			ret = StringUtils.replace(ret, "$$$tabs_title$$$","");
			ret = StringUtils.replace(ret, "border-bottom", "");
			ret = StringUtils.replace(ret, "$$$button$$$", "");
			ret = ret.replace("$$$buttonTitle$$$", "");
			ret = ret.replace("$$$isLoginButton$$$", "");
		}

		ret = StringUtils.replace(ret, "$$$rohs$$$","");
		
		StringBuilder strMylist = new StringBuilder();
		strMylist.append("<button class=\"isLoginFalse   relative group flex-fixed js-mylist-btn l-mr16 s-s32 m-s32 s24\" type=\"button\" onclick=\"location.href='/customer/ja/tologin.do?dst=$$$url$$$'\">")
				.append( "<img class=\"s-full object-fit-contain js-icon-plus\" src=\"/assets/smcimage/common/folderPlus.svg\" alt=\"マイリストに追加\" title=\"マイリストに追加\">")
				.append( "<span class=\"tooltip-text\" role=\"tooltip\">マイリストに追加</span>")
				.append( "</button>")
				.append( "<div class=\"isLoginTrue\" style=\"display: none;\">")
				.append( "<button class=\"myList bt_hide_mylistwin to_mylist_$$$sid$$$   relative group flex-fixed l-mr16 s-s32 m-s32 s24\" type=\"button\" onclick=\"add_mylist_sid('$$$sid$$$', 'ja');return false;\">")
				.append( "<img class=\"s-full object-fit-contain js-icon-plus\" src=\"/assets/smcimage/common/folderPlus.svg\" alt=\"マイリストに追加\" title=\"マイリストに追加\">")
				.append( "<span class=\"tooltip-text\" role=\"tooltip\">マイリストに追加</span>")
				.append( "</button>")
				.append( "<button class=\"end_mylist   relative group flex-fixed  l-mr16 s-s32 m-s32 s24 \" type=\"button\" id=\"end_mylist_$$$sid$$$\" onclick=\"return false;\"  style=\"display: none;\">")
				.append( "<img class=\"s-full object-fit-contain js-icon-check\" src=\"/assets/smcimage/common/folder-check.svg\" alt=\"マイリストに追加済\" title=\"マイリストに追加済\">")
				.append( "</button>")
				.append( "</div>");

		// /assets/js/global.jsのgetLoginData()でログイン描画させる。
		// 中国語はマイリスト無し。ja-jp en-jpのみ (en-sg等も要らない。)
		if (isGuide) {
			ret = StringUtils.replace(ret, "$$$mylist$$$", "");
		}
		else if (lang.equals("ja-jp") ) {
			String mylist = strMylist.toString().replace("$$$sid$$$", series.getModelNumber());
			mylist = mylist.replace("$$$url$$$", url);
			ret = StringUtils.replace(ret, "$$$mylist$$$", mylist);
		} else if (lang.equals("en-jp")) {
			String mylist = strMylist.toString().replace("$$$sid$$$", series.getModelNumber());
			mylist = mylist.replace("$$$url$$$", url);
			mylist = mylist.replace("マイリストに追加済", "Added to My List");
			mylist = mylist.replace("マイリストに追加", "Add to My List");
			mylist = mylist.replace("'ja'", "'en'");
			ret = StringUtils.replace(ret, "$$$mylist$$$", mylist);
		} else {
			ret = StringUtils.replace(ret, "$$$mylist$$$", "");
		}

		String other = series.getOther();
		ret = StringUtils.replace(ret, "$$$other$$$", other);

		String tmp = "";
		if (isGuide) {
			// ガイドはカテゴリの一覧をパンくずで表示
			List<String> titleList = new LinkedList<>();
			List<String> slugList = new LinkedList<>();
			boolean catRet = series.getCatpansTitleAndLink(titleList, slugList);
			if (catRet) {
				tmp += getGuideCatpan2026(lang, titleList, slugList);
			}
		}
		else if (StringUtils.isEmpty(series.getNotice()) == false)
		{
			tmp = "<p class=\"note\">" + series.getNotice() + "</p>";
		}
		ret = StringUtils.replace(ret, "$$$notice$$$", tmp);
		tmp = "";
		if (StringUtils.isEmpty(series.getImageTop()) == false) tmp = series.getImageTop();
		ret = StringUtils.replace(ret, "$$$imageTop$$$",tmp);
		tmp = "";
		if (StringUtils.isEmpty(series.getImageBottom()) == false) tmp = series.getImageBottom();
		ret = StringUtils.replace(ret, "$$$imageBottom$$$",tmp);

		String detail = series.getDetail().replace("\r\n", "<br>");
		ret = StringUtils.replace(ret, "$$$details$$$", detail);

		// 製品特長 $$$cate2$$$ $$$advantageBody$$$ $$$backUrl$$$
		if  (isGuide == false && isAdvantage) {
			if (c2 != null) {
				ret = StringUtils.replace(ret, "$$$cate2$$$", c2.getName());
			} else {
				ret = StringUtils.replace(ret, "$$$cate2$$$","");
			}
			if (series.getAdvantage().isEmpty() == false) {

				adv = series.getAdvantage();
				if (adv.trim().indexOf("@@@") == 0) {
					String[] arr2 = adv.split("@@@");
					if (arr2.length >= 2) {
						try {
							int page = Integer.parseInt(arr2[1]);
							adv = LibOkHttpClient.getHttpsHtml(AppConfig.PageCDNIdUrl + page, AppConfig.BasicAuthCDNID, AppConfig.BasicAuthCDNPW);
							String[] arr3 = StringUtils.splitByWholeSeparator(adv, "<body>");
							if (arr3.length >= 2) {
								String[] arr4 = StringUtils.splitByWholeSeparator(arr3[1], "</body>");
								adv = arr4[0];
							}
						}catch (Exception e) {
							log.error("@@@xxx@@@ parse error. advantage = " + adv.toString());
							adv = "";
						}
					}
				} else if (adv.trim().indexOf("/") == 0) {
					String h = LibOkHttpClient.getHttpsHtml(AppConfig.PageCDNIdUrl + adv.trim());
					if (h == null || h.isEmpty()) {
						adv = LibOkHttpClient.getHtml(AppConfig.PageProdUrl + adv.trim());
					} else {
						adv = h;
					}
				} else {
					adv = LibOkHttpClient.getHtml( adv.trim());
				}
				ret = StringUtils.replace(ret, "$$$advantageBody$$$", adv);
			} else {
				ret = StringUtils.replace(ret, "$$$advantageBody$$$", "");
			}
			//ret = StringUtils.replace(ret, "$$$backUrl$$$", "/"); // そのまま置いておく。表示時に選択

		} else {
			if (c2 != null) {
				ret = StringUtils.replace(ret, "$$$cate2$$$", c2.getName());
			} else {
				ret = StringUtils.replace(ret, "$$$cate2$$$","");
			}
			ret = StringUtils.replace(ret, "$$$advantageBody$$$", "");
		}
		String str = "一覧へ戻る";
		if (lang.indexOf("en-") > -1) str = "Back to list";
		else if (lang.indexOf("zh-") > -1) str = "返回";

		ret = StringUtils.replace(ret, "$$$backUrlMessage$$$", str);
		
		return ret;
	}

	// AppConfig.DispItemTitleArrにあるItemのタイトル + [length-1] リンク
	// 1行目はタイトル
	// type [0]2DCAD [1]Manual [2]Manifold [3]Doc
	// 1つも入力されていなければnullを返す。
	public List<String[]> getGuideIDLinks(String guideId, String spec, int type) {
		List<String[]> ret = new ArrayList<String[]>();
		boolean isExists = false;

		if (spec != null) {
			spec = spec.replace("\r", "").replace("\n", "").replace("\t", "");
			spec = spec.replace(",null", ",\"\"");
		}

		List<String> chk = Arrays.asList(AppConfig.DispItemTitleArr);
		List<Integer> dispCol = new LinkedList<Integer>();
		int cnt2D = -1;

		int cnt = 0;
		int cnt2 = 0;
		try {
			JSONArray jsonArr = new JSONArray(spec);
			for (Object arr : jsonArr)
			{
				JSONArray arr2 = (JSONArray)arr;
				if (cnt == 0) {
					for (Object obj : arr2) {
						if (cnt == 0) { // タイトルから場所を特定
							if (obj.equals(AppConfig.LinkJSONHead[0])) { // [2DCAD]
								cnt2D = cnt2;
								break;
							}
							else if (chk.contains(obj)) {
								dispCol.add(cnt2);
							}
						}
						cnt2++;
					}
					// タイトル準備
					String[] r = new String[dispCol.size()+1];
					for(int i = 0; i < dispCol.size(); i++) {
						r[i] = arr2.getString(dispCol.get(i));
					}
					r[dispCol.size()] = messagesource.getMessage("tab.button.search.result", null,  _locale);
					ret.add(r);
				} else {
					String[] r = new String[dispCol.size()+1];
					for(int i = 0; i < dispCol.size(); i++) {
						r[i] = arr2.getString(dispCol.get(i));
					}
					String link = (String)arr2.getString(cnt2D+type);
					if (link != null && link.isEmpty() == false) isExists = true;
					r[dispCol.size()] = link;
					ret.add(r);
				}
				cnt++;
			}
		} catch(org.json.JSONException e) {
			log.error("getGuideIDLinks() JSONException="+e.getMessage());
			log.error("spec = "+spec);
		} catch (Exception e) {
			log.error("getGuideIDLinks()"+e.getMessage());
		}
		if (isExists == false) ret = null;
		return ret;
	}

	public String getPictureList(Category c, Category c2, List<Series> list) {
		String ret = "<div class=\"cata_picture_list\">\r\n";
		for (Series s : list) {
			ret += "<a href=\""+AppConfig.ContextPath+"/"+s.getLang()+"/"+c.getSlug()+"/"+c2.getSlug()+"/"+s.getModelNumber()+"\">\r\n";
			if (s.getImage() != null && s.getImage().isEmpty() == false ) {
				if (s.getLang() != null && s.getLang().equals("zh-cn")) ret += "<img src=\""+AppConfig.ImageProdPath+s.getLang()+"/"+s.getImage()+"\">\r\n";
				else ret += "<img src=\""+AppConfig.ImageProdUrl+s.getLang()+"/"+s.getImage()+"\">";
			}
			ret += "<p>";
			ret += s.getName();
			ret += "</p>";
			ret += "<p>";
			ret += s.getNumber();
			ret += "</p>";
			ret += "</a>\r\n";
		}
		ret += "</div>\r\n";
		return ret;
	}
	public String getPictureList2026(Category c, Category c2, List<Series> list) {
		String ret = "<div class=\"tile-grid-wrap\">\r\n"
					+ "  <div class=\"grid border-left tile-grid-fixed grid-autofit-300 s-grid-autofit-160\">\r\n";
		for (Series s : list) {
			ret += "<a class=\"f fclm flex-top gap-16 border-top bg-base-container-default border-bottom border-right p16 s-gap-10\" href=\""+AppConfig.ContextPath+"/"+s.getLang()+"/"+c.getSlug()+"/"+c2.getSlug()+"/"+s.getModelNumber()+"\">";
			if (s.getImage() != null && s.getImage().isEmpty() == false ) {
				String imgUrl = "";
				if (s.getLang() != null && s.getLang().equals("zh-cn")) imgUrl = AppConfig.ImageProdPath+s.getLang()+"/"+s.getImage();
				else imgUrl = AppConfig.ImageProdUrl+s.getLang()+"/"+s.getImage();
				ret += "<img class=\"w-full object-fit-contain object-position-top\" src=\""+imgUrl+"\" alt=\""+s.getNumber()+"\">\r\n";
			}
			ret += "<div class=\"f fclm gap-8\">\r\n"
					+ "<div class=\"f gap-8 s-gap-4 m-gap-4\">\r\n"
					+ "<div class=\"f fm flex-fixed h24\"><img class=\"object-fit-contain s16\" src=\"/assets/smcimage/common/arrow-right.svg\" alt=\"\" aria-hidden=\"true\"></div>\r\n"
					+ "<span class=\"text-base leading-normal fw5 word-break-word\"><span class=\"hover-link-underline\">"+s.getNumber()+"</span></span></div>\r\n"
					+ "<span class=\"pl24 leading-tight fw5 text-base-foreground-muted text-xs s-text-sm\">"+s.getName()+"</span></div>"
					+ "</a>";
			
		}
		ret += "  </div>\r\n"
			+ "</div>\r\n";
		return ret;
	}

	/**
	 * 仕様比較のHTML取得
	 * 検索用の値、NarrowDownCompareをそのまま表示 2025/11/25
	 * @param lang
	 * @param baseLang
	 * @param c
	 * @param c2
	 * @param compareList
	 * @param colList
	 * @param list narrowDownService.getNarrowDown()で検索したSeriesList
	 * @param request
	 * @return
	 */
	public String getCompareHtml(String lang, String baseLang, Category c, Category c2,
			List<NarrowDownColumn> colList, List<Series> list, HashMap<String, List<NarrowDownValue>> map,
			HttpServletRequest request) {
		
		String ret = "<div class=\"cata_compare_list\">";
		
		final String separateStr = ", "; // Valueが複数の場合の区切り文字
		String seriesStr = "シリーズ";
		if (baseLang.indexOf("en-") > -1) seriesStr="Series";
		else if (baseLang.indexOf("zh-") > -1) seriesStr="系列";
		
		String width = "100%";
		if (list != null && list.size() > 0) {
			width = list.size() * 150 + "px";
			ret+= "<table class=\"cata_compare_table\" width=\""+width+"\"><tr>";
			ret+="<th>&nbsp;</th>";
			for (Series s : list) {
				ret += "<td>";
				if (s.getImage() != null && s.getImage().isEmpty() == false ) {
					ret += "<a href=\""+AppConfig.ContextPath+"/"+s.getLang()+"/"+c.getSlug()+"/"+c2.getSlug()+"/"+s.getModelNumber()+"\">";
					if (s.getLang() != null && s.getLang().equals("zh-cn")) ret += "<img src=\""+AppConfig.ImageProdPath+s.getLang()+"/"+s.getImage()+"\">";
					else ret += "<img src=\""+AppConfig.ImageProdUrl+s.getLang()+"/"+s.getImage()+"\">";
					ret += "</a>\r\n";
				} else {
					ret += "<a href=\""+AppConfig.ContextPath+"/"+s.getLang()+"/"+c.getSlug()+"/"+c2.getSlug()+"/"+s.getModelNumber()+"\">";
					ret += "None image";
					ret += "</a>\r\n";
				}
				ret += "</td>";
			}
			ret += "</tr>";
			
			// タイトル行
			ret+= "<tr>";
			ret+="<th>";
			ret+= seriesStr;
			ret+="</th>";
			for (Series s : list) {
				String tmp = s.getNumber();
				if (tmp == null || tmp.equals("")) tmp = "<td style=\"text-align:center;\"> - ";
				ret += "<td>";
				ret += "<a href=\""+AppConfig.ContextPath+"/"+lang+"/"+c.getSlug()+"/"+c2.getSlug()+"/"+s.getModelNumber()+"\">";
				ret += tmp;
				ret += "</a>\r\n";
				ret += "</td>";
			}
			ret += "</tr>";
			
			// 項目
			for(NarrowDownColumn col : colList) 
			{
				String title = col.getTitle();
				ret+= "<tr>";
				ret+="<th>";
				ret+= title;
				ret+="</th>";
				for (Series s : list) {
					List<NarrowDownValue> vList = map.get(s.getId());
					boolean isFind = false;
					for(NarrowDownValue val : vList) {
						if (col.getId().equals(val.getColumnId())) {
							String[] arr = val.getParam();
							if (arr != null && arr.length > 0) {
								String tmp = String.join(separateStr, arr);
								if (tmp != null && tmp.isEmpty()) 
								{
									ret += "<td style=\"text-align:center;\"> - ";
								} else {
									ret += "<td>";
									ret += tmp;
								}
							} else if (val.getRangeParam() != null){
								ret += "<td>";
								ret +=val.getRangeParam();
							} else {
								ret += "<td style=\"text-align:center;\"> - ";
							}
							ret += "</td>";
							isFind = true;
							break;
						}
					}
					if (isFind == false) {
						ret += "<td style=\"text-align:center;\"> - </td>";
					}
				}
				ret += "</tr>";
			}
			ret += "</table>\r\n";
			ret += "</div>\r\n";
		} else  {
			if (baseLang.indexOf("en-") > -1) {
				ret = "<h4>There were no series that matched the criteria.</h4>";
			} else if(baseLang.equals("zh-tw")){
				ret = "<h4>沒有符合標準的系列。</h4>";
			} else if (baseLang.indexOf("zh-") > -1) {
				ret = "<h4>没有符合标准的系列。</h4>";
			} else {
				ret = "<h4>条件に一致したシリーズがありませんでした。</h4>";
			}
		}
		return ret;
	}
	public String getCompareHtml2026(String lang, String baseLang, Category c, Category c2,
			List<NarrowDownColumn> colList, List<Series> list, HashMap<String, List<NarrowDownValue>> map,
			HttpServletRequest request) {
		
		StringBuilder ret = new StringBuilder();
				
		final String separateStr = ", "; // Valueが複数の場合の区切り文字
		String seriesStr = "シリーズ";
		if (baseLang.indexOf("en-") > -1) seriesStr="Series";
		else if (baseLang.indexOf("zh-") > -1) seriesStr="系列";
		
		String width = "100%";
		if (list != null && list.size() > 0) {
			ret.append("<div class=\"w-full min-w-0 overflow-auto mb48\">\r\n");
			ret.append("                <div style=\"overflow-y: hidden;overflow-x: scroll;transform: rotateX(180deg);\" class=\"w70per s-w100per m-w100per\">");
			
			width = (list.size() + 1) * 170 + "px";
			ret.append("<table class=\"table\" width=\""+width+"\" style=\"table-layout: fixed;transform: rotateX(180deg);\"><tr>");
			ret.append("<th class=\"th th-sticky w170\" colspan=\"1\"></th>");
			for (Series s : list) {
				ret.append("<td class=\"td w170 text-center\" colspan=\"1\">");
				if (s.getImage() != null && s.getImage().isEmpty() == false ) {
					ret.append("<a href=\"").append(AppConfig.ContextPath).append("/").append(s.getLang()).append("/").append(c.getSlug()).append("/").append(c2.getSlug()).append("/").append(s.getModelNumber()).append("\">");
					if (s.getLang() != null && s.getLang().equals("zh-cn")) ret.append("<img class=\"s120 object-fit-contain\" src=\"").append(AppConfig.ImageProdPath).append(s.getLang()).append("/").append(s.getImage()).append("\">");
					else ret.append( "<img class=\"s120 object-fit-contain\" src=\"").append(AppConfig.ImageProdUrl).append(s.getLang()).append("/").append(s.getImage()).append("\">");
					ret.append( "</a>\r\n");
				} else {
					ret.append( "<a href=\"").append(AppConfig.ContextPath).append("/").append(s.getLang()).append("/").append(c.getSlug()).append("/").append(c2.getSlug()).append("/").append(s.getModelNumber()).append("\">");
					ret.append( "None image");
					ret.append( "</a>\r\n");
				}
				ret.append( "</td>");
			}
			ret.append( "</tr>");
			
			// タイトル行
			ret.append( "<tr>");
			ret.append("<th class=\"th th-sticky w170\" colspan=\"1\">");
			ret.append( seriesStr);
			ret.append("</th>");
			for (Series s : list) {
				String tmp = s.getNumber();
				if (tmp == null || tmp.equals("")) {
					ret.append( "<td class=\"text-center\"> - ");
				} else {
					ret.append( "<td class=\"td text-sm leading-tight fw4 overflow-wrap-break-word\" colspan=\"1\">");
					ret.append( "<a href=\"").append(AppConfig.ContextPath).append("/").append(lang).append("/").append(c.getSlug()).append("/").append(c2.getSlug()).append("/").append(s.getModelNumber()).append("\">");
					ret.append( tmp);
					ret.append( "</a>\r\n");
				}
				ret.append("</td>");
			}
			ret.append("</tr>");
			
			// 項目
			for(NarrowDownColumn col : colList) 
			{
				ret.append("<tr>");
				ret.append("<th class=\"th th-sticky w170\" colspan=\"1\">");
				ret.append( col.getTitle());
				ret.append("</th>");
				for (Series s : list) {
					List<NarrowDownValue> vList = map.get(s.getId());
					boolean isFind = false;
					for(NarrowDownValue val : vList) {
						if (col.getId().equals(val.getColumnId())) {
							String[] arr = val.getParam();
							if (arr != null && arr.length > 0) {
								String tmp = String.join(separateStr, arr);
								if (tmp != null && tmp.isEmpty()) 
								{
									ret.append( "<td class=\"td text-center\"> - ");
								} else {
									ret.append( "<td class=\"td text-sm leading-tight fw4\" colspan=\"1\">");
									ret.append( tmp);
								}
							} else if (val.getRangeParam() != null){
								ret.append( "<td class=\"td text-sm leading-tight fw4\" colspan=\"1\">");
								ret.append(val.getRangeParam());
							} else {
								ret.append( "<td class=\"td text-center\"> - ");
							}
							ret.append( "</td>");
							isFind = true;
							break;
						}
					}
					if (isFind == false) {
						ret.append( "<td class=\"td text-center\"> - </td>");
					}
				}
				ret.append( "</tr>");
			}
			ret.append( "</table>\r\n");
			ret.append( "  </div>\r\n")
				.append( "</div>\r\n");
		} else  {
			ret.append("<div class=\"f fh border boder-base-stroke-subtle h160 w-full bg-base-container-accent\">\r\n")
				.append("  <span class=\"fw5 s-px16 s-text-center m-px16 m-text-center\">");
			if (baseLang.indexOf("en-") > -1) {
				ret.append( "There were no series that matched the criteria.");
			} else if(baseLang.equals("zh-tw")){
				ret.append( "沒有符合標準的系列。");
			} else if (baseLang.indexOf("zh-") > -1) {
				ret.append( "没有符合标准的系列。");
			} else {
				ret.append( "条件に一致したシリーズがありませんでした。");
			}
			ret.append("</span></div>");
		}
		return ret.toString();
	}
	// 旧tab内のHTML
	private String getTabString(String modelNumber, int tabCnt, String tab) {
		String ret = "";
		if (tab != null) {
			if (modelNumber != null && modelNumber.indexOf("/") > -1) modelNumber = modelNumber.replace("/", "_");
			if (modelNumber != null && modelNumber.indexOf("(") > -1) modelNumber = modelNumber.replace("(", "_").replace(")", "_");
			String head = "<li id=\"cont$$$itemcnt$$$_$$$tabcnt$$$\" class=\"w_panel$$$active$$$\">";
			head = head.replace("$$$itemcnt$$$", modelNumber);
			head = head.replace("$$$tabcnt$$$", String.format("%02d", tabCnt));
			if (tabCnt == 1) {
				head = head.replace("$$$active$$$", " active");
			} else {
				head = head.replace("$$$active$$$", "");
			}
			String foot = "</li>";
			ret = head + tab + foot;
		}
		return ret;
	}
	private String getCESpecListHtml(String number, String modelNumber, String url, String lang, List<String> specList) {
		String ret = "";
		boolean isFind = false;
		boolean isConnected = false;
		String button = "downloadBt";
		if (lang != null && lang.indexOf("en-") > -1) button += "_E";
		// 繋げたものがそのままある場合、全てOKにする。
		String dispNumber = "";
		if (modelNumber.contains("/") || modelNumber.contains("・") || modelNumber.contains(",")){
			isConnected = true;
			dispNumber = number;
			isFind = isCeExists(modelNumber, lang, isConnected);
		} else if (number.contains("/") || number.contains("・") || number.contains(",")){
			isConnected = true;
			dispNumber = modelNumber;
			isFind = isCeExists(number, lang, isConnected);
		}
		Locale locale = getLocale(lang);
		if (isConnected && isFind) {
			for(String sp : specList) {
				ret+="<tr>\r\n<td>" + sp + "</td><td>";
				ret+="<a class=\""+button+"\"  target=\"_blank\" href=\"/overseas/international/"+lang+"/ce/index.html?dec="+dispNumber+"\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
				ret+="</td></tr>\r\n";
			}
		} else {
			for(String sp : specList) {
				ret+="<tr>\r\n<td>" + sp + "</td><td>";
				if (sp.indexOf("<br") > -1) {
					String[] arr = sp.replace("<br/>", "<br>").replace("<br />", "<br>").split("<br>");
					for(String tmp : arr) {
						if (isCeExists(tmp.trim(), lang, true)) { // 残圧排気弁<br/>VP542-X536<br/>直接配管形など。どれかtrueならOK
							ret+="<a class=\""+button+"\"  target=\"_blank\" href=\"/overseas/international/"+lang+"/ce/index.html?dec="+modelNumber+"\" >"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
							break;
						}
					}
				}
				else if (sp.contains("/") || sp.contains("・") || sp.contains(",") || sp.contains("，")){
					String[] arr = null;
					arr = sp.split("[,，・/]");
					for(String tmp : arr) {
						if (isCeExists(tmp.trim(), lang, true)) { // VR3200，3201など。どちらかでもtrueならOK
							ret+="<a class=\""+button+"\"  target=\"_blank\" href=\"/overseas/international/"+lang+"/ce/index.html?dec="+modelNumber+"\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
							break;
						}
					}
				} else {
					if (isCeExists(modelNumber, lang, false)) {
						ret+="<a class=\""+button+"\"  target=\"_blank\" href=\"/overseas/international/"+lang+"/ce/index.html?dec="+modelNumber+"\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
					} else if (isCeExists(number, lang, false)) {
						ret+="<a class=\""+button+"\"  target=\"_blank\" href=\"/overseas/international/"+lang+"/ce/index.html?dec="+modelNumber+"\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
					} else {
						// すべて当てはまらない場合、入力されたリンクにする。
						ret+="<a class=\""+button+"\"  target=\"_blank\" href=\""+url+"\">"+messagesource.getMessage("tab.button.search.result", null,  locale)+"</a>";
					}
				}
				ret+="</td></tr>\r\n";
			}
		}
		return ret;
	}
	private boolean isCeExists(String number, String lang, boolean isConnected) {
		boolean ret = false;
		if (number == null || number.length() == 0) return ret;
		char[] chr = number.toCharArray();
		List<String> list = getCeList(chr[0], lang);
		if (list != null) {
			for(String num : list) {
				if (num.equals(number)) {
					ret = true;
					break;
				} else if (isConnected && num.indexOf(number) > -1 ) {
					ret = true;
					break;
				}
			}
			if (ret == false) {
				for(String num : list) {
					if (num.contains("/") || num.contains("／") || num.contains("・") || num.contains(",") ||num.contains("，")) {
						String[] arr = num.split("[,，・/／]"); // SJ3A6／絞り弁付真空破壊弁 SJ3A6/Vacuum Release Valve with Restrictor
						for (String tmp : arr) {
							if (number.equals(tmp)) {
								ret = true;
								break;
							}
						}
					}
				}
			}
		}
		if (ret == false) {
			list = getCeFileList(chr[0], lang);
			if (list != null) {
				if (isConnected) {
					List<String> numberList = getConnectedStringToList(number);
					for(String tmp : numberList) {
						for(String num : list) {
							if (num.indexOf("x") > -1) {
								String reg = num.replace("x", ".");
								if (tmp.matches(reg)) {
									ret = true;
									break;
								}
							} else if (num.contains("/") || num.contains("／") || num.contains("・") || num.contains(",") ||num.contains("，")) {
								String[] arr = num.split("[,，・/／]");
								for (String a : arr) {
									if (tmp.equals(a)) {
										ret = true;
										break;
									}
								}
							} else 	if (num.equals(tmp)) {
								ret = true;
								break;
							}
						}
					}
				} else {
					for(String num : list) {
						if (num.indexOf("x") > -1) {
							String reg = num.replace("x", ".");
							if (number.matches(reg)) {
								ret = true;
								break;
							}
						} else if (num.contains("/") || num.contains("／") || num.contains("・") || num.contains(",") ||num.contains("，")) {
							String[] arr = num.split("[,，・/／]"); // SJ3A6／絞り弁付真空破壊弁 SJ3A6/Vacuum Release Valve with Restrictor
							for (String tmp : arr) {
								if (number.equals(tmp)) {
									ret = true;
									break;
								}
							}
						} else {
							if (num.equals(number)) {
								ret = true;
								break;
							} else if (isConnected && num.indexOf(number) > -1 ) {
								ret = true;
								break;
							}
						}
					}
				}
			}
		}
		return ret;
	}
	private static final Pattern ALPHA_PATTERN = Pattern.compile("[a-zA-Z]+");
	private List<String> getConnectedStringToList(String number) {
		List<String> ret = null;
		String[] arr = number.split("[,，・/／]");
		String first = arr[0];
		Matcher matcher = ALPHA_PATTERN.matcher(first);
		if (matcher.lookingAt()) {
			String index = matcher.group();
			ret = new LinkedList<String>();
			int cnt = 0;
			for(String  tmp : arr) {
				if (cnt == 0) {
					ret.add(first);
				} else {
					ret.add(index + tmp);
				}
				cnt++;
			}
		}
		return ret;
	}

	private List<String> getCeList(char c, String lang) {
		List<String> ret = null;
		Map<String, List<String>> map = null;
		if (lang.equals("ja-jp")) {
			if (_ceSeriesMap == null) getCeMap(lang);
			map = _ceSeriesMap;
		}
		else if (lang.indexOf("en-") > -1) {
			if (_ceSeriesMapE == null) getCeMap(lang);
			map = _ceSeriesMapE;
		} else {
			if (_ceSeriesMapZH == null) getCeMap(lang);
			map = _ceSeriesMapZH;
		}
		ret = map.get(String.valueOf(c));
		return ret;
	}
	private List<String> getCeFileList(char c, String lang) {
		List<String> ret = null;
		Map<String, List<String>> map = null;
		if (lang.equals("ja-jp")) {
			if (_ceSeriesFileMap == null) getCeMap(lang);
			map = _ceSeriesFileMap;
		}
		else if (lang.indexOf("en-") > -1) {
			if (_ceSeriesFileMapE == null) getCeMap(lang);
			map = _ceSeriesFileMapE;
		} else {
			if (_ceSeriesFileMapZH == null) getCeMap(lang);
			map = _ceSeriesFileMapZH;
		}
		ret = map.get(String.valueOf(c));
		return ret;
	}
	// https://www.smcworld.com/overseas/international/ja-jp/ce/index.htmlで一覧を取得し、シリーズの頭文字毎のMapを作成
	private void getCeMap(String lang) {
		String tmp = "";
		if (lang.equals("ja-jp")) {
			tmp = LibOkHttpClient.getHtml(AppConfig.PageProdCeUrl);
			_ceSeriesMap = getMap(tmp);
			_ceSeriesFileMap = getFileMap(tmp);
		} else if (lang.indexOf("en-") > -1) {
			tmp = LibOkHttpClient.getHtml(AppConfig.PageProdCeUrl.replace("ja-", "en-"));
			_ceSeriesMapE = getMap(tmp);
			_ceSeriesFileMapE = getFileMap(tmp);
		} else {
			tmp = LibOkHttpClient.getHtml(AppConfig.PageProdCeUrl.replace("ja-jp", "zh-cn"));
			_ceSeriesMapZH = getMap(tmp);
			_ceSeriesFileMapZH = getFileMap(tmp);
		}
	}
	private Map<String, List<String>> getMap(String html)
	{
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		if (html != null && html.isEmpty() == false) {
			String[] arr = html.split("<tr");
			for(String tr : arr) {
				String[] tdList = tr.split("</td>");
				if (tdList.length > 1 ) {
					int start = tdList[0].indexOf("\">");
					if (start > -1) {
						String tmp = tdList[0].substring(start+2);
						char[] b = tmp.toCharArray();
						List<String> list = map.get(String.valueOf(b[0]));
						if (list == null) list = new ArrayList<String>();
						list.add(tmp.replace("</span>", "").trim());
						map.put(String.valueOf(b[0]), list);
					}
				}
			}
		}
		return map;

	}
	private Map<String, List<String>> getFileMap(String html)
	{
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		if (html != null && html.isEmpty() == false) {
			String[] arr = html.split("<tr");
			for(String tr : arr) {
				String[] tdList = tr.split("</td>");
				if (tdList.length > 1 ) {
					int start = tdList[2].indexOf("href=");
					if (start > -1) {
						String tmp = tdList[2].substring(start+6);
						tmp = tmp.substring(0, tmp.indexOf("\""));
						String[] arr2 = tmp.split("/");
						if (arr2.length > 1) {
							String sid = arr2[arr2.length-1].trim();
							char[] b = sid.toCharArray();
							List<String> list = map.get(String.valueOf(b[0]));
							if (list == null) list = new ArrayList<String>();
							list.add(sid.replace(".pdf", "").trim());
							map.put(String.valueOf(b[0]), list);
						}
					}
				}
			}
		}
		return map;
	}
	public String getGuideCatpan2026(String lang, List<String> titleList, List<String> slugList) {
		String catpan = "";
		String link = "/webcatalog/"+lang+"/";
		int cnt = 0;

		catpan += "<ul class=\"catpan_box text-sm mb24 px24\">";
		for(String title : titleList) {
			if (cnt % 2 == 0) catpan +="<li>\r\n";
			link += slugList.get(cnt) + "/";
			if (cnt % 2 == 1) catpan += " &gt; ";
			catpan += "  <a class=\"breadcrumb-item\" href=\""+link+"\">"+title+"</a>\r\n";
			if (cnt % 2 == 1) {
				catpan +="</li>\r\n";
				link = "/webcatalog/"+lang+"/";
			}

			cnt++;
		}
		catpan += "</ul>";
		
		return catpan;
	}

	private Locale getLocale(String lang) {
		Locale loc = Locale.JAPANESE;
		if (lang.indexOf("en") > -1) loc = Locale.ENGLISH;
		else if (lang.indexOf("zh") > -1)  loc = Locale.CHINESE;
		return loc;
	}
}
