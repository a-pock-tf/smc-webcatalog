package com.smc.webcatalog.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ログイン用フォーム
 * @author miyasit
 *
 */
@Getter
@Setter
public class SeriesFaqForm implements Serializable {

	private String id;
	
	private String seriesId;
	
	private String lang;
	
	String name;
	@NotNull
	String modelNumber;

	String faq;
	
}
