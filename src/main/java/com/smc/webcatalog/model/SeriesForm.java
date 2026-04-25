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
public class SeriesForm {

	private String id;
	private ModelState state;
	private String lang;
	private boolean active;

	private String number;
	private String modelNumber;
	private String searchword;
	private String breadcrumb;
	private String image;

	private String detail;

	private String advantage;
	private String other;
	private String spec;

	private String langRefId;
	private String stateRefId;

	// CategorySeries
	private String[] categoryList;
	private String addCategory;

	private String[] linkList;

	private boolean cad3d;
	private boolean custom; // 簡易特注
	private boolean orderMade; // オーダーメイド
	private boolean pre; // 準備中

	private String notice;
	private String imageTop;
	private String imageBottom;
	private String order; // 検索の表示順。（カテゴリ選択時はCategorySeriesの順番）

	private String oldId;

	@NotEmpty
	@Size(min = 1, max = 150)
	private String name;
}
