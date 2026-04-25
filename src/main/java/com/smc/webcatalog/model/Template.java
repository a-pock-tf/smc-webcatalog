package com.smc.webcatalog.model;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "template")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class Template extends BaseModel {

	public Template(String lang) {
		setLang(lang);
	}

    // HeatCoreで作成しpage.jspでインポート。
	String heartCoreId;
	String header;
	String footer;

	String contents;

	//説明など
	String memo;
	
	public boolean is2026() {
		return (contents != null && contents.indexOf("<main ") > -1);
	}
	
	// TESTからPROD用にパラメータをコピー
	// id stateRefId langRefIdはここではコピーしない
	public void setUpdateParam(Template t) {
		heartCoreId = t.getHeartCoreId();
		header = t.getHeader();
		footer = t.getFooter();
		contents = t.getContents();
		memo = t.getMemo();
		setMtime(new Date());
		setOrder(t.getOrder());
		setActive(t.isActive());
	}

}
