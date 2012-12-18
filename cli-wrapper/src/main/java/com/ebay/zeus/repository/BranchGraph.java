package com.ebay.zeus.repository;

import java.util.List;

import com.ebay.zeus.exceptions.GitException;

public interface BranchGraph {

	/**
	 * add new branch graph entry.
	 * 
	 * @param entry
	 */
	public void add(BranchGraphEntry entry);
	
	/**
	 * clear up branch graph entries.
	 */
	public void clear();
	
	/**
	 * it will sort branches by level.
	 * Such as branch A is cut from master.
	 * so master should be lv 1, and branch A should be lv 2.
	 * Branches in the same level, the order doesn't matter.
	 * 
	 * @return
	 */
	public List<BranchGraphEntry> getSortedBranches() throws GitException;
}
