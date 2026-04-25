package com.smc.cad3d.service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smc.cad3d.dao.Cad3dRepository;
import com.smc.cad3d.dao.Cad3dTemplateImpl;
import com.smc.cad3d.model.Cad3d;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.MyErrors;
import com.smc.webcatalog.model.MyErrorsImpl;

import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Cad3dServiceImpl implements Cad3dService {

	@Autowired
	Cad3dRepository repo;

	@Autowired
	Cad3dTemplateImpl temp;

	@Autowired
	SeriesTemplateImpl seriesTemp;

	@Override
	public List<Cad3d> searchKeyword(List<String> kw, String c1c2, String series, String lang) {
		List<Cad3d> list = temp.search(kw, c1c2, series, lang, null);
		return  list;
	}

	@Override
	public List<Cad3d> searchIndex(String index, String c1c2, String series, String lang) {
		List<Cad3d> list = temp.searchIndex(index, c1c2, series, lang, null);
		return  list;
	}

	@Override
	public List<Cad3d> searchUrl(String ppath, String lang) {
		List<Cad3d> list = temp.searchUrl(ppath, lang, null); // CSVにactiveは無い。
		return  list;
	}

	@Override
	public List<Cad3d> search(String sid, String series, String lang, String cat) {
		List<Cad3d> list = temp.search(sid, series, lang, cat);
		return  list;
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
	public int importItem(String path,String lang,String enc){

		log.info("============START "+lang+" import start.");

		repo.deleteAllByLang(lang); // 全部削除してから登録

		int c = 0;

		if(lang.equals("en-jp")){
			c = 25000;
		}else if(lang.indexOf("zh") > -1){
		    c = 50000;
		}

		try{

			List<Cad3d> list = new LinkedList<Cad3d>();
			List<String> listIds = new ArrayList<String>();

			List<String[]> line = createCSVReader(path,1,enc).readAll();
			for(String[] col:line){

				if(StringUtils.isNotEmpty(col[1])){

					c++;
					Cad3d cad = new Cad3d();
					cad.setNum(String.valueOf(c));
					cad.setLang(lang);
					cad.setC1(col[0].trim());
					cad.setC2(col[1].trim());
					cad.setC3(col[2].trim());
					cad.setC4(col[3].trim());
					cad.setC5(col[4].trim());

					cad.setSeries(col[5]);
					cad.setCat(col[6]);
					cad.setItem(col[7]);
					cad.setName(col[8]);

					String[] ids = col[9].split("/");
					String idss = "";
					for(String _s:ids){
					    _s = _s.trim();
						idss+="【"+_s+"】";

						listIds.add(_s);
					}

					cad.setIds(idss);
					cad.setUrl1(col[10]);
					cad.setNewurl(col[13]);
					cad.setUrl2(col[15]);

					//リンク文言
					cad.setNewmsg(col[11]);

					if (col[12] != null && col[12].isEmpty() == false) {
						String[] newids = col[12].split("/");
						String newidss = "";
						for(String _s:newids){
							newidss+="【"+_s+"】";
						}
						cad.setNewids(newidss);
					}

					list.add(cad);

				}
			}
			repo.saveAll(list);

			// webcatalog.series cad3dフラグを一旦全部落として、アップ！
			seriesTemp.updateCad3D(listIds, lang);


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
