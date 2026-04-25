package com.smc.webcatalog.web;

import org.springframework.util.StringUtils;

import com.smc.webcatalog.model.CategoryType;
import com.smc.webcatalog.model.ModelState;
import com.smc.webcatalog.model.ViewState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenStatusHolder {

	public ScreenStatusHolder() {
		view = ViewState.CATEGORY.toString();
		type = CategoryType.CATALOG.toString();
		isProd = false;
		isLayer1Category = true;
		lang = "ja-jp";
		keyword = "";
		temp = "";
		topUrl = "";
		backUrl = "";
	}

	private String view; // Enumのままでは比較できない
	private String type; // Enumのままでは比較できない
	private boolean isProd; // false = TEST
	private boolean isLayer1Category; // 大カテゴリ
	private String lang;
	private String keyword;
	private String temp;
	private String topUrl; // ロゴクリック時の戻り先
	private String backUrl; // 戻り先URL

	public ModelState getViewState() {
		ModelState ret = ModelState.PROD;
		if (isProd == false) ret = ModelState.TEST;
		return ret;
	}
	// Webカタログ以外のシステム用
	public String getState() {
		String ret = ModelState.PROD.toString();
		if (isProd == false) ret = ModelState.TEST.toString();
		return ret;
	}
	public CategoryType getCategoryType() {
		CategoryType ret = CategoryType.CATALOG;
		if (type.equals(CategoryType.OTHER.toString())) {
			ret = CategoryType.OTHER;
		}
		return ret;
	}
	public boolean isBackToList()
	{
		boolean ret = true;
		if (StringUtils.isEmpty(backUrl) == false && backUrl.indexOf("search") > 0) ret = false;
		return ret;
	}
	public String getCategoryTypeUrl()
	{
		String ret = "";
		if (type != null && type.equalsIgnoreCase("other")) {
			ret += "/other";
		}
		return ret;
	}
	/**
	 * テスト、本番、選択時はカテゴリかシリーズのどちらかに遷移。
	 * シリーズの場合はシリーズ、他の場合はカテゴリ画面を表示
	 * @return
	 */
	public String getCategoryOrSeriesUrl() {
		String ret = "category";
		if (view.equals(ViewState.SERIES.toString())) {
			ret = "series";
		} else if (view.equals(ViewState.DISCON_CATEGORY.toString())) {
			ret = "discontinued/category";
		} else if (view.equals(ViewState.DISCON_SERIES.toString())) {
			ret = "discontinued/series";
		} else if (type != null && type.equalsIgnoreCase("other")) {
			ret += "/other";
		}
		return ret;
	}

	/**
	 * 言語選択時はカテゴリ、シリーズ、リンクマスタのどちらかに遷移。
	 * シリーズの場合はシリーズ、他の場合はカテゴリ画面を表示
	 * @return
	 */
	public String getLangSelectedUrl() {
		String ret = "category";
		if (view.equals(ViewState.SERIES.toString())) {
			ret = "series";
		} else if (view.equals(ViewState.DISCON_CATEGORY.toString())) {
			ret = "discontinued/category";
		} else if (view.equals(ViewState.DISCON_SERIES.toString())) {
			ret = "discontinued/series";
		} else if (view.equals(ViewState.DISCON_TEMPLATE.toString())) {
			ret = "discontinued/template";
		} else if (view.equals(ViewState.SERIES_LINK_MASTER.toString())) {
			ret = "serieslinkmaster";
		} else if (type != null && type.equalsIgnoreCase("other")) {
			ret += "/other";
		}
		return ret;
	}

	public String getViewUrl() {
		String ret = "";
		if (view.equals(ViewState.CATEGORY.toString()))
		{
			ret = "category";
		}
		else if (view.equals(ViewState.OTHER.toString()))
		{
			ret = "category/other";
		}
		else if (view.equals(ViewState.SERIES.toString()))
		{
			ret = "series";
		}
		else if (view.equals(ViewState.SERIES_LINK_MASTER.toString()))
		{
			ret = "serieslinkmaster";
		}
		else if (view.equals(ViewState.USER.toString()))
		{
			ret = "user";
		}
		else if (view.equals(ViewState.LANG.toString()))
		{
			ret = "lang";
		}
		else if (view.equals(ViewState.DISCON_CATEGORY.toString()))
		{
			ret = "discontinued/category";
		}
		else if (view.equals(ViewState.DISCON_SERIES.toString()))
		{
			ret = "discontinued/series";
		}
		else if (view.equals(ViewState.DISCON_TEMPLATE.toString()))
		{
			ret = "discontinued/template";
		}

		return ret;
	}

	@Override
	public
	String toString()
	{
		String ret = "[view=" + view + " type=" + type + " prod=" + isProd + " lang=" + lang + " keyword=" + keyword + " temp=" + temp + "]";
		return ret;
	}
}
