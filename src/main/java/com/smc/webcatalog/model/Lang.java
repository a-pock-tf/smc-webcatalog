package com.smc.webcatalog.model;

import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "lang")
@Getter
@Setter
public class Lang extends BaseModel {

	// activeのみ。
	public final static String APPLICATION_CONTEXT_PREFIX = "LANG_LIST";

	// すべて
	public final static String APPLICATION_CONTEXT_ALL_PREFIX = "LANG_LIST_ALL";

	// active==true version==false
	public final static String APPLICATION_CONTEXT_VIEW_PREFIX = "LANG_VIEW_LIST";

	public Lang () {
		setName("ja-jp");
		setActive(true);
	}
	public Lang(String name) {
		setName(name);
		setLang(name);
		setActive(true);
		setVersion(false);
		setBaseLang(null);
	}

	public Lang(String name, boolean version, String base) {
		setName(name);
		setLang(name);
		setActive(true);
		setVersion(version);
		setBaseLang(base);
	}

	//ja-jpなど
	@UniqueElements
	String name;

	//説明など
	String memo;

	boolean version; // baseLangを表示。URLを変更

	String baseLang;

}
