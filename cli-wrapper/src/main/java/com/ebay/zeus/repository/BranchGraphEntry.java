package com.ebay.zeus.repository;

import org.eclipse.jgit.revwalk.RevCommit;

public class BranchGraphEntry {
	private String branchName;
	private RevCommit startCommit;
	private String fromBranchName;
	
	public String getBranchName() {
		return branchName;
	}
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
	public RevCommit getStartCommit() {
		return startCommit;
	}
	public void setStartCommit(RevCommit startCommit) {
		this.startCommit = startCommit;
	}
	public String getFromBranchName() {
		return fromBranchName;
	}
	public void setFromBranchName(String fromBranchName) {
		this.fromBranchName = fromBranchName;
	}
}
