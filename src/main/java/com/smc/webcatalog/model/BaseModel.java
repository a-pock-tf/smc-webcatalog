package com.smc.webcatalog.model;

import java.util.Date;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.DBRef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.lang.NonNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BaseModel {

	public BaseModel() {
		setId(null);
		setCtime(new Date());
		setMtime(new Date());
		setState(ModelState.TEST);
		setLang("ja-jp");
	}

	@Id
	private String id;

	@NonNull
	private ModelState state;
	/**
	 *  Stateの結びつきを示すID
	 *  PROD,ARCHIVEへTESTのIDを格納
	 */
	private String stateRefId;

	// LangクラスではなくStringで(ja-jp)など
	private String lang;

	/**
	 * 言語の結びつきを示すID
	 * ja-jp以外の言語へ ja-jpのTESTのIDを格納
	 */
	private String langRefId;

	// 並び順 Lang.java Category.java 
	// Series.java(検索結果) カテゴリクリック時はCategorySeriesの順番。 
	private int order;

	// 非公開フラグ
	private boolean active;

	// Archive時のメモ
	private String versionLog;

	//編集者
	@JsonIgnore
	@DBRef
	private User user;

	// 作成日時
	@ToString.Exclude
	private Date ctime;

	// 更新日時(state=ARCHIVEのversionとしても利用)
	@ToString.Exclude
	private Date mtime;

}
