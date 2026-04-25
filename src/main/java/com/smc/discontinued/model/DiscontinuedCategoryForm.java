package com.smc.discontinued.model;

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
public class DiscontinuedCategoryForm {

	private String id;
	private DiscontinuedModelState state;
	private String lang;

	private String htmlTop;
	private String htmlMiddle;
	private String htmlBottom;
	private String htmlUrl;

	private String langRefId;
	private String stateRefId;

	private boolean active;

	private String oldId;
	@NotEmpty
	private String slug;

	@NotEmpty
	@Size(min = 1, max = 100)
	private String name;

}
