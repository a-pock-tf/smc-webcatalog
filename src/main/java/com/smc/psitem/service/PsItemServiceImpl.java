package com.smc.psitem.service;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smc.psitem.dao.PsItemRepository;
import com.smc.psitem.dao.PsItemTemplateImpl;
import com.smc.psitem.model.PsItem;
import com.smc.webcatalog.config.ErrorCode;
import com.smc.webcatalog.dao.SeriesTemplateImpl;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.MyErrors;
import com.smc.webcatalog.model.MyErrorsImpl;
import com.smc.webcatalog.model.Series;

import au.com.bytecode.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PsItemServiceImpl implements PsItemService {

	@Autowired
    PsItemRepository repo;

	@Autowired
	PsItemTemplateImpl temp;

	@Autowired
	SeriesTemplateImpl seriesTemp;

	@Override
	public List<PsItem> searchKeyword(List<String> kw, String condition, String c1c2, String series, String lang) {
		List<PsItem> list = temp.search(kw, condition, c1c2, series, lang, true);
		return  list;
	}

	@Override
	public List<PsItem> searchIndex(String index, String c1c2, String series, String lang) {
		List<PsItem> list = temp.searchIndex(index, c1c2, series, lang, null);
		return  list;
	}
	
	@Override
	public List<PsItem> searchKeywordAndOr(String[] kwArr, String lang, String cd, int max, Boolean active) {
		
		List<PsItem> list = temp.searchAndOr(kwArr, lang, cd, max, active);
		return list;
	}
	@Override
	public long searchKeywordAndOrCount(String[] kwArr, String lang, String cd, Boolean active) {
		long ret = temp.searchAndOrCount(kwArr, lang, cd, active);
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
	public int importItem(String path,String lang,String enc){

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

			List<PsItem> list = new LinkedList<PsItem>();

			List<String[]> line = createCSVReader(path,1,enc).readAll();
			for(String[] col:line){

				if(StringUtils.isNotEmpty(col[3])){
					c++;
					num++;

					PsItem i = null;

					//query は sidの#区切りでid=xxx&id=xxxを生成
					String sid = col[8];
					String query = "";
					if (StringUtils.isNotEmpty(sid)) {
					    String[] sids = sid.split("#");
				    	// PRODに登録されて居なければ除外
				    	Optional<Series> oS = seriesTemp.findByModelNumber(sids[0], ModelState.PROD, true);
				    	if (oS.isPresent() == false) {
				    		continue;
				    	}
				    	i = new PsItem();
					    //sid(webカタログ付け合せ用id)は先頭のIDのみ取得
					    i.setSid(sids[0]);

					    for(String _s:sids){
				    		query+="id="+_s+"&";
					    }
					    if(query.matches("^(.*)&$")){
						    query = query.substring(0, query.length()-1);
						}
						i.setQuery(query);
					} else {
						continue;
					}

					i.setLang(lang);
					i.setNum(String.valueOf(num));
					i.setFlg(col[0]);
					i.setKan(col[1]);
					i.setPage(col[2]);
					i.setC1(col[3]);
					i.setC2(col[4]);
					i.setC1c2(col[3]+"/"+col[4]);
					i.setSeries(col[5]);
					i.setItem(col[6]);
					i.setActive(true);

					String item_regexp = "";
					if(col[6]!=null){
						item_regexp = col[6].replaceAll("□", ".*");
					}
					i.setRegex(item_regexp);

					i.setName(col[7]);

					list.add(i);

				}
			}
			repo.saveAll(list);


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
