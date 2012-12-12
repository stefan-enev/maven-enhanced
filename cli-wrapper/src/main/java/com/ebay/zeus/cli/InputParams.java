package com.ebay.zeus.cli;


public class InputParams {

	private RunMode mode;
	private String mapSvcUrl;
	private String srcRepoRoot;
	
	public RunMode getMode() {
		return mode;
	}

	public void setMode(RunMode mode) {
		this.mode = mode;
	}

	public String getMapSvcUrl() {
		return mapSvcUrl;
	}

	public void setMapSvcUrl(String mapSvcUrl) {
		this.mapSvcUrl = mapSvcUrl;
	}
	
	public void setSourceRepoRoot(String sourceRepoRoot){
		this.srcRepoRoot = sourceRepoRoot;
	}
	
	public String getSourceRepoRoot(){
		return this.srcRepoRoot;
	}
}
