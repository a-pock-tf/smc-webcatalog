package com.smc.cad3d.service;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.smc.cad3d.model.Cad3d;
import com.smc.webcatalog.model.MyErrors;

@Service
@Scope("session")
public interface Cad3dService {

	List<Cad3d> searchKeyword(List<String> kw, String c1c2, String series, String lang);

	List<Cad3d> searchIndex(String idx, String c1c2, String series, String lang);

	List<Cad3d> searchUrl(String ppath, String lang);

	List<Cad3d> search(String sid, String series, String lang, String cat );

	/**
	 * CSVのフォーマットチェック
	 */
	MyErrors checkFormat(String fullpath, String enc, int colSize);

	/**
	 *
	 * @param path
	 * @param lang
	 * @param enc
	 * @return
	 */
	int importItem(String path,String lang,String enc);
}
