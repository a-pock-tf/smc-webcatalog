package com.smc.webcatalog.util;

import java.util.ArrayList;
import java.util.List;

public class JpServiceResult extends S3SResult {

	private List<S3SSeriesInfo> data = new ArrayList<S3SSeriesInfo>();

	public List<S3SSeriesInfo> getData() {
		return data;
	}

	public void setData(List<S3SSeriesInfo> data) {
		this.data = data;
	}

}
