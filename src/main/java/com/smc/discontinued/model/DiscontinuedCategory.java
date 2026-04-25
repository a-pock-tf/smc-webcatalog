 package com.smc.discontinued.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.NonNull;
import com.smc.webcatalog.model.User;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "discontinued_category")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class DiscontinuedCategory extends DiscontinuedBaseModel {

	//初期化は明示的に
	public DiscontinuedCategory() {
		setOldId("");
		setLang("ja-jp");
		setSeriesList(new ArrayList<DiscontinuedSeries>());
		setCtime(new Date());
		setMtime(new Date());
		setActive(true);
	}

	// カテゴリ名
	@NonNull
	private String name;

	private String oldId; // A02 旧システムのID 旧呼び出しに対応 https://www.smcworld.com/discon/ja/list.do#A02

	// タグスラッグ(同一階層内ユニーク。英数字ハイフン、_のみ)
	// 一意の文字列ID
	// - デフォルトで最新の連番を生成
	// - 過去の連番もここに入れる
	@Indexed
	@NonNull
	private String slug;

	// 生産終了品
	@Transient
	private List<DiscontinuedSeries> seriesList;


	public void SetUpdateParam(DiscontinuedCategory c, User u) {
		name = c.getName();
		slug = c.getSlug();
		setActive(c.isActive());
		setOrder(c.getOrder());

		setUser(u);
		setMtime(new Date());
	}

	public DiscontinuedCategory Copy() {
		DiscontinuedCategory c2 = new DiscontinuedCategory();
		c2.setActive(true);
		c2.setCtime(getCtime());
		c2.setId(null);
		c2.setLang(getLang());
		c2.setMtime(getMtime());
		c2.setName(getName());
		c2.setOldId(getOldId());
		c2.setOrder(getOrder());
		c2.setSlug(getSlug());
		c2.setState(getState());
		c2.setUser(getUser());
		return c2;
	}

}
