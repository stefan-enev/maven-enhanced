package com.ebay.git.utils;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

import java.util.ArrayList;
import java.util.Collection;

public class GitUtils {
	
	public static String getRepositoryName( String repository ){
		
		String repositoryName=null;
		
		if( repository == null || repository.trim().equals("")){
			throw new NullPointerException("Repository name cannot be null or empty");
		}
		
		// git ssh format
		// git@github.com:snambi/myrepo.git
		if( repository.contains( "@" )){
			String[]  s = repository.split("/");
			
			if( s != null && s.length >=2 ){
				
				// take the last one
				String gitrepo = s[s.length-1];
				
				if( gitrepo.contains(".git")){
					repositoryName = getRepo(gitrepo);
				}
			}
		}
		
		// git readonly format
		// git://github.com/snambi/maven-enhanced.git
		if( repository.startsWith("git://")){
			
			String[] s = repository.split("/");
			
			if( s!= null  && s.length >= 2 ){
				repositoryName = getRepo(s[ s.length -1 ]);
			}
		}
		
		// http format
		// https://github.com/snambi/myrepo.git
		if( repository.startsWith("http")){
			
			String[] s = repository.split("\\/");
			if( s!= null  && s.length >= 2 ){
				repositoryName = getRepo(s[ s.length -1 ]);
			}
		}
		
		return repositoryName;
	}
	
	/**
	 * extracts repository name from string of the format "repo.git".
	 * @param repository
	 * @return
	 */
	public static String getRepo(String input ){
		String repository=null;
		
		if( input != null && !input.trim().equals("")){
			
			if( input.contains(".git")){
				String[] a = input.split("\\.");
				repository = a[0];
			}else{
				repository = input;
			}
			
		}
		
		return repository;
	}

	public static Collection<String> getFilesToStage( StatusCommand statusCmd ){
		
		Collection<String> tobeAdded = new ArrayList<String>();
		
		try {
			Status status = statusCmd.call();
			
			
			Collection<String> issues = new ArrayList<String>();
			
			tobeAdded.addAll(status.getModified());
			tobeAdded.addAll(status.getChanged());
			tobeAdded.addAll(status.getRemoved());
			tobeAdded.addAll(status.getUntracked());
			
			for( String file : tobeAdded){
				System.out.println(file);
			}
			
			issues.addAll(status.getConflicting());
			issues.addAll(status.getMissing());
			
			for( String file: issues ){
				// TODO: what to do? throw error
				System.out.println("BAD: " + file);
			}
			
		} catch (NoWorkTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tobeAdded;
	}
}
