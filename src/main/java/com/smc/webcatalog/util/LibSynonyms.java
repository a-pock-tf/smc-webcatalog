package com.smc.webcatalog.util;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LibSynonyms {
	
	static String UrlSynonyms = "https://api.smcworld.com/gsearch/api/v1/synonyms?lang=";
	public List<String> getSynonyms(String[] kwArr, String lang) 
	{
		List<String> ret = null;
		
		if (kwArr != null && kwArr.length > 0) 
		{
			HttpClient client = new HttpClient();
			GetMethod get = null;
			String synonymsLang = "ja_JP";
			String url = UrlSynonyms;
			try{
		        
				if (lang == null || lang.isEmpty() || lang.equals("ja-jp")) {
					
				} else if (lang.indexOf("en-") > -1) {
					synonymsLang = "en_US";
				} else if (lang.equals("zh-tw")) {
					synonymsLang = "zh_TW";
				} else if (lang.indexOf("zh-") > -1) {
					synonymsLang = "zh_CN";
				}
				url += synonymsLang;
				
				//+ "&query="
				url += "&query=";
				String q = "";
				for(String kw : kwArr) {
					q += kw.trim() + " ";
				}
				q = q.substring(0, q.length()-1);
				url+=URLEncoder.encode(q, "UTF-8");

				URL u = new URL(url);
				String host = u.getHost();
		        client.getHostConfiguration().setHost(host, 443, "https");

				//timeout
		        client.getParams().setParameter("http.socket.timeout", new Integer(2000));
		        get = new GetMethod( url );
		        
		        // 実行
		        int status = client.executeMethod(get);
		        String response = IOUtils.toString(get.getResponseBodyAsStream(), "UTF-8");
		        if(status == 200 && response != null)
		        {
		        	ret = new ArrayList<>();
		        	JSONArray json = new JSONArray("["+response+"]");
		        	for (Object obj :  json) {
		        		if (obj instanceof Map) {
			        		Map<String,JSONArray> tmp = (Map<String,JSONArray>)obj;
				        	for(String kw : kwArr) {
				        		if (tmp.get(kw) != null) {
				        			String str = kw + " ";
				        			for(Object o : tmp.get(kw)) {
				        				String s = (String)o;
				        				str += s + " ";
				        			}
				        			str = str.substring(0, str.length()-1);
				        			ret.add(str);
				        		}
				        	}
		        		} else if (obj instanceof JSONObject) {
		        			JSONObject object = (JSONObject)obj;
		        			for(String kw : kwArr) {
				        		if (object.get(kw) != null) {
				        			String str = kw + " ";
				        			JSONArray arr = (JSONArray)object.get(kw);
				        			for(Object o : arr) {
				        				String s = (String)o;
				        				str += s + " ";
				        			}
				        			str = str.substring(0, str.length()-1);
				        			ret.add(str);
				        		}
				        	}
		        		}
		        	}
		        } else {
		        	log.error("ERROR! LibSynonyms.getSynonyms() status="+status+" response="+response);
		        }
			} catch(Exception ex){
		    	log.error(ex.toString());
		    } finally {

		        if (get != null) get.releaseConnection();
		    }
		}
		return ret;
	}

}
