package com.smc.webcatalog.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;


public class ImpSeries extends ImpAbstractEntity implements Serializable {

	private String name;
	private String name2;
	private String text;
	private String text2;

	private String contain;
	private String more;
	private String webcatalog;

	private Integer order;
	private Map<String,ImpSeriesMap> seriesmap = new LinkedHashMap<String,ImpSeriesMap>();//<カラム名称,Sereismap>

	private Integer ca_id;

	private Integer ca_parent_order;

	private Long cs_id;
	private Integer cs_ca_id;
	private Integer cs_order;


	private List<ImpItem> items = new LinkedList<ImpItem>();

	private ImpCategory category;
	private List<ImpCategory> categories = new LinkedList<ImpCategory>();
	private List<List<ImpCatpan>> catpans = new LinkedList<List<ImpCatpan>>();

	private String lang ="ja";

	private Integer search_idx = 0;

	private String opt1;
	private String catpans_txt;
	private boolean hascad3d = false;

	private boolean has_cy_custom;
	private boolean has_cy_om;

	private String ml_id;

	/**
	 * ImpSeries to Series
	 * @param lang
	 * @param langRefId
	 * @param stateId
	 * @param state
	 * @param linkMaster // 言語ごとのSeriesLinkMasterのList
	 * @param user
	 * @return
	 */
	public Series getSeries(String lang, String langRefId, String stateId, ModelState state, List<SeriesLinkMaster> linkMaster, User user)
	{
		Series s = new Series();
		s.setOldId(String.valueOf(getId()));
		s.setName(name);
		s.setNumber(name2);
		s.setModelNumber(getSid());

		s.setBreadcrumb(getCatpans_txt()); // DBそのまま。
		s.setOther(getOpt1());
		s.setDetail(text);
		s.setAdvantage(getShow_page_path()); // 製品特長
		s.setKeyword(keyword.split(","));
		s.setImage(getImg1()); // 画像

		String spec = "[[";
		for (ImpSeriesMap map : seriesmap.values())
		{
			if (map.active) spec+="\""+map.getName()+"\",";
		}
		// 2021/11/07 itemの2DCADなどの情報も一緒に保持
		//spec = spec.substring(0, spec.length()-1);
		spec += "\"[2DCAD]\",\"[Manual]\",\"[Manifold]\",\"[DoC]\",";
		spec += "],[";
		int cnt = 0;
		int max = items.size();
		for(ImpItem item : items) {
			Map<String,ImpSeriesMap> map = item.getSeriesmap();
			for(ImpSeriesMap val: map.values())
			{
				if (val != null) {
					if (val.active && val.getValue() != null) spec+="\""+val.getValue().trim()+"\",";
				}
			}
			//spec = spec.substring(0, spec.length()-1);
			if (item.link2 != null && item.link2.isEmpty() == false) {
				spec += '"'+item.link2+"\","; // 2DCAD
			}
			else spec += "\"\",";
			if (item.link1 != null) spec += '"'+item.link1+"\","; // 取説
			else spec += "\"\",";
			if (item.link4 != null) spec += '"'+item.link4+"\","; // マニフォールド
			else spec += "\"\",";
			if (item.link5 != null) spec += '"'+item.link5+"\""; // CE自己宣言書
			else spec += "\"\"";
			cnt++;
			if (max != cnt) spec += "],[";
		}
		spec+= "]]";
		s.setSpec(spec);

		// SeriesLink
		// LinkMasterの順番を要確認！
		List<SeriesLink> sLink = new LinkedList<SeriesLink>();
		for(SeriesLinkMaster sm: linkMaster) {
			if (sm.getOrder() == 0) { // カタログ閲覧
				String tmp = getCatalog_pdf_link();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 1) { // 貸出サービス
				String tmp = getLink5();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 2) { // モーターレス仕様
				String tmp = getLink8();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 3) { // 選定ソフト
				String tmp = getLink9();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 4) { // アプリケーション例
				String tmp = getLink11();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 5) { // 設定ソフトウェア
				String tmp = getLink12();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 6) { // パッキンセット
				String tmp = getLink13();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 7) { // エレメント交換案内
				String tmp = getLink14();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 8) { // 動画
				String tmp = getLink15();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 9) { // IODD
				String tmp = getLink17();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 10) { // ロック解除ユニット
				String tmp = getLink18();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 11) { // icon オーダーメイド仕様
				String tmp = getLink19();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 12) { // icon メンテナンスサービス
				String tmp = getLink6();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}
			} else if (sm.getOrder() == 13) { // icon 製品View
				String tmp = getLink20();
				if (tmp != null && tmp.equals("") == false) {
					SeriesLink sl = new SeriesLink(sm, lang, user, tmp, state);
					sLink.add(sl);
				}

			// 2021/11/07
			//	2DCAD マニホールド仕様書
			// 取扱説明書 自己宣言書
			// 以上はspecのjsonに持たせる。
			/*} else if (sm.getOrder() == 52) { // "2DCAD"
				if (getHas2DCAD()) {
					SeriesLink sl = new SeriesLink(sm, lang, user, getHas2DCADStr(), state);
					sLink.add(sl);
				}
			//} else if (sm.getOrder() == 13) { //  "3DCAD" // 2020/11/12 要らない se_idを渡すだけ。なので、下の s.setCad3d(hascad3d) のみでOK
			//	if (getHas3DCAD()) {
			//		SeriesLink sl = new SeriesLink(sm, lang, user, getHas3DCADStr(), state);
			//		sLink.add(sl);
			//	}
			} else if (sm.getOrder() == 53) { // "取扱説明書" // HeartCore
				if (getHasManual()) {
					List<String> list =  getHasManualList();
					for (String mani: list) {
						SeriesLink sl = new SeriesLink(sm, lang, user, mani, state);
						sLink.add(sl);
					}
				}
			} else if (sm.getOrder() == 54) { // "マニホールド仕様書"
				if (getHasManifold()) {
					Map<String, String> list =  getHasManifoldList();
					for (String mani: list.keySet()) {
						String val = list.get(mani);
						SeriesLink sl = new SeriesLink(sm, lang, user, val, state);
						sl.setModelNumber(mani);
						sLink.add(sl);
					}
				}
			} else if (sm.getOrder() == 55) { // "自己宣言書" // HeartCore
				if (getHasDc()) {
					List<String> list =  getHasDcList();
					for (String mani: list) {
						SeriesLink sl = new SeriesLink(sm, lang, user, mani, state);
						sLink.add(sl);
					}
				}*/
			}

		}
		s.setLink(sLink);

		s.setCad3d(hascad3d); // 3DCAD
		s.setOrderMade(has_cy_om); // オーダーメイド
		s.setCustom(has_cy_custom); // 簡易特注

		if (order != null) s.setOrder(order);
		s.setActive(active);
		s.setCtime(ctime);
		s.setMtime(mtime);

		s.setLang(lang);
		s.setLangRefId(langRefId);
		s.setState(state);
		s.setStateRefId(stateId);
		s.setUser(user);
		return s;
	}

	// インポート用。JSY-EからJSYに戻す処理。ja-jpのIDを取得するため。
	public String getSidN(String lang)
	{
		String sid = getSid();
		String prefix = "-E";
		if (lang.contentEquals("ja-jp") == false)
		{
			if (lang.contentEquals("zh-cn")) prefix = "-ZH";
			if (lang.contentEquals("zh-tw")) prefix = "-ZHTW";
			String tmp = sid.replace(prefix, "");
			if ((tmp+prefix).length() < sid.length() )
			{
				sid = sid.substring(0, sid.length() - prefix.length());
			}
			else
			{
				sid = tmp;
			}
		}
		return sid;
	}

	Pattern p = Pattern.compile("^%(\\d+)%(.*)$");


	//ダウンロードを表示するか
	public boolean getPrintDownload(){
		boolean b = false;

		if(getHas2DCAD()||getHas3DCAD()||getHasManual()||getHasManifold()||getHasManual()||getHasDc()){
			b = true;
		}
		if(!StringUtils.isEmpty(getLink1()) && getLink1().indexOf("/upfiles/")==-1){
			b = true;
		}

		return b;
	}


	//カタログpdfのパス変換
	public String getCatalog_pdf_link(){

		String s = "";

		if(!StringUtils.isEmpty(getLink10())){
			s = ImpSetPdfSizeAction.getPdfPath(getLink1());
		}


		return s;

	}

	public String getCatalog_pdf_link_NEWBEST(){

		String s = "";

		if(!StringUtils.isEmpty(getLink10())){
			s = ImpSetPdfSizeAction.getPdfPath_NEWBEST(getLink1());
		}


		return s;

	}

	//3DCAD検索用のリンクを作成する
	public String getCad3d_link(){

		String s = "";
		String andor = "or";
		String l7 = getLink7();
		if(l7!=null){

			l7 = l7.replaceAll("\\s+", " ");
			l7 = l7.replaceAll(" ", "+");

			if(l7.matches(".*\\[AND\\].*")){
				//Log.log("match+++++++++++");
				l7 = l7.replaceAll("\\[AND\\]", "");
				andor="and";
			}
			l7 = l7.replaceAll("\\+$", "");

			s = "k="+l7+"&andor="+andor;
		}

		return s;

	}



	//新製品情報の詳細をガイド中に呼び出す
	public String getShow_page_path(){

		String path = "";

		if(getLink_new()!=null&&getLink_new().matches("\\[.*\\]")){
			path = getLink_new().replace("[", "");
			path = path.replace("]" ,"");
		}
		if(getLink_new()!=null&&getLink_new().matches("@@@.*@@@")){
		    path = getLink_new().trim();

		}
		//Log.log("se.getShowPagePath="+path);
		return path;

	}

    //HeartCoreのページをインクルードする用
    public String getHc_pageid(){

        String path = "";

        if(getLink_new()!=null&&getLink_new().matches("@@@.*@@@")){
            path = getLink_new().replaceAll("@@@", "");
            path = path.trim();
        }
        //Log.log("se.getShowPagePath_hc="+path);
        return path;

    }

	//テーブルを表示するか
	public boolean getShowTable(){


		boolean b = true;
		int count = 0;
		if(getSeriesmap()!=null){
			for(ImpSeriesMap map:seriesmap.values()){
				if(!map.getName().equals("ID")&&!map.getName().equals("特長")&&map.getActive()){
					count ++;
				}
			}
		}

		if(count==0){
			b=false;
		}
		//Log.log(getName2()+" c="+count);


		return b;
	}


	//総カタのみ機能
	public String getLink_new() {

		String s = "";
		s = getLink3();
		if(s!=null){

			if(s.matches("^/docs/.*")){
				s  = s.replace("/docs/new", "");
				//if(getLang().equals("ja")){
					s = "/docs/new/ja.jsp?URL="+s;
				//}else{
				//	s = "/docs/new/en.jsp?URL="+s;
				//}
			}
		}

		//Log.log(s);
		return s;
	}

	/*
	 *
【シリーズ】
・リンク１　iconカタログ閲覧
・リンク２　未使用
・リンク３　製品特長
・リンク４　未使用
・リンク５　貸出サービス
・リンク６　未使用

・リンク８　iconモーターレス仕様
・リンク９　Icon選定ソフト
・リンク10　未使用。PDFの容量？
・リンク11　iconアプリケーション例(youtube)
・リンク12　icon設定ソフトウェア
・リンク13　iconパッキンセット
・リンク14　iconエレメント交換案内
・リンク15　icon動画(youtube)
・リンク16　未使用。（ZP2）で使っていた。
・リンク17　iconIODD
・リンク18　iconロック解除ユニット
・リンク19　iconオーダーメイド仕様

【製　品】
・リンク１　取扱い説明書
・リンク２　２Ｄ　ＣＡＤ
・リンク３　３Ｄ　ＣＡＤ
・リンク４　マニホールド仕様書
・リンク５　ＣＥ自己宣言書
・リンク６　未使用


	 */

	//自己宣言書あるか
	public boolean getHasDc(){

		boolean b = false;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink5())){
					b= true;
				}
			}
		}
		return b;
	}
	public boolean getHasManifold(){

		boolean b = false;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink4())){
					b= true;
				}
			}
		}
		return b;
	}


	//総カタのみ機能
	public boolean getHas2DCAD(){

		boolean b = false;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink2())){
					b= true;
				}
			}
		}
		return b;
	}

	//総カタのみ機能
	public boolean getHas3DCAD(){

		boolean b = false;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink3())){
					b= true;
				}
			}
		}
		return b;
	}

	//総カタのみ機能
	public boolean getHasManual(){

		boolean b = false;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink1())){
					b= true;
				}
			}
		}
		return b;
	}

	//総カタのみ機能



	public String getEcho(String s){
		return s;
	}

	public int getImpItemsize(){
		int i = 0;
		if(items!=null) i = items.size();
		return i;
	}

	public String getText_html(){
		String s ="";
		if(getText()!=null){
			s = getText().replace("\r\n", "<br/>");
		}
		return s;
	}

	public ImpSeriesMap getFirst(){
		List<ImpSeriesMap> list = new LinkedList<ImpSeriesMap>();

		for(ImpSeriesMap _map:seriesmap.values()){
			list.add(_map);
		}

		return list.get(0);
	}

	//カラム名から取得
	public ImpSeriesMap getMapByCname(String cname){
		ImpSeriesMap map = null;
		for(ImpSeriesMap _map:seriesmap.values()){
			if(_map.getCname().equals(cname)){
				map = _map;
			}
		}
		return map;
	}

	//カラム名(ラベル)から取得
	public String getValueByName(String name){

		String s = "";
		ImpSeriesMap map = null;
		for(ImpSeriesMap _map:seriesmap.values()){
			if(_map.getName().equals(name)){
				map = _map;
			}
		}
		if(map!=null) s = map.getValue();
		return s;
	}


	public void addSeriesMap(ImpSeriesMap map){
		seriesmap.put(map.getName(), map);
	}

	public Collection<ImpSeriesMap> getSeriesMapList(){

		return seriesmap.values();
	}

	public String getName() {
		return name;
	}

	public String getName_h3(){

		String s = "";
		if(name!=null){
			s = name.replaceAll("\\[", "<span class=\"small\">");
			s = s.replaceAll("\\]", "</span>");

		}
		return s;

	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName2() {
		return name2;
	}

	public void setName2(String name2) {
		this.name2 = name2;
	}


	public Map<String, ImpSeriesMap> getSeriesmap() {
		return seriesmap;
	}

	public void setSeriesmap(Map<String, ImpSeriesMap> seriesmap) {
		this.seriesmap = seriesmap;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}


	public String getText2() {
		return text2;
	}


	public void setText2(String text2) {
		this.text2 = text2;
	}

	public List<ImpItem> getItems() {
		return items;
	}

	public void setItems(List<ImpItem> items) {
		this.items = items;
	}

	public int getCa_id() {
		return ca_id;
	}

	public void setCa_id(int ca_id) {
		this.ca_id = ca_id;
	}


	public String getContain() {
		return contain;
	}

	public void setContain(String contain) {
		this.contain = contain;
	}

	public String getMore() {
		return more;
	}

	public void setMore(String more) {
		this.more = more;
	}

	public String getWebcatalog() {
		return webcatalog;
	}

	public void setWebcatalog(String webcatalog) {
		this.webcatalog = webcatalog;
	}


	public int getCs_order() {
		return cs_order;
	}




	public void setCs_order(Integer cs_order) {
		this.cs_order = cs_order;
	}


	public Long getCs_id() {
		return cs_id;
	}


	public void setCs_id(Long cs_id) {
		this.cs_id = cs_id;
	}


	public List<ImpCategory> getCategories() {
		return categories;
	}


	public void setCategories(List<ImpCategory> categories) {
		this.categories = categories;
	}


	public ImpCategory getImpCategory() {
		return category;
	}


	public void setImpCategory(ImpCategory category) {
		this.category = category;
	}


	public Integer getCs_ca_id() {
		return cs_ca_id;
	}


	public void setCs_ca_id(Integer cs_ca_id) {
		this.cs_ca_id = cs_ca_id;
	}




	public void setCa_id(Integer ca_id) {
		this.ca_id = ca_id;
	}


	public Integer getOrder() {
		return order;
	}


	public void setOrder(Integer order) {
		this.order = order;
	}


	public String getLang() {
		return lang;
	}


	public void setLang(String lang) {
		this.lang = lang;
	}


	public Integer getSearch_idx() {
		return search_idx;
	}


	public void setSearch_idx(Integer search_idx) {
		this.search_idx = search_idx;
	}


	public String getOpt1() {
		return opt1;
	}


	public void setOpt1(String opt1) {
		this.opt1 = opt1;
	}


	public Integer getCa_parent_order() {
		return ca_parent_order;
	}



	public void setCa_parent_order(Integer ca_parent_order) {
		this.ca_parent_order = ca_parent_order;
	}



	public List<List<ImpCatpan>> getCatpans() {
		return catpans;
	}



	public void setCatpans(List<List<ImpCatpan>> catpans) {
		this.catpans = catpans;
	}


	public String getCatpans_txt(){
		String s = "";

		for(List<ImpCatpan> line:catpans){
			s += "●";
			for(ImpCatpan p:line){
				s+="%"+p.getId()+"%"+p.getName()+"#";
			}
		}

		s = s.replaceAll("【総合[^】]*】", "");

		return s;

	}

	public void setCatpans_txt(String s){

		if(s!=null){


			StringTokenizer st = new StringTokenizer(s,"●");
			catpans.clear();
			while(st.hasMoreTokens()){
				List<ImpCatpan> line = new ArrayList<ImpCatpan>();
				StringTokenizer st2 = new StringTokenizer((String)st.nextToken(),"#");
				while(st2.hasMoreTokens()){
					String cattxt = (String)st2.nextElement();
					//Log.log(cattxt);


					Matcher m = p.matcher(cattxt);
					if(m.find()){
						ImpCatpan pan = new ImpCatpan();
						String id = m.group(1);
						String name = m.group(2);
						pan.setId(Integer.parseInt(id));
						pan.setName(name);
						line.add(pan);
					}
				}
				catpans.add(line);
			}

		}

	}


	public boolean isHascad3d() {
		return hascad3d;
	}


	public void setHascad3d(boolean hascad3d) {
		this.hascad3d = hascad3d;
	}


    public boolean isHas_cy_custom() {
        return has_cy_custom;
    }


    public void setHas_cy_custom(boolean has_cy_custom) {
        this.has_cy_custom = has_cy_custom;
    }


    public boolean isHas_cy_om() {
        return has_cy_om;
    }


    public void setHas_cy_om(boolean has_cy_om) {
        this.has_cy_om = has_cy_om;
    }


    public String getMl_id() {
        return ml_id;
    }


    public void setMl_id(String ml_id) {
        this.ml_id = ml_id;
    }


    // ====== Import Add ====
	//自己宣言書あるか
	public String getHasDcStr(){

		String b = null;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink5())){
					b= i.getLink5();
				}
			}
		}
		return b;
	}
	public List<String> getHasDcList(){

		List<String> b = null;
		if(items!=null){
			b = new LinkedList<String>();
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink5())){
					b.add( i.getLink5());
				}
			}
		}
		return b;
	}
	public Map<String, String> getHasManifoldList(){

		Map<String, String> b = null;
		if(items!=null){
			b = new HashMap<String, String>();
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink4())){
					b.put( i.getSid(), i.getLink4());
				}
			}
		}
		return b;
	}
	public List<String> getHas2DCADList(){

		List<String> b = null;
		if(items!=null){
			b = new LinkedList<String>();
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink2())){
					b.add( i.getLink2());
				}
			}
		}
		return b;
	}

	//総カタのみ機能
	public String getHas2DCADStr(){

		String b = null;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink2())){
					b= i.getLink2();
				}
			}
		}
		return b;
	}

	//総カタのみ機能
	public String getHas3DCADStr(){

		String b = null;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink3())){
					b= i.getLink3();
				}
			}
		}
		return b;
	}

	//総カタのみ機能
	public String getHasManualStr(){

		String b = null;
		if(items!=null){
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink1())){
					b= i.getLink1();
				}
			}
		}
		return b;
	}
	public List<String> getHasManualList(){

		List<String> b = null;
		if(items!=null){
			b = new LinkedList<String>();
			for(ImpItem i:items){
				if(!StringUtils.isEmpty(i.getLink1())){
					b.add( i.getLink1());
				}
			}
		}
		return b;
	}


}
