package com.smc.webcatalog.model;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.codec.net.URLCodec;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.smc.webcatalog.config.AppConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "series")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class Series extends BaseModel {

	public Series() {

	}

	private String oldId; // 旧システムのID mylist imageフォルダなどで利用 // 99999で準備中表示

	private String name;
	private String number; // ZP2 など、シリーズ共通の型番

	private String modelNumber; // SeriesID LEYG_D-ZH (末尾で言語 -E -ZH -ZHTW 日本は無し)

	private String[] keyword; // 検索用

	private String detail; // 説明

	private String advantage; // 製品特長

	private String other; // その他資料 (HTML)
	private String spec; // 旧Item 配列 (ex) [["ColA","ColB"],["test","NewB1"],["NewA2","NewB2"]]

	private String breadcrumb;
	private String image; // 画像のPath

	private boolean cad3d; // リンクが設定されていてもこのフラグが無ければ表示しない。
	private boolean custom; // 簡易特注
	private boolean orderMade; // オーダーメイド

	private String notice;
	private String imageTop;
	private String imageBottom;

	// CategorySeries
	@Transient
	private List<CategorySeries> categorySeries;

	// SeriesLink
	@Transient
	private List<SeriesLink> link; // CAD 取扱説明書 自己宣言書 カタログPDF など

	public boolean isPre() { // 「msg.search.pre=該当の製品は準備中です。」の表示。検索で引っかかってしまう問題があった。
		return (oldId != null && oldId.equals("-1"));
	}

	URLCodec codec = new URLCodec("UTF-8");
	public String getDispModelNumber() {
		String ret = "";
		if (modelNumber.contains("/")) {
			ret = modelNumber.replace("/", "%20");
		}
		return ret;
	}
	// TESTからPROD用にパラメータをコピー
	// id stateRefId langRefIdはここではコピーしない
	public void setUpdateParam(Series s) {
		oldId = s.getOldId();
		name = s.getName();
		number = s.getNumber();
		modelNumber = s.getModelNumber();
		keyword = s.getKeyword();
		detail = s.getDetail();
		advantage = s.getAdvantage();
		other = s.getOther();
		spec = s.getSpec();
		breadcrumb = s.getBreadcrumb();
		image = s.getImage();
		imageTop = s.getImageTop();
		imageBottom = s.getImageBottom();
		cad3d = s.isCad3d();
		custom = s.isCustom();
		orderMade = s.isOrderMade();
		notice = s.getNotice();
		setLang(s.getLang());
		setOrder(s.getOrder());
		setActive(s.isActive());
	}

	public String getBaseModelNumber() {
		String ret = this.modelNumber;
		if (super.getLang() != null && super.getLang().equals("ja-jp") == false) {
			String lang = super.getLang();
			if (lang.indexOf("en-") > -1) {
				int e = ret.lastIndexOf("-E");
				if (e  > -1) ret = ret.substring(0,e); 
			} else if (lang.equals("zh-tw") ) {
				int e = ret.lastIndexOf("-ZHTW");
				if (e  > -1) ret = ret.substring(0,e); 
			} else {
				int e = ret.lastIndexOf("-ZH");
				if (e  > -1) ret = ret.substring(0,e); 
			}
		}
		return ret;
	}
	/**
	 * slugがnullなら最初の、null以外なら一致するslugのパンくず取得
	 * 主に表示カテゴリが決まっていないシリーズ表示から呼ばれる
	 * @param slug null OK
	 * @return [0] slug [1] slug2
	 */
	public String[] getCatpansSlug(String slug){

		String[] ret = new String[2];
		if(breadcrumb!=null){

			boolean isFind = false;
			StringTokenizer st = new StringTokenizer(breadcrumb,"●");
			while(st.hasMoreTokens()){
				StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
				int cnt = 0;
				while(st2.hasMoreTokens()){
					String cattxt = (String)st2.nextElement();

					String[] arr = cattxt.split("%");
					if(arr.length >= 2) {
						if (slug == null || slug.equals(arr[1])) isFind = true;
						ret[cnt] = arr[1];
						cnt++;
						if (cnt >= 2) break;
					}
				}
				if (isFind) break;
			}
		}
		return ret;
	}

	// list.html用。breadclumbが複数あった場合、今のカテゴリを選択されるように１つにしておく
	public void setCatpansString(String slug) {
		if(breadcrumb!=null){

			String[] arr = breadcrumb.split("●");

			for(String str : arr) {
				if (str.indexOf("%"+slug+"%") > -1) {
					breadcrumb = "●" + str;
					break;
				}
			}
		}
	}

	public String getCatpansString()
	{
		String[] arr = getCatpansSlug(null);
		String ret = "";
		for(String a : arr) {
			ret += a + "/";
		}
		return ret;
	}

	/**
	 *  パンくず用のHTML
	 * ●%jajp0%方向制御機器#%jajp4%省配線フィールドバスシステム(シリアル伝送システム)#
	 */
	public List<String> getCatpansHtml(String lang)
	{
		List<String> ret = null;
		if(breadcrumb!=null) {
			ret = new LinkedList<String>();
			StringTokenizer st = new StringTokenizer(breadcrumb,"●");
			while(st.hasMoreTokens()){
				String catpan = "";
				String before = "";
				StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
				while(st2.hasMoreTokens()){
					String cattxt = (String)st2.nextElement();

					String[] arr = cattxt.split("%");
					if(arr.length > 2) {
						if (catpan.length() == 0) catpan+= "<a href='" + AppConfig.ContextPath + "/"+lang+"/"+arr[1]+"/'>"+arr[2]+"</a>";
						else  catpan+= "&nbsp;»&nbsp;<a href='"+AppConfig.ContextPath+"/"+lang+"/"+before+"/"+arr[1]+"/'>"+arr[2]+"</a>";
						before = arr[1];
					}
				}
				ret.add(catpan);
			}
		}
		return ret;
	}
	/**
	 * パンくず用のタイトルとリンクリスト
	 * @param listTitle
	 * @param listSlug
	 * @return
	 */
	public boolean getCatpansTitleAndLink(List<String> listTitle, List<String> listSlug)
	{
		boolean ret = false;
		if(breadcrumb != null) {
			StringTokenizer st = new StringTokenizer(breadcrumb,"●");
			while(st.hasMoreTokens()){
				StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
				while(st2.hasMoreTokens()){
					String cattxt = (String)st2.nextElement();

					String[] arr = cattxt.split("%");
					if(arr.length > 2) {
						listTitle.add(arr[2]);
						listSlug.add(arr[1]);
						ret = true;
					}
				}
			}
		}
		return ret;
	}

	/**
	 *  型式、頭文字検索用。
	 *  分類で 方向制御機器 /<br>省配線フィールドバスシステム(シリアル伝送システム)
	 * ●%jajp0%方向制御機器#%jajp4%省配線フィールドバスシステム(シリアル伝送システム)#
	 */
	public String getCatpansText()
	{
		String catpan = "";
		if(breadcrumb!=null) {
			StringTokenizer st = new StringTokenizer(breadcrumb,"●");
			while(st.hasMoreTokens()){
				String before = "";
				StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
				while(st2.hasMoreTokens()){
					String cattxt = (String)st2.nextElement();

					String[] arr = cattxt.split("%");
					if(arr.length > 2) {
						if (catpan.length() == 0) catpan+= arr[2]+" /<br>";
						else  catpan+= arr[2];
					}
				}
			}
		}
		return catpan;
	}

}
