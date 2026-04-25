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
public class TemplateForm {

	private String id;
	private String lang;
	String heartCoreId;
	String header;
	String footer;
	private String contents;
	private String memo;
	private String stateRefId;
	private ModelState state;
	private boolean active;
}
