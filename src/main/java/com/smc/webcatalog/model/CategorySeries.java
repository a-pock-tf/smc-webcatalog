package com.smc.webcatalog.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * CategoryとSeriesの結びつき
 * @note ManyToManyだが、Categoryからの表示、結び付きが多いので 1:n
 *
 */
@Document(collection = "category_series")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class CategorySeries {

	//初期化は明示的に
	public CategorySeries() {
		setSeriesList(new ArrayList<Series>());
	}

	@Id
	private String id;

	// カテゴリID
	@TextIndexed
	private String categoryId;

	// シリーズ
	@DBRef
	private List<Series> seriesList;

}
