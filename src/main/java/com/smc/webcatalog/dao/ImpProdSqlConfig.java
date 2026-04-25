package com.smc.webcatalog.dao;

import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import com.smc.util.ImpLog;

public class ImpProdSqlConfig {

	private static SqlMapClient sqlMap;


	static {
			try {

				String resource = "src/main/resources/sql/SqlMapConfig.xml";

				if (sqlMap == null) {
					//Reader reader = Resources.getResourceAsReader(resource);
					InputStream is = new ClassPathResource("sql/SqlMapConfig.xml").getInputStream();
					//sqlMap = SqlMapClientBuilder.buildSqlMapClient(reader);
					sqlMap = SqlMapClientBuilder.buildSqlMapClient(is);
				}



			} catch (Exception e) {
					ImpLog.logEx(e);
			}

	}



	public static SqlMapClient getSqlMapInstance() {
		return sqlMap;
	}


}
