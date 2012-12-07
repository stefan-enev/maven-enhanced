package com.ebay.maven.nexus;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Repository;
import org.junit.Test;

import com.ebay.zeus.maven.nexus.NexusRequest;

public class NexusRequestTest {
	
	@Test
	public void getJson(){
		
		NexusRequest request = new NexusRequest();
		
		Repository repo1 = new Repository();
		repo1.setId("releases");
		repo1.setUrl("http://d-sjc-00531046.corp.ebay.com:5081/nexus/content/repositories/central/");
		
		request.getRepositories().add(repo1);
		
		Repository repo2 = new Repository();
		repo2.setId("nambi.test");
		repo2.setUrl("http://d-sjc-00531046.corp.ebay.com:5081/nexus/content/repositories/nambi.test/");
		
		request.getRepositories().add(repo2);
		
		
		// add dependencies
		Dependency d1 = new Dependency();
		d1.setGroupId("org.apache");
		d1.setArtifactId("");
		d1.setVersion("");
		
		request.getDependencies().add(d1);
		
		System.out.println(request.toJson());
	}

}
