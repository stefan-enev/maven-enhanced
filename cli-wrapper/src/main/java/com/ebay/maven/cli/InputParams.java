package com.ebay.maven.cli;


public class InputParams {

	private RunMode mode;
	private String mapSvcUrl;
	
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
}
