package com.smc.webcatalog.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "series_faq")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class SeriesFaq extends BaseModel {

	private String seriesId; // TEST seriesのmongoDBのobjectID
	
	private String name;

	private String modelNumber; // SeriesID LEYG_D-ZH (末尾で言語 -E -ZH -ZHTW 日本は無し)

	private String faq; // 1,2,3 faqのIDをカンマ区切り
	
	// TESTからPROD用にパラメータをコピー
	// id seriesId stateRefId langRefIdはここではコピーしない
	public void setUpdateParam(SeriesFaq s) {
		name = s.getName();
		modelNumber = s.getModelNumber();
		faq = s.getFaq();
		super.setLang(s.getLang());
		super.setOrder(s.getOrder());
	}
}
