package com.smc.webcatalog.util;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public final class LibOkHttpClient {

	public static String getHttpsHtml(String url, String basicAuthID, String basicAuthPW) {
		return getHtml(url, 443, "https", basicAuthID, basicAuthPW);
	}
	public static String getHttpsHtml(String url) {
		return getHtml(url, 443, "https", null, null);
	}
	public static String getHtml(String url) {
		return getHtml(url, 80, "http", null, null);
	}

	public static String getHtml(String url, String basicAuthID, String basicAuthPW) {
		return getHtml(url, 80, "http", basicAuthID, basicAuthPW);
	}

	public static String getHtml(String url, int port, String protcol, String basicAuthID, String basicAuthPW)
	{

		String ret = "";

		if (url.isEmpty()) return null;

		OkHttpClient client = new OkHttpClient.Builder()
	            .connectTimeout(10, TimeUnit.SECONDS) // Connection timeout
	            .readTimeout(30, TimeUnit.SECONDS)    // Read timeout
	            .writeTimeout(30, TimeUnit.SECONDS)   // Write timeout
	            .build();
        GetMethod get = null;

        try{
        	URL u = new URL(url);

        	String host = u.getHost();
        	Request request = null;
	        //auth
	        boolean isAuth = false;
	        if (basicAuthID != null && basicAuthID.isEmpty() == false && basicAuthPW != null && basicAuthPW.isEmpty() == false) {
	        	request = new Request.Builder()
	                    .url(url)
	                    .header("Authorization", basicAuth(basicAuthID, basicAuthPW))
	                    .get()
	                    .build();
	        	isAuth = true;
	        } else {
	        	request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
	        }

	        Response response = client.newCall(request).execute();
	        if (!response.isSuccessful()) {
	        	log.error("getHtml() status="+response.code()+" url="+url+" response="+response);
                return ret;
            } else {
            	if(response.code() == 200 && response.body()!=null)
    	        {
    	        	ret = response.body().string();
    	        } else {
    	        	log.error("getHtml() status="+response.code()+" url="+url+" response="+response.body().string());
    	        }
            }

	    }catch(Exception ex){
	    	log.debug(ex.toString());
	    }finally{

	        if (get != null) get.releaseConnection();
	    }

	    return ret;
	}

	// 正常なら ret = "";
	public static String postJson(String url, int port, String protcol, String basicAuthID, String basicAuthPW, String jsonString)
	{
		String ret = "";

		if (url == null || url.isEmpty()) return null;

        HttpClient client = new HttpClient();
        PostMethod post = null;
        try{
        	URL u = new URL(url);

        	String host = u.getHost();
        	if (port > 0) {
        		client.getHostConfiguration().setHost(host, port, protcol);
        	} else {
        		client.getHostConfiguration().setHost(host);
        	}

	      //auth
	        boolean isAuth = false;
	        if (basicAuthID != null && basicAuthID.isEmpty() == false && basicAuthPW != null && basicAuthPW.isEmpty() == false) {
	        	client.getState().setCredentials(
	                new AuthScope(host, port, null),
	                new UsernamePasswordCredentials(basicAuthID, basicAuthPW));
	        	isAuth = true;
	        }

	        //timeout
	        client.getParams().setParameter("http.socket.timeout", new Integer(105000));
	        StringRequestEntity requestEntity = new StringRequestEntity(
	        		jsonString,
	        	    "application/json",
	        	    "UTF-8");

	        post = new PostMethod( url );
            if (isAuth) post.setDoAuthentication(true);
            post.addRequestHeader("Content-Type", "application/json");

            post.setRequestEntity(requestEntity);
	        int status = client.executeMethod(post);
	        List<String> response = IOUtils.readLines(post.getResponseBodyAsStream(), "UTF-8");
	        if(status!=200&& response!=null)
	        {
	        	for(String r : response) ret += r + "\r\n";
	        }

        }catch(Exception ex){
	    	log.debug(ex.toString());
	    }finally{

	        if (post != null) post.releaseConnection();
	    }
		return ret;
	}
	private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder()
                               .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
