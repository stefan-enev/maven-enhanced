package com.ebay.git.utils;

import org.junit.Test;

public class GitUtilsTest {
	
	@Test
	public void testGitSsh(){
		String repo = GitUtils.getRepositoryName( "git@github.com:snambi/myrepo.git" );
		System.out.println(repo);
	}

	@Test
	public void testGitReadonly(){
		String repo = GitUtils.getRepositoryName("git://github.com/snambi/maven-enhanced.git");
		System.out.println(repo);
	}
	
	@Test
	public void testGitHttp(){
		String repo = GitUtils.getRepositoryName("https://github.com/snambi/myrepo.git");
		System.out.println(repo);
	}	
}
