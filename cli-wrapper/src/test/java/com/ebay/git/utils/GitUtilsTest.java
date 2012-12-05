package com.ebay.git.utils;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Test;

import com.ebay.maven.binaryrepository.GitException;

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
	
	@Test
	public void getLastCommit(){
		
		FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
		
		try {
			
			Repository repository = repobuiler.findGitDir().build();
			GitUtils.getLastCommit(repository);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void listAllBranches(){
		
		FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
		
		try {
			
			Repository repository = repobuiler.findGitDir().build();
			List<String> branches = GitUtils.getAllRemoteBranches(repository);
			
			for( String branch : branches ){
				System.out.println(branch);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
