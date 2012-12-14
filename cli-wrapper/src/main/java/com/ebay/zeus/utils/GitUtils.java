package com.ebay.zeus.utils;

import java.io.File;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;

public class GitUtils {
	public final static Logger logger = LoggerFactory.getLogger(GitUtils.class);
	
	/**
	 * "git init"
	 * 
	 * @param repoRoot
	 * @return
	 * @throws GitException
	 */
	public static Git initRepository(File repoRoot) throws GitException {
		logger.info("initializing bare repository:"+repoRoot);
		
		try {
			FileUtils.mkdir(repoRoot, true);
			
			InitCommand initCmd = Git.init();
			initCmd.setDirectory(repoRoot);
			
			Git git = initCmd.call();
			
			return git;
		} catch (Exception e) {
			throw new GitException("unable to initialize repository", e);
		}
	}
	
	/**
	 * "git clone <git url>"
	 * 
	 * @param giturl
	 * @param repoDirectory
	 * @return
	 * @throws GitException
	 */
	public static Git cloneRepository(String giturl, File repoDirectory)
			throws GitException {
		CloneCommand cloneCmd = Git.cloneRepository();
		cloneCmd.setURI(giturl);
		cloneCmd.setDirectory(repoDirectory);
		cloneCmd.setCloneAllBranches(true);

		try {
			logger.debug("cloning repository " + giturl);
			return cloneCmd.call();
		} catch (Exception e) {
			throw new GitException("unable to clone " + giturl, e);
		} 
		
	}
	
	/**
	 * get repository name with remote git url
	 * 
	 * @param remoteUrl
	 * @return
	 */
	public static String getRepositoryName( String remoteUrl ){
		
		String repositoryName=null;
		
		if( remoteUrl == null || remoteUrl.trim().equals("")){
			throw new NullPointerException("Repository name cannot be null or empty");
		}
		
		// git ssh format
		// git@github.com:snambi/myrepo.git
		if( remoteUrl.contains( "@" )){
			String[]  s = remoteUrl.split("/");
			
			if( s != null && s.length >=2 ){
				
				// take the last one
				String gitrepo = s[s.length-1];
				
				if( gitrepo.contains(Constants.DOT_GIT)){
					repositoryName = getRepo(gitrepo);
				}
			}
		}
		
		// git readonly format
		// git://github.com/snambi/maven-enhanced.git
		if( remoteUrl.startsWith("git://")){
			
			String[] s = remoteUrl.split("/");
			
			if( s!= null  && s.length >= 2 ){
				repositoryName = getRepo(s[ s.length -1 ]);
			}
		}
		
		// http format
		// https://github.com/snambi/myrepo.git
		if( remoteUrl.startsWith("http")){
			
			String[] s = remoteUrl.split("\\/");
			if( s!= null  && s.length >= 2 ){
				repositoryName = getRepo(s[ s.length -1 ]);
			}
		}
		
		return repositoryName;
	}
	
	/**
	 * get org name from remote git url.
	 * 
	 * @param gitUrl
	 * @return
	 */
	public static String getOrgName( String gitUrl ){
		
		String orgName=null;
		
		if( gitUrl == null || gitUrl.trim().equals("")){
			throw new NullPointerException("Repository name cannot be null or empty");
		}
		
		// git ssh format
		// git@github.com:snambi/myrepo.git
		if( gitUrl.contains( "@" )){
			String[]  s = gitUrl.split(":");
			
			if( s != null && s.length == 2 ){
				
				// take the first second one
				String gitrepo = s[1];
				
				if( gitrepo.contains(Constants.DOT_GIT)){
					if( gitrepo.contains("/")){
						String o[] = gitrepo.split("/");
						if( o.length == 2){
							orgName = o[0];
						}
					}
				}
			}
		}
		
		// git readonly format
		// git://github.com/snambi/maven-enhanced.git
		if( gitUrl.startsWith("git://")){
			
			String[] s = gitUrl.split("/");
			
			if( s!= null  && s.length >= 3 ){
				orgName = s[ s.length -2 ];
			}
		}
		
		// http format
		// https://github.com/snambi/myrepo.git
		if( gitUrl.startsWith("http")){
			
			String[] s = gitUrl.split("\\/");
			if( s!= null  && s.length >= 3 ){
				orgName = s[ s.length -2 ];
			}
		}
		
		return orgName;
	}
	
	/**
	 * extracts repository name from string of the format "repo.git".
	 * @param input
	 * @return
	 */
	private static String getRepo(String input ){
		String repository=null;
		
		if( input != null && !input.trim().equals("")){
			
			if( input.contains(Constants.DOT_GIT)){
				String[] a = input.split("\\.");
				repository = a[0];
			}else{
				repository = input;
			}
			
		}
		
		return repository;
	}

	public static String getShortBranchName(String fullBranchName) {
		if (fullBranchName.startsWith(com.ebay.zeus.utils.Constants.ORIGIN_BRANCHNAME_PREFIX)){
			fullBranchName = fullBranchName
					.substring(com.ebay.zeus.utils.Constants.ORIGIN_BRANCHNAME_PREFIX
							.length());
		}
		return fullBranchName;
	}
}
