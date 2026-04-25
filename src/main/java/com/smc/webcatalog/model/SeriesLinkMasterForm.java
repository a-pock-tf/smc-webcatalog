package com.smc.webcatalog.model;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * Category 編集用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class SeriesLinkMasterForm {

	private String id;
	private String title;
	@NotEmpty
	@Size(min = 1, max = 150)
	private String name;
	private String lang;
	private SeriesLinkType type;
	private String defaultUrl;
	private String iconClass;
	private int order;
	private boolean blank;
	private boolean active;

}
