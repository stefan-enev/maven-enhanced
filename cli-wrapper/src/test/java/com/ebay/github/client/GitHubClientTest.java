package com.ebay.github.client;

import java.io.IOException;

import org.junit.Test;

import com.ebay.github.client.GitHubClient;

public class GitHubClientTest {
	
	@Test
	public void listRepos(){
		try {
			GitHubClient client = new GitHubClient();
			client.listRepositories("Binary");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
