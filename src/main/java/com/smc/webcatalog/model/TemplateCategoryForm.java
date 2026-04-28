package com.smc.webcatalog.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 言語用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class TemplateCategoryForm {

	private String id;

	private String categoryId;
	private String lang;

	private String heartCoreId;
	private String template;
	private String sidebar;
	private String catpan;
	private String formbox;
	private String h1box;
	private String content;

	private String memo;
	
	private String stateRefId;
	private ModelState state;

	private boolean active;
}
