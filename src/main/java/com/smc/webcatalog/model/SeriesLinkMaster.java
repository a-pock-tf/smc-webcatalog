package com.smc.webcatalog.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "series_link_master")
@Getter
@Setter
public class SeriesLinkMaster extends BaseModel {

	public final static String APPLICATION_CONTEXT_PREFIX = "SERIES_LINK_MASTER_LIST";

	public final static String APPLICATION_CONTEXT_ALL_PREFIX = "SERIES_LINK_MASTER_LIST_ALL";

	// lang langRefIdは使用するがState StateRefIDは使用しない。
	// SeriesLinkでは使用する。
	public SeriesLinkMaster () {
		setName("name");
		setLang("ja-jp");
		setActive(true);
	}
	public SeriesLinkMaster(String name) {
		setName(name);
		setActive(true);
	}

	// 覚えやすい名前 (特定開発品情報 方向制御機器用）
	String title;

	// 特定開発品情報 など
	String name;

	// デフォルトのURL(入力があれば選択時にデフォルトとして設定）
	String defaultUrl;

	// 表示場所
	SeriesLinkType type;

	// SeriesLinkType.ICONのCSS Class名
	String iconClass;

	// target="_blank"を表示する
	boolean blank;


}
