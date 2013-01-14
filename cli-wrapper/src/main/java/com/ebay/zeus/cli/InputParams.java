package com.ebay.zeus.cli;


public class InputParams {

	private RunMode mode;
	private String srcRepoRoot;
	
	public RunMode getMode() {
		return mode;
	}

	public void setMode(RunMode mode) {
		this.mode = mode;
	}
	
	public void setSourceRepoRoot(String sourceRepoRoot){
		this.srcRepoRoot = sourceRepoRoot;
	}
	
	public String getSourceRepoRoot(){
		return this.srcRepoRoot;
	}
}
