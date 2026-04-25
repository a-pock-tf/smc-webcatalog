package com.smc.webcatalog.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.smc.exception.DataAccessException;
import com.smc.util.ImpLog;
import com.smc.webcatalog.model.ImpPsItem;
import com.smc.webcatalog.model.ImpPsItemSearchCondition;


public class ImpPsItemDao extends ImpIBatisBaseDao {

	public ImpPsItemDao(ImpDaoStatus status){
		super.initDao(status);
	}



	public void insertNoTrans(ImpPsItem o){

		ImpLog.logStart(this,"insertNoTrans");

		try{
			//startTrans();

			mapclient.insert("insert_PsItem",o);

			//commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			//endTrans();
		}

	}

	public void deleteAll(String lang){

		ImpLog.logStart(this,"deleteAll");

		try{
			startTrans();

			mapclient.delete("deleteAll_PsItem",lang);

			commitTrans();
		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{
			endTrans();
		}

	}


	public List<ImpPsItem> search(List<String> keywords,String c1c2,String series,String lang,String name_series,String mode){

		ImpLog.logStart(this,"search");

		ImpPsItemSearchCondition cond = new ImpPsItemSearchCondition();
		cond.setKeywords(keywords);
		cond.setLang(lang);
		cond.setC1c2(c1c2);
		cond.setSeries(series);

		if(StringUtils.isEmpty(name_series)){
			name_series="SERIES";
		}
		cond.setName_series(name_series);

		if(!mode.equals("KW")){
			mode = "HEAD";
		}
		cond.setMode(mode);

		List<ImpPsItem> list = new ArrayList<ImpPsItem>();
		try{
			list = (List<ImpPsItem>)mapclient.queryForList("search_PsItem",cond);


		}catch(Exception ex){
			ImpLog.logEx(ex);
			throw new DataAccessException();
		}finally{

		}
		return list;

	}







}
