package com.smc.discontinued.model;

import org.springframework.data.mongodb.core.mapping.Document;

import com.smc.webcatalog.model.BaseModel;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "discontinued_template")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class DiscontinuedTemplate extends BaseModel {

	public DiscontinuedTemplate() {

	}

    // HeatCoreで作成しpage.jspでインポート。 $$content$$ $$title$$ $$catpan$$ を置き換え
	private String heartCoreID; // import元

	private String template;
	private String sidebar;
	private String catpan;
	private String formbox;
	private String h1box;
	private String content;

	//説明など
	String memo;

}
