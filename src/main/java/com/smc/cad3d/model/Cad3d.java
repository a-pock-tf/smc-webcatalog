package com.smc.cad3d.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.smc.webcatalog.model.Series;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "cad3d")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class Cad3d extends Cad3dBaseModel {

	String c1; // 方向控制元件
	String c2; // 先导式4、5通电磁阀
	String c3;
	String c4;
	String c5;

	private String series;
	private String cat;
	private String item;
	private String name;
	private String ids;

	private String url1;
	private String url2;//未使用
	private String newurl;//未使用

	private String newmsg;//代替品用メッセージ
	private String newids;//代替品

	private List<Series> new_serieses = new ArrayList<Series>();//代替品(シリーズ)


	// 以下、使わない
	String num;

}
