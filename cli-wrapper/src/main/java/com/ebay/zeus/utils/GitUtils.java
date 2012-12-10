package com.ebay.zeus.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RefMap;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.github.GitHubClient;
import com.ebay.zeus.repositorys.BinaryZeusRepository;
import com.google.common.base.Strings;

public class GitUtils {
	public final static Logger logger = LoggerFactory.getLogger(GitUtils.class);
	
	public static Git initRepository(File repoRoot) throws GitException {
		InitCommand initCmd = Git.init();
		initCmd.setDirectory(repoRoot);
		Git git = null;
		try {
			System.out.println("initializing bare repository");
			git = initCmd.call();
		} catch (GitAPIException e) {
			throw new GitException("unable to initialize repository", e);
		}
		return git;
	}
	
	public static Git cloneRepository(String giturl, File binaryRepoFolder)
			throws GitException {
		CloneCommand cloneCmd = Git.cloneRepository();
		cloneCmd.setURI(giturl);
		cloneCmd.setDirectory(binaryRepoFolder);
		cloneCmd.setCloneAllBranches(true);

		try {
			logger.debug("cloning repository " + giturl);
			return cloneCmd.call();
		} catch (Exception e) {
			throw new GitException("unable to clone " + giturl, e);
		} 
		
	}
	
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
	
	public static String getOrgName( String repository ){
		
		String orgName=null;
		
		if( repository == null || repository.trim().equals("")){
			throw new NullPointerException("Repository name cannot be null or empty");
		}
		
		// git ssh format
		// git@github.com:snambi/myrepo.git
		if( repository.contains( "@" )){
			String[]  s = repository.split(":");
			
			if( s != null && s.length == 2 ){
				
				// take the first second one
				String gitrepo = s[1];
				
				if( gitrepo.contains(".git")){
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
		if( repository.startsWith("git://")){
			
			String[] s = repository.split("/");
			
			if( s!= null  && s.length >= 3 ){
				orgName = s[ s.length -2 ];
			}
		}
		
		// http format
		// https://github.com/snambi/myrepo.git
		if( repository.startsWith("http")){
			
			String[] s = repository.split("\\/");
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

	//TODO: AddCommand support to add files as pattern, so could add buntch of files in one shot
	//      I mean needn't this method to get files to add.
	public static Collection<String> getFilesToStage( StatusCommand statusCmd ){
		
		Collection<String> tobeAdded = new ArrayList<String>();
		
		try {
			Status status = statusCmd.call();
			
			
			Collection<String> issues = new ArrayList<String>();
			
			tobeAdded.addAll(status.getModified());
			tobeAdded.addAll(status.getChanged());
			tobeAdded.addAll(status.getRemoved());
			tobeAdded.addAll(status.getUntracked());
			
			/*for( String file : tobeAdded){
				System.out.println(file);
			}*/
			
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

    public static boolean existsInGit(final String repo) throws IOException {
        if (Strings.isNullOrEmpty(repo)) return false;

        final org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepository(repo);
        final StoredConfig config = repository.getConfig();
        final String name = config.getString("user", null, "name");
        final String email = config.getString("user", null, "email");
        String[] split = new String[2];
        if (name == null || email == null) {
            System.out.println("User identity is unknown!");
        } else {
            split = email.split("@");
            System.out.println("User identity is " + name + " <" + email + ">");
        }

        String userName = null;
        if (split.length > 0 ) {
            userName = split[0];
        }

        if (!Strings.isNullOrEmpty(userName)) {
            final GHUser user = new GitHubClient().connect().getUser(userName);
            if (user != null) {
                return user.getRepository(repo) != null;
            }
        }
        return false;
    }
    
    //FIXME: not sure why get last commit, latest one?
    public static RevCommit getLastCommit( Repository repository ) throws GitException{
		// get the history from binary repository
		Git bingit = Git.wrap(repository);
		RevWalk binwalk = new RevWalk(repository);
		
		Iterable<RevCommit> logs;
		try {
			logs = bingit.log().call();
			Iterator<RevCommit> i = logs.iterator();
			
			RevCommit commit = null;
			while( i.hasNext() ){
				commit = binwalk.parseCommit(i.next() );
			}
			
			if (commit == null){
				throw new GitException("Haven't found any commits in specified repository:"+repository.getDirectory().getAbsolutePath());
			}
			
			return commit;
		} catch (Exception e) {
			throw new GitException(e);
		}
    }

    public static List<RevCommit> getAllCommitsHistory( Repository repository ){
    	Git git = Git.wrap(repository);
    	LogCommand logCmd = git.log();
    	
    	try {
    		List<RevCommit> commitList = new ArrayList<RevCommit>();
    		
    		Iterable<RevCommit> commits = logCmd.call();
    		Iterator<RevCommit> iterator = commits.iterator();
    		while(iterator.hasNext()){
    			commitList.add(iterator.next());
    		}
    		
    		return commitList;
		} catch (Exception e) {
			//TODO: should log it, but shouldn't block whole process
		}
		
		return Collections.emptyList();
    }
    
    public static boolean isRemoteBranchExists( Repository repository, String branch ){
    	boolean result = false;
    	
    	List<String> branches = getAllRemoteBranches(repository);
    	
    	for( String b : branches ){
    		if( b.contains(branch)){
    			result = true;
    			break;
    		}
    	}
    	return result;
    }

    //TODO: currently, only analyze local git repo's refs instead of talk with remote git repo.
    public static List<String> getAllRemoteBranches( Repository repository){
    	
    	Iterable<Ref> refs;
    	
    	// get the sorted refs
    	Map<String, Ref> all = repository.getAllRefs();
		if (all instanceof RefMap
				|| (all instanceof SortedMap && ((SortedMap) all).comparator() == null)){
			refs= all.values();
		}else{
			refs= RefComparator.sort(all.values());
		}
		
		List<String> branches = new ArrayList<String>();
		for (final Ref r : refs ) {
			String branch = getRemoteBranchName(r.getName());
						
			// ignore branches with name 'HEAD' or ""(not remote branch)
			if( branch.endsWith("HEAD") || branch.startsWith("heads")
					|| branch.startsWith(Constants.R_TAGS) || branch.equals("")){
				continue;
			}else{
				branches.add( branch);
			}
		}
		
		return branches;
    }
    
    private static String getRemoteBranchName(String refName){
    	if (refName.startsWith(Constants.R_REMOTES))
			return refName.substring(Constants.R_REMOTES.length());
		return "";
    }

    /**
     * add all changes into staging area, then commit them all.
     * 
     * @param git
     * @param commitMessage
     * @throws GitException
     */
    public static void addAndCommitAllChanges(Git git, String commitMessage) throws GitException{
    	addAllChanges(git);
    	commitAllChanges(git, commitMessage);
    }

	private static void addAllChanges(Git git) throws GitException{
    	AddCommand addCmd = git.add();
		addCmd.addFilepattern(".");
        try {
			addCmd.call();
		} catch (Exception e) {
			throw new GitException("unable to add files", e);
		}
    }
    
	private static void commitAllChanges(Git git, String commitMessage) throws GitException {
        CommitCommand commit = git.commit();
		commit.setMessage(commitMessage);
        
		try{
			commit.call();
		}catch(Exception e){
			throw new GitException("fail to commit changes.", e);
		}
	}
    
    public static void main(String[] args) throws Exception {
        System.out.println("MAIN" + GitUtils.existsInGit("binrepo-devex"));
        System.out.println("MAIN" + GitUtils.existsInGit("CreatedUsingGitHub-API-Client"));
        System.out.println("MAIN Nambi" + GitUtils.existsInGit("maven-enhanced"));
    }


}
