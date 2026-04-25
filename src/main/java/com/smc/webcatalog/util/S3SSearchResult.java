package com.smc.webcatalog.util;

import java.util.ArrayList;
import java.util.List;

public class S3SSearchResult extends S3SResult {

	List<Candidate> candidate = new ArrayList<Candidate>();

	public List<Candidate> getCandidate() {
		return candidate;
	}

	public void setCandidate(List<Candidate> candidate) {
		this.candidate = candidate;
	}

}
