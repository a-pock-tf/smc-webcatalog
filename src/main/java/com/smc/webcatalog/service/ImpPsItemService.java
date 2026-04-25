package com.smc.webcatalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ImpPsItemService {

/*	private static ImpPsItemDao psdao = ImpDaoFactory.createPsItemDao();

	public List<ImpPsItem> searchKeyword(List<String> kw, String c1c2, String series, String lang)
	{

		List<ImpPsItem> list = psdao.search(kw, c1c2, series, getLang(lang), "", "KW");
		return  list;
	}

	public List<ImpPsItem> searchIndex(List<String> kw, String c1c2, String series, String lang)
	{
		List<ImpPsItem> list = psdao.search(kw, c1c2, series, getLang(lang), "", "HEAD");
		return  list;
	}

	// psItemはlangが2桁のため変換
	private String getLang(String lang) {
		String la = "ja";
		if (lang != null && lang.isEmpty() == false) {
			la = lang.substring(0,2);
		}
		return la;
	}*/
}
