package com.ebay.zeus.repository;

import java.util.Collections;
import java.util.List;

public class EmptyBranchGraph implements BranchGraph {

	public List<BranchGraphEntry> getSortedBranches() {
		return Collections.emptyList();
	}

	public void add(BranchGraphEntry entry) {
		// NTD
	}

	public void clear() {
		// NTD
	}

}
