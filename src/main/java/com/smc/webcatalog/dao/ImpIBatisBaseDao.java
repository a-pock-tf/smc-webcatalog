package com.smc.webcatalog.dao;

import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import com.smc.util.ImpLog;


public class ImpIBatisBaseDao {

	public ImpDaoStatus status;

	protected SqlMapClient mapclient;

	protected void initDao(ImpDaoStatus status){

		this.status = status;

		switch (status) {

			case PROD:
				mapclient = ImpProdSqlConfig.getSqlMapInstance();

				break;

			case TEST:
//				mapclient = TestSqlConfig.getSqlMapInstance();
				break;

			case PROD_SERVER:

//				mapclient = ProdServerSqlConfig.getSqlMapInstance();

				break;



		default:

			try {

				String resource = "resources/sql/SqlMapConfig.xml";

				if (mapclient == null) {
					InputStream is = new ClassPathResource("sql/SqlMapConfig.xml").getInputStream();
//					Reader reader = Resources.getResourceAsReader(resource);
//					mapclient = SqlMapClientBuilder.buildSqlMapClient(reader);
					mapclient = SqlMapClientBuilder.buildSqlMapClient(is);
				}


			} catch (Exception e) {
				ImpLog.logEx(e);
			}

			break;
		}




	}

	public void commitTrans(){
		try{
			mapclient.commitTransaction();
		}catch(Exception ex){
			ImpLog.logEx(ex);
		}
	}

	public void startTrans(){
		try{
			mapclient.startTransaction();
		}catch(Exception ex){
			ImpLog.logEx(ex);
		}
	}

	public void endTrans(){
		try{
			mapclient.endTransaction();
		}catch(Exception ex){
			ImpLog.logEx(ex);
		}
	}


}
