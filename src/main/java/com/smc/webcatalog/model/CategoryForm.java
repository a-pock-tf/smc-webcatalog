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
public class CategoryForm {

	private String id;
	private ModelState state;
	private CategoryType type;
	private String lang;

	private String htmlTop;
	private String htmlMiddle;
	private String htmlBottom;
	private String htmlUrl;

	private String langRefId;
	private String stateRefId;

	private boolean narrowdown;
	private boolean compare;
	private boolean active;

	private String oldId;
	@NotEmpty
	private String parentId;
	@NotEmpty
	private String slug;
	
	// orderはソートで編集

	@NotEmpty
	@Size(min = 1, max = 100)
	private String name;

}
