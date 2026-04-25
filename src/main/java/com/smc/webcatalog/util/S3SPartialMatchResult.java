package com.smc.webcatalog.util;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S3SPartialMatchResult {

	private String hitCount;
	private String pageCurrent;
	private List<S3SPartialMatchResultData> searchData;

}
