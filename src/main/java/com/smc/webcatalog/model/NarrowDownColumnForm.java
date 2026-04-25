package com.smc.webcatalog.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 絞り込み検索、項目用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class NarrowDownColumnForm {

	String categoryId;
	String json;
	boolean active;
}
