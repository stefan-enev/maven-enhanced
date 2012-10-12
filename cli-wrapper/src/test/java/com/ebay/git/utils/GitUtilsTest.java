package com.ebay.git.utils;

import org.junit.Test;

public class GitUtilsTest {
	
	public final static String repoUrlGitSSH = "git@github.com:snambi/myrepo.git";
	public final static String repoUrlGitReadonly = "git://github.com/snambi/maven-enhanced.git";
	public final static String repoUrlGitHttps = "https://github.com/snambi/myrepo.git";
	
	@Test
	public void testGitSsh(){
		
		String repo = GitUtils.getRepositoryName( repoUrlGitSSH  );
		System.out.println( repoUrlGitSSH + " : " + repo);
	}

	@Test
	public void testGitReadonly(){
		String repo = GitUtils.getRepositoryName(  repoUrlGitReadonly);
		System.out.println( repoUrlGitReadonly + " : " + repo);
	}
	
	@Test
	public void testGitHttp(){
		String repo = GitUtils.getRepositoryName( repoUrlGitHttps);
		System.out.println(repoUrlGitHttps + " : " + repo );
	}	
	
	@Test
	public void getGitOrgNameSsh(){
		String org = GitUtils.getOrgName(repoUrlGitSSH);
		System.out.println( "org for " + repoUrlGitSSH + " = " + org);
	}
	
	@Test
	public void getGitOrgNameGitReadonly(){
		String org = GitUtils.getOrgName(repoUrlGitReadonly);
		System.out.println( "org for " + repoUrlGitReadonly + " = " + org);
	}
	
	@Test
	public void getGitOrgNameGitHtts(){
		String org = GitUtils.getOrgName(repoUrlGitHttps);
		System.out.println( "org for " + repoUrlGitHttps + " = " + org);
	}
}
