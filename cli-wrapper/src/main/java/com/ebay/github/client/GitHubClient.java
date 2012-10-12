package com.ebay.github.client;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;

public class GitHubClient {

	private static String githubUrl = "https://github.scm.corp.ebay.com/api/v3";

	// private static String accessToken = "5d8e186b08062ca405ab25d489fca9823c2a7136";

    private static String accessToken = "1cf7d9792235b8592eda18bd7dcc2de37f99b3bc";

    private GitHub github;
	
	public GitHub getGithub() {
		return github;
	}

	public void setGithub(GitHub github) {
		this.github = github;
	}

	public GitHubClient() throws IOException {
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


	@Deprecated
    public GitHub connect() {
        /*String githubUrl = "https://github.scm.corp.ebay.com/api/v3/";
        String accessToken = "1cf7d9792235b8592eda18bd7dcc2de37f99b3bc";

        GitHub gitHub = null;
        try {
            gitHub = GitHub.connectUsingOAuth(githubUrl, accessToken);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
        return this.github;
    }

    public static void main(String[] args) {
        GitHubClient client;
        try {
            client = new GitHubClient();
            System.out.println(client.github.getMyself().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
