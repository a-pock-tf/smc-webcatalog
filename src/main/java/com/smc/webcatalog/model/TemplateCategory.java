package com.smc.webcatalog.model;

import java.util.Date;

import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "templateCategory")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class TemplateCategory extends BaseModel {

	public TemplateCategory() {
		setId(null);
	}

	// カテゴリID
	@TextIndexed
	private String categoryId;

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
	
	public boolean is2026() {
		return (template != null && template.indexOf("<main ") > -1);
	}
	
	public String getProductsSupport() {
		String ret = "";
		String[] arr = content.split("<!-- products_support -->");
		if (arr.length >= 2) {
			ret = arr[1];
		}
		return ret;
	}

	public void SetUpdateParam(TemplateCategory tc, User u) {
		heartCoreID = tc.getHeartCoreID();
		template = tc.getTemplate();
		sidebar = tc.getSidebar();
		catpan = tc.getCatpan();
		formbox = tc.getFormbox();
		h1box = tc.getH1box();
		content = tc.getContent();
		setActive(tc.isActive());

		setUser(u);
		setMtime(new Date());
	}
}
