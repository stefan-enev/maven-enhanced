package com.ebay.github.client;

import java.io.IOException;
import java.util.Map;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class GitHubClient {

	private static String githubUrl = "https://github.scm.corp.ebay.com/api/v3/";
	private static String accessToken = "5d8e186b08062ca405ab25d489fca9823c2a7136";
	private GitHub github;
	
	public GitHubClient() throws IOException{
			github = GitHub.connectUsingOAuth(githubUrl, accessToken);
	}
	
	public GitHubClient( String githubserver, String accessToken ) throws IOException{
		github = GitHub.connectUsingOAuth(githubserver, accessToken);
	}
	
	public void listRepositories( String organization ){
		try {
			GHOrganization org = github.getOrganization(organization);
			Map<String,GHRepository> result = org.getRepositories();
			
			for( String key : result.keySet() ){
				GHRepository repo = result.get(key);
				System.out.println( key + " : " + repo.getUrl() );
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
