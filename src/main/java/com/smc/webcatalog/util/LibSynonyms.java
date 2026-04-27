package com.smc.webcatalog.util;


import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

		        // 実行
		        String response = LibOkHttpClient.getHtml(url);
		        if(response != null && response.isEmpty() == false)
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
		        	log.error("ERROR! LibSynonyms.getSynonyms() response="+response);
		        }
			} catch(Exception ex){
		    	log.error(ex.toString());
		    } finally {

		    }
		}
		return ret;
	}

}
