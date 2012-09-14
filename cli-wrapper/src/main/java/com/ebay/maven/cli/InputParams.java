package com.ebay.maven.cli;

import java.util.ArrayList;
import java.util.List;

public class InputParams {

	private RunMode mode;
	private List<String> actions = new ArrayList<String>();

	public RunMode getMode() {
		return mode;
	}

	public void setMode(RunMode mode) {
		this.mode = mode;
	}

	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}

}
