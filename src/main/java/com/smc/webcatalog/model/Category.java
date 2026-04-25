 package com.smc.webcatalog.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.lang.NonNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "category")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class Category extends BaseModel {

	//初期化は明示的に
	public Category() {
		setParentId("");
		setOldId("");
		setChildren(new ArrayList<Category>());
		setSeriesList(new ArrayList<Series>());
		setActive(true);
		setType(CategoryType.CATALOG);
		setDisplayType(CategoryDisplayType.GENERAL);
		isShortcut = false;
		isCompare = false;
	}

	// カテゴリ名
	@NonNull
	private String name;

	private String oldId; // 旧システムのID 旧呼び出しに対応 https://www.smcworld.com/products/ja/s.do?ca_id=102#rp

	// 親のID
	@TextIndexed
	private String parentId;

	// タグスラッグ(同一階層内ユニーク。英数字ハイフン、_のみ)
	// 一意の文字列ID
	// - デフォルトで最新の連番を生成
	// - 過去の連番もここに入れる
	@Indexed
	private String slug;

	@Indexed
	private CategoryType type;
	@Indexed
	private CategoryDisplayType displayType;

	// trueの場合、parentIdに元のIDが入る。
	private boolean isShortcut;
	
	// 絞り込み検索を表示
	private boolean isNarrowdown;
	// 仕様比較を表示
	private boolean isCompare;

	// サブカテゴリ
	@Transient
	private List<Category> children;

	// シリーズ
	@Transient
	private List<Series> seriesList;

	public boolean isRoot() {
		boolean ret = false;
		if (parentId == null || parentId.isEmpty()) {
			ret = true;
		}
		return ret;
	}

	public void SetUpdateParam(Category c, User u) {
		name = c.getName();
		slug = c.getSlug();
		type = c.getType();
		displayType = c.displayType;
		setShortcut(c.isShortcut());
		setNarrowdown(c.isNarrowdown());
		setCompare(c.isCompare());
		setActive(c.isActive());
		setOrder(c.getOrder());

		setUser(u);
		setMtime(new Date());
	}

	public Category Copy() {
		Category c2 = new Category();
		c2.setActive(true);
		c2.setNarrowdown(isNarrowdown);
		c2.setCompare(isCompare);
		c2.setCtime(getCtime());
		c2.setDisplayType(getDisplayType());
		c2.setId(null);
		c2.setLang(getLang());
		c2.setMtime(getMtime());
		c2.setName(getName());
		c2.setOldId("");
		c2.setOrder(getOrder());
		c2.setParentId(getParentId());
		c2.setSlug(getSlug());
		c2.setState(getState());
		c2.setType(getType());
		c2.setUser(getUser());
		return c2;
	}

}
