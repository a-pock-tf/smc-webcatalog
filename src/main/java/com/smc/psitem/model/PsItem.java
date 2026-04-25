package com.smc.psitem.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "psitem")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class PsItem extends PsitemBaseModel {

	String name; // 5通电磁阀
	String series; // VZS2000/3000
	String c1; // 方向控制元件
	String c2; // 先导式4、5通电磁阀
	String c1c2; // 検索用。/で繋げたもの。方向控制元件/方向控制元件
	String item; // VZS3000
	String regex; // CA2□M- -> CA2.*M-
	String sid; // VZS-ZH
	String query; // id=VZS-ZH

	// 以下、使わない
	String num;
	String kan;
	String flg;
	String page;

}
