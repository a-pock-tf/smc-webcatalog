package com.smc.discontinued.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 言語用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class DiscontinuedTemplateForm {

	private String id;

	private String lang;

	private String heartCoreId;
	private String template;
	private String sidebar;
	private String catpan;
	private String formbox;
	private String h1box;
	private String content;

	private String memo;

	private boolean active;
}
