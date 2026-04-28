/*
 * Author Tsutomu Miyashita( WhiteBaseSystems,Limited )
 * Created on 2002/12
 */

package com.smc.webcatalog.util;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.arnx.jsonic.JSON;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@Slf4j
public class JpServiceUtil {

//	public static String S3SAPI_SERVER_URL = "http://153.120.135.17"; // 以前
//	public static String S3SAPI_SERVER_URL = "http://www.smc3s.com"; // TEST
	public static String S3SAPI_SERVER_URL = "https://3sapi.smcworld.com"; // サーバー増強


	// 品番取得
	public JpServiceResult get(ServletContext context, String id, String lang) {

		log.debug( "----------start-----------");

		if (StringUtils.isEmpty(lang))
			lang = "ja-JP";

		Client client = S3SClient.getInstance();

		JpServiceResult jpresult = null;
		try {

			AccessTokenResult access_token = getAccessToken(context, client, Calendar.getInstance());

			String lang3S = "ja-JP";
			if (lang.indexOf("zh") > -1) {
				lang3S = "zh-CHS";
			} else if (lang.indexOf("en") > -1) {
				lang3S = "en-US";
			}

			//get TypeId(s) by Japanese SeriesID
			MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
			headers.putSingle("Authorization", "Bearer " + access_token.getAccess_token());

			WebTarget target = client.target(S3SAPI_SERVER_URL)
					.path("/3SApi/check/v1")
					// .property("jersey.config.client.connectTimeout", 10000)
					.property("jersey.config.client.connectTimeout", 3000) // 接続タイムアウト3秒
					.property("jersey.config.client.readTimeout", 7000) // 読み込みタイムアウト
					.queryParam("modelNo", URLEncoder.encode(id, "UTF-8"))
					.queryParam("fields", "-1")
					.queryParam("language", lang3S);

			String jpServiceResult = target.request().headers(headers).get(String.class);

			//Log.log("result=" + jpServiceResult);

			jpresult = JSON.decode(jpServiceResult, JpServiceResult.class);

			log.debug("errno:" + jpresult.getErrno() + "/message:" + jpresult.getMessage());
			for (S3SSeriesInfo si : jpresult.getData()) {
				log.debug("typeId:" + si.getTypeId() + "/name:" + si.getName());
			}

		} catch (BadRequestException e) {
			log.error("BadRequestException response=" + e.getResponse().toString()+e.getMessage());
		} catch (Throwable ex) {
			log.error("Throwable. response=" + ex.getMessage()+ex.getLocalizedMessage());
		}

		return jpresult;

	}//end execute

	// 品番一覧
	public JpServiceResult list(ServletContext context, String id, String lang) {

		log.debug( "----------start-----------");

		if (StringUtils.isEmpty(lang))
			lang = "ja-JP";

		Client client = S3SClient.getInstance();

		JpServiceResult jpresult = null;
		try {

			AccessTokenResult access_token = getAccessToken(context, client, Calendar.getInstance());

			String lang3S = "ja-JP";
			if (lang.indexOf("zh") > -1) {
				lang3S = "zh-CHS";
			} else if (lang.indexOf("en") > -1) {
				lang3S = "en-US";
			}

			//get TypeId(s) by Japanese SeriesID
			MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
			headers.putSingle("Authorization", "Bearer " + access_token.getAccess_token());

			WebTarget target = client.target(S3SAPI_SERVER_URL)
					.path("/3SApi/jpService/typeIdMappings")
					//.property("jersey.config.client.connectTimeout", 10000) // 接続タイムアウト10秒
					.property("jersey.config.client.connectTimeout", 3000) // 接続タイムアウト3秒
					.property("jersey.config.client.readTimeout", 8000) // 読み込みタイムアウト10秒
					.queryParam("typeIdJapan", URLEncoder.encode(id, "UTF-8"))
					.queryParam("language", lang3S);

			String jpServiceResult = target.request().headers(headers).get(String.class);

			//Log.log("result=" + jpServiceResult);

			jpresult = JSON.decode(jpServiceResult, JpServiceResult.class);

			log.debug("errno:" + jpresult.getErrno() + "/message:" + jpresult.getMessage());
			for (S3SSeriesInfo si : jpresult.getData()) {
				log.debug("typeId:" + si.getTypeId() + "/name:" + si.getName());
			}

		} catch (BadRequestException e) {
			log.debug("response=" + e.getResponse().toString());
			throw e;
		} catch (Throwable ex) {
			log.error("Throwable. response=" + ex.getMessage()+ex.getLocalizedMessage());
		}

		return jpresult;

	}//end
	
	// 品番検索 3Sコンフィグレータ #101 部分一致検索の件 2023/10/20
	// page,limit,searchTypeはnull可。その場合、3Sapiのデフォルト、1,10,inが適用。
	public S3SPartialMatchResult search(ServletContext context, List<String> kwList, String lang, Integer page, Integer limit, String searchType) {
		log.debug( "----------start-----------");

		if (StringUtils.isEmpty(lang))
			lang = "ja-JP";
		
		Client client = S3SClient.getInstance();

		S3SPartialMatchResult result = null;
		try {

			AccessTokenResult access_token = getAccessToken(context, client, Calendar.getInstance());

			String lang3S = "ja-JP";
			if (lang.indexOf("zh") > -1) {
				lang3S = "zh-CHS";
			} else if (lang.indexOf("en") > -1) {
				lang3S = "en-US";
			}
			boolean isSharp = false;
			String strSharp = "?kw=";
			for(String k : kwList) {
				if (k.indexOf('#') > -1) {
					if (isSharp == false) {
						isSharp = true;
						strSharp+=k;
					} else {
						strSharp+="&kw="+k;
					}
				}
			}
			//get TypeId(s) by Japanese SeriesID
			MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>();
			headers.putSingle("Authorization", "Bearer " + access_token.getAccess_token());

			WebTarget target = null;
			if (isSharp) {
				String param = "";
				for(String k : kwList) {
					if (k.indexOf('#') == -1) param += "&kw="+ k;
				}
				if (page != null && page > 1) {
					param += "&page="+ page;
				}
				if (limit != null && limit > 0) {
					param += "&limit="+ limit;
				}
				if (searchType != null && searchType != "in") {
					param += "&searchType="+ "fore";
				}
				String url = S3SAPI_SERVER_URL+"/3SApi/search/v1"+strSharp+"&language="+lang3S+param;
				
				Request request = new Request.Builder().url(url)
						.addHeader("Authorization", "Bearer " + access_token.getAccess_token())
						.build();
				OkHttpClient okclient = new OkHttpClient.Builder().build();
				Response okresponse = okclient.newCall(request).execute();
				String response = okresponse.body().string();
				
				result = JSON.decode(response, S3SPartialMatchResult.class);
			} else {
				target = client.target(S3SAPI_SERVER_URL)
						.path("/3SApi/search/v1")
						//.property("jersey.config.client.connectTimeout", 10000) // 接続タイムアウト10秒
						.property("jersey.config.client.connectTimeout", 3000) // 接続タイムアウト秒
						.property("jersey.config.client.readTimeout", 8000); // 読み込みタイムアウト秒
				for(String k : kwList) {
					target = target.queryParam("kw", k);
				}
				if (lang3S != null) {
					target = target.queryParam("language", lang3S);
				}
				if (page != null && page > 1) {
					target = target.queryParam("page", page);
				}
				if (limit != null && limit > 0) {
					target = target.queryParam("limit", limit);
				}
				if (searchType != null && searchType != "in") {
					target = target.queryParam("searchType", "fore");
				}
				String jpServiceResult = target.request().headers(headers).get(String.class);
				result = JSON.decode(jpServiceResult, S3SPartialMatchResult.class);
			}

			
			if (result != null && result.getSearchData() != null) {
				for (S3SPartialMatchResultData si : result.getSearchData()) {
					log.debug("typeId:" + si.getTypeID() + "/name:" + si.getDescription());
				}
			}

		} catch (BadRequestException e) {
			log.debug("response=" + e.getResponse().toString());
			throw e;
		} catch (Throwable ex) {
			String tmp = ex.getMessage()+ex.getLocalizedMessage();
			log.error("Throwable. response=" + tmp);
			// 401 Unauthorizedならtokenを再度取得
			if (tmp != null && tmp.indexOf("401 Unauthorized") > -1) {
				AccessTokenResult access_token = (AccessTokenResult) context.getAttribute("3S_ACCESS_TOKEN");
				access_token = null;
				context.setAttribute("3S_ACCESS_TOKEN", access_token);
			}
		}
		return result;
	}


	public static AccessTokenResult getAccessToken(ServletContext context, Client client, Calendar now) {

		//examine token
		//(read from context)
		AccessTokenResult access_token = (AccessTokenResult) context.getAttribute("3S_ACCESS_TOKEN");
		try {

			//get token if token expired.
			if (access_token == null || access_token.isExpired(now)) {

				log.debug("--> access_token expired. renew.");

				Entity<Form> entity = Entity.entity(new Form()
						.param("grant_type", "client_credentials")
						.param("client_id", "3SWebApp")
						.param("client_secret", "6wCQMAhzNBjfgMUS"),
						MediaType.APPLICATION_FORM_URLENCODED_TYPE);

				String result = client.target(S3SAPI_SERVER_URL)
						.path("/3SApi/token")
						.property("jersey.config.client.connectTimeout", 3000) // 接続タイムアウト3秒
						.property("jersey.config.client.readTimeout", 5000) // 読み込みタイムアウト
						.request()
						.post(entity, String.class);

				log.debug("result=" + result);
				access_token = JSON.decode(result, AccessTokenResult.class);
				context.setAttribute("3S_ACCESS_TOKEN", access_token);
			} else {
				log.debug("--> access_token found."+access_token.getAccess_token());
			}

		} catch (BadRequestException e) {
			log.debug("response=" + e.getResponse().toString());
			throw e;
		} catch (ResponseProcessingException e) {
			log.debug("response=" + e.getResponse().toString());
			throw e;
		} catch (ProcessingException e) {
			log.debug("response=" + e.getMessage().toString());
			throw e;
		} catch (WebApplicationException e) {
			log.debug("response=" + e.getMessage().toString());
			throw e;
		} catch (Throwable ex) {
			log.error("getAccessToken() Throwable. message="+ex.getMessage());
		}

		return access_token;
	}

}//end Class
