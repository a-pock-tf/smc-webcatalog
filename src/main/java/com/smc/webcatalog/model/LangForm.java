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
public class LangForm {

	private String id;
	private String name;
	private String memo;
	private boolean version;
	private String baseLang;
	private boolean active;
}
