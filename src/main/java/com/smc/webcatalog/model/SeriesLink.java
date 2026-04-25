package com.smc.webcatalog.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "series_link")
@Getter
@Setter
public class SeriesLink extends BaseModel {

	// SeriesとSeriesLinkMasterの結び付き

	public SeriesLink () {
		setLang("ja-jp");
		setActive(true);
	}
	/**
	 * インポートで使用。SeriesのIDは保存後なので、後で設定
	 * @param masterId
	 * @param lang
	 * @param user
	 * @param url
	 */
	public SeriesLink(SeriesLinkMaster master, String lang, User user, String url, ModelState state) {
		setSeriesId(null);

		setLinkMaster(master);
		setLang(lang);
		setUrl(url);
		setUser(user);
		setState(state);
		setActive(true);
	}

	// シリーズID
	private String seriesId;

	private String modelNumber; // modelNumber

	// LinkMasterID
	@DBRef
	private SeriesLinkMaster linkMaster;

	String url;

	boolean targetBlank;

}
