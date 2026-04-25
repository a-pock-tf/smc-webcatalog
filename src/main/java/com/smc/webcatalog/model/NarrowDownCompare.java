package com.smc.webcatalog.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "narrow_down_compare")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class NarrowDownCompare extends BaseModel {

	public NarrowDownCompare() {
		super.setActive(true);
	}
	private String categoryId; // TEST or PROD categoryのmongoDBのobjectID
	
	private String title; // 項目名 「チューブ内径、ロッドタイプ」など

	// active はBaseModel
	
	// TESTからPROD用にパラメータをコピー
	// id seriesId stateRefId langRefIdはここではコピーしない
	public void setUpdateParam(NarrowDownCompare s) {
		title = s.getTitle();
		super.setActive(s.isActive()); // 表示、非表示
		super.setLang(s.getLang());
		super.setOrder(s.getOrder());
	}
	
}
