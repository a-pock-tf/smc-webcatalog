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
public class DiscontinuedSeriesForm {

	private String id;
	@NotEmpty
	private String categoryId;
	@NotEmpty
	private String lang;

	@NotEmpty
	private String seriesName;
	@NotEmpty
	private String seriesId;
	@NotEmpty
	@Size(min = 1, max = 150)
	private String name;
	@NotEmpty
	private String series;
	@NotEmpty
	private String newSeries;

	private String image;
	private String newImage;
	private String catalogLink;
	private String newCatalogLink;
	private String manualLink;
	private String compatibility;

	private String other;
	private String caution;
	private String comparison;

	private String date;
	private String detail;

	private String langRefId;
	private String stateRefId;
	private int order;
	private String versionLog;

	private DiscontinuedModelState state;
	private boolean active;

}
