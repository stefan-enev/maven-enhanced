package com.ebay.zeus.maven.nexus;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Repository;

import com.google.gson.Gson;

public class NexusRequest {
	
	private List<Repository> repositories = new ArrayList<Repository>();
	private List<Dependency> dependencies = new ArrayList<Dependency>();
	
	public List<Repository> getRepositories() {
		return repositories;
	}
	public void setRepositories(List<Repository> repositories) {
		this.repositories = repositories;
	}
	public List<Dependency> getDependencies() {
		return dependencies;
	}
	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}
	
	public String toJson(){
		Gson gson = new Gson();
		String jsonStr = gson.toJson(this);
		return jsonStr;
	}
}
