package com.smc.omlist.service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smc.omlist.dao.OmlistRepository;
import com.smc.omlist.dao.OmlistTemplateImpl;
import com.smc.omlist.model.Omlist;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.MyErrors;
import com.smc.webcatalog.model.MyErrorsImpl;

import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OmlistServiceImpl implements OmlistService {

	@Autowired
	OmlistRepository repo;

	@Autowired
	OmlistTemplateImpl temp;

	@Autowired
	SeriesTemplateImpl seriesTemp;

	@Override
	public List<Omlist> searchKeyword(List<String> kw, String category, String div, String series, String lang) {
		List<Omlist> list = temp.search(kw, category, div, series, lang, null);
		return  list;
	}
	@Override
	public String getTableHtml(List<Omlist> list, String lang) {
		String ret = "";
		ret+="<table class=\"resulttbl sp_resulttbl\" cellpadding=\"0\" cellspacing=\"0\">\r\n" +
				"<tbody><tr>";
		if (lang.equals("en-jp")) {
			ret += "<th width=\"20%\">Symbol</th>\r\n" +
					"<th width=\"\">Specifications</th>\r\n" +
					"<th width=\"10%\">Download</th>\r\n" ;
		} else {
			ret += "<th width=\"20%\">型 式</th>\r\n" +
					"<th width=\"\">名 称</th>\r\n" +
					"<th>ダウンロード</th>\r\n" ;
		}
		ret += "</tr>";
		for (Omlist om : list) {
			ret += "<tr>\r\n" +
					"<td>"+om.getKata()+" "+om.getDiv()+"</td>\r\n" +
					"<td>"+om.getSpec()+"</td>\r\n" ;
			if (om.getFile() != null && om.getFile().isEmpty() == false)
					ret+="<td class=\"tdc\" nowrap=\"nowrap\"><a href=\"/upfiles/etc/custom/"+om.getFile()+"\" target=\"_blank\" class=\"ico_pdf\">PDF</a></td>\r\n" ;
			else ret+="<td></td>";
			ret += "</tr>";
		}
		ret += "</tbody></table>";
		return ret;
	}
	
	@Override
	public String getTableHtml2026(List<Omlist> list, String lang) {
		String ret = "<table class=\"table-hover s-full border-bottom border-right border-base-stroke-default border-collapse-collapse\">\r\n"
				+ "       <thead>";
		ret+="<tr>";
		if (lang.equals("en-jp")) {
			ret += "<th class=\"w20per py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">Symbol</th>\r\n" +
					"<th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">Specifications</th>\r\n" +
					"<th class=\"w15per py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">Download</th>\r\n" ;
		} else {
			ret += "<th class=\"w20per py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">型 式</th>\r\n" +
					"<th class=\"py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">名 称</th>\r\n" +
					"<th class=\"w15per py10 px12 bg-base-container-muted border-top border-left border-base-stroke-default text-sm leading-tight fw5\" scope=\"col\">ダウンロード</th>\r\n" ;
		}
		ret += "</tr>";
		ret += "</thead>\r\n";
		ret += "<tbody>\r\n";
		for (Omlist om : list) {
			ret += "<tr>\r\n" +
					"<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">"+om.getKata()+" "+om.getDiv()+"</td>\r\n" +
					"<td class=\"py10 px12 bg-base-container-default border-top border-left border-base-stroke-default text-xs leading-normal fw5\">"+om.getSpec()+"</td>\r\n" ;
			if (om.getFile() != null && om.getFile().isEmpty() == false) {
				ret+="<td class=\"bg-base-container-default border-top border-left border-base-stroke-default word-break-word py10 px12\">"
					+ "<div class=\"f fc\">"
					+ "  <a class=\"f fm gap-4\" target=\"_blank\" href=\"/upfiles/etc/custom/"+om.getFile()+"\">"
					+ "    <span class=\"text-primary text-sm leading-tight fw5 hover-link-underline\">PDF</span>"
					+ "    <img class=\"s16 object-fit-contain\" src=\"/assets/smcimage/common/external-link.svg\" alt=\"\" title=\"\">"
					+ "  </a>"
					+ "</div>"
					+ "</td>";
			}
			else ret+="<td></td>";
			ret += "</tr>";
		}
		ret += "</tbody></table>";
		return ret;
	}


	@Override
	public MyErrors checkFormat(String fullpath, String enc, int colSize) {

		MyErrors ret = new MyErrorsImpl();

		int c = 0;

		try{
			//1行ずつチェック
			CSVReader reader = createCSVReader(fullpath,0,enc);
			String [] col;

			while ((col = reader.readNext()) != null) {
				c++;
				if(col.length < colSize){
					ErrorObject e = new ErrorObject();
					e.setCode(ErrorCode.E10005);
					e.setMessage("カラム数エラー:"+c+"行目");
					ret.addError(e);
				}
			}


		}catch(Exception ex){
			ErrorObject e = new ErrorObject();
			e.setCode(ErrorCode.E99999);
			e.setMessage("その他のエラー");
			ret.addError(e);

		}
		if (c == 0) {
			ErrorObject e = new ErrorObject();
			e.setCode(ErrorCode.E99999);
			e.setMessage("読み取りエラー");
			ret.addError(e);
		}
		return ret;
	}

	@Override
	public int importItem(String path, String lang,String enc){

		log.info("============START "+lang+" import start.");

		repo.deleteAllByLang(lang); // 全部削除してから登録

		int c = 0;
		int num  = 0;

		if(lang.equals("en-jp")){
			num = 20000;
		}else if(lang.indexOf("zh") > -1){
		    num = 50000;
		}

		try{

			List<Omlist> list = new LinkedList<Omlist>();
			List<String> customList = new ArrayList<String>();
			List<String> omList = new ArrayList<String>();

			List<String[]> line = createCSVReader(path,1,enc).readAll();
			for(String[] col:line){

				c++;
				num++;

				Omlist i = new Omlist();

				// 日本語と英語は同じファイルで列が違う。
				if (lang.equals("ja-jp")) {
					i.setCategory(col[0]);
					i.setDiv(col[2]);
					i.setKata(col[5]);
					i.setSpec(col[7]);
					i.setFile(col[9]);
					i.setBestnum(col[11]);
					i.setBestpage(col[12]);
					String[] ids = col[13].split("/");
	                String idss = "";
	                for(String _s:ids){
	                   _s = _s.trim();
	                   idss+="【"+_s+"】";
		                if (i.getDiv().equals("簡易特注")) {
		                	customList.add(_s);
		                } else {
		                	omList.add(_s);
		                }
	                }
	                i.setIds(idss);
				} else {
					i.setCategory(col[1]);
					i.setDiv(col[3]);
					i.setKata(col[6]);
					i.setSpec(col[8]);
					i.setFile(col[10]);
					i.setBestnum("");
					i.setBestpage("");
					String[] ids = col[14].split("/");
	                String idss = "";
	                for(String _s:ids){
	                   _s = _s.trim();
	                   idss+="【"+_s+"】";
	                   if (i.getDiv().equals("Simple Specials")) {
		                	customList.add(_s);
		                } else {
		                	omList.add(_s);
		                }
	                }
	                i.setIds(idss);
				}
				i.setLang(lang);
				i.setActive(true);
				list.add(i);
			}
			repo.saveAll(list);

			// webcatalog.series フラグを一旦全部落として、アップ！
			seriesTemp.updateCustom(customList, lang);
			seriesTemp.updateOrderMade(omList, lang);


		}catch(Exception ex){
			log.error(ex.getMessage());
		}finally{
			log.info("============END "+c+" items imported.");
		}
		return c;
	}

	// ========== private ==========
	private static CSVReader createCSVReader(String datafilepath,int start_line,String enc){

		CSVReader reader = null;
		try{
			reader =  new CSVReader(new InputStreamReader(new FileInputStream(datafilepath),enc),',','"',start_line);
		}catch(Exception ex){
			log.error(ex.getMessage());
		}
		return reader;
	}



}
