package com.smc.webcatalog.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 絞り込み検索、項目用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class NarrowDownSeriesForm {
	String id;
	String seriesId;
	String name;
	String modelNumber;
	List<String> params;
	List<String> idList;
}
