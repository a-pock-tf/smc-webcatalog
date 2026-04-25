package com.smc.webcatalog.model.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResult {

	private String message = "NG";
	
	private long hitCount = 0;
	
	private String html = "";
}
