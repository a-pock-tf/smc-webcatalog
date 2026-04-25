package com.smc.webcatalog.util;

import java.net.URL;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LibHttpClient {

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

        HttpClient client = new HttpClient();
        GetMethod get = null;

        try{
        	URL u = new URL(url);

        	String host = u.getHost();
	        client.getHostConfiguration().setHost(host, port, protcol);

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

	        get = new GetMethod( url );
            if (isAuth) get.setDoAuthentication(true);

	        int status = client.executeMethod(get);

	        List<String> response = IOUtils.readLines(get.getResponseBodyAsStream(), "UTF-8");
// 下記エラーのため変更
// Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended.
//	        body = get.getResponseBodyAsString();

	        if(status == 200 && response!=null)
	        {
	        	for(String r : response) ret += r + "\r\n";
	        } else {
	        	log.error("getHtml() status="+status+" url="+url+" response"+response);
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

}
