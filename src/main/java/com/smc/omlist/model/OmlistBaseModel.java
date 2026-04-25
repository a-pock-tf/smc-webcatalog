package com.smc.omlist.model;

import java.util.Date;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.DBRef;

import com.smc.webcatalog.model.User;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OmlistBaseModel {

	public OmlistBaseModel() {
		setId(null);
		setCtime(new Date());
		setMtime(new Date());
		setLang("ja-jp");
	}

	@Id
	private String id;

	/**
	 *  Stateの結びつきを示すID
	 *  PROD,ARCHIVEへTESTのIDを格納
	 */

	// LangクラスではなくStringで(ja-jp)など
	private String lang;


	//並び順Lang.java
	private int order;

	// 非公開フラグ
	private boolean active;

	//編集者
	@DBRef
	private User user;

	// 作成日時
	@ToString.Exclude
	private Date ctime;

	// 更新日時(state=ARCHIVEのversionとしても利用)
	@ToString.Exclude
	private Date mtime;

}
