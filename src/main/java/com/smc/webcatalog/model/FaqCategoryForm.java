package com.smc.webcatalog.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import lombok.Getter;
import lombok.Setter;

/**
 * 言語用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class FaqCategoryForm {

	public FaqCategoryForm() {
		seriesFaqFormList = new ArrayList<>();
	}
	@Valid
	private List<SeriesFaqForm> seriesFaqFormList;
}
