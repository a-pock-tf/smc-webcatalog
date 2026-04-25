package com.smc.webcatalog.config;

import java.text.SimpleDateFormat;

import com.smc.webcatalog.dao.ImpDaoStatus;


public class ImpConstants {

	public static final ImpDaoStatus DAO_STATUS = ImpDaoStatus.PROD;
	//public static final DaoStatus DAO_STATUS = DaoStatus.DEV;
	public static final int UPFILE_MAX=1024000;
	public static final int COOKIE_MAXAGE=2952000;//30days
	public static final String COL_PREFIX = "c";//itemdb column prefix
	public static final int COL_MAX=50;
	public static final String UPDIR="upfiles";

	public static final SimpleDateFormat sdf_yyyyMMdd = new SimpleDateFormat("yyyy/MM/dd");

	public static final String APPDIR="/home/htdocs/smccamp/";
	public static final String CATALOGDIR="/home/htdocs/smccatalog/";

    public static final String APP_PROP_FILE = APPDIR+"WEB-INF/app.xml";

	public static final int MYLIST_MAX = 9999;
	public static final int MYLIST_FOLDER_MAX = 9999;

	public static final String PROD_SERVER_ADDR="192.168.0.2";
	public static final int SEMINAR_YEAR = 2020;



}
