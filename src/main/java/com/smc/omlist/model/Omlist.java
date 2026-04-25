package com.smc.omlist.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "omlist")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class Omlist extends OmlistBaseModel {

	String category; // カテゴリ
	String div; // 区分
	String kata; // 表示記号
	String spec; // 仕様
	String file; // ファイル名（PDFリンク名）
	String bestnum; // Link（BEST 巻数）JA
	String bestpage; // Link（BEST ページ）
	String ids; // ID(日)


}
