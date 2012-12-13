package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefComparator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.util.RefMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.GitUtils;

/**
 * It's kind of wrapper class for git operations 
 * and  would provide below capacities:
 * 
 * 1. git pull
 * 2. get current branche & current commit.
 * 3. get all branches
 * 4. get changed files for one commit
 * 5. git checkout branch
 * 6. git checkout specific commit
 * 
 * 
 * @author yunfwang@ebay.com
 *
 */
public class ZeusRepository extends FileRepository{

	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	public Git git;
	
	public ZeusRepository(File gitDir) throws IOException {
		super(gitDir);
		git = Git.wrap(this);

		git.open(gitDir);
	}
	
	public String getName(){
		String remoteUrl = this.getConfig().getString("remote", "origin", "url");
		
		if (remoteUrl==null){
			return null;
		}
		
		String repository = GitUtils.getRepositoryName(remoteUrl);
		return repository;
	}
	
	public String getRemoteUrl() {
		return this.getConfig().getString("remote", "origin", "url");
	}
	
	/**
	 * execute "git pull" for specified repository
	 * pull result must be successful, merge result should be MergeStatus.FAST_FORWARD || MergeStatus.MERGED
	 * 
	 * @throws GitException
	 */
	public void pull() throws GitException{
		
//		Git srcgit = Git.wrap(this);
		
		try {
			
			PullResult pullResult = git.pull().call();
			
			if( pullResult.isSuccessful()){

				//comment out it, no updates, that's ok, pull is successful.
//				if( pullResult.getFetchResult() != null &&
//						( pullResult.getFetchResult().getTrackingRefUpdates().size() > 0 ) ){
//					result = true;
//				}
				if( pullResult.getMergeResult() != null && 
						pullResult.getMergeResult().getMergeStatus() != MergeStatus.FAST_FORWARD &&
						pullResult.getMergeResult().getMergeStatus() != MergeStatus.MERGED &&
						pullResult.getMergeResult().getMergeStatus() != MergeStatus.ALREADY_UP_TO_DATE){
					throw new Exception("can't merge changes successfully.");
				}
				
				// TODO: rebase status needs to be checked but it is ignored for now
			}
			
		} catch (TransportException e) {
			//ignore, if noting to fetch.
			if (e.getMessage().equals("Nothing to fetch.")){
				return;
			}
			throw new GitException(e);
		} catch (Exception e){
			throw new GitException(e);
		}
	}
	
	/**
	 * get HEAD commit hash code.
	 * 
	 * @return HEAD's commit hash
	 * @throws GitException
	 */
	public String getHead() throws GitException{
        
        ObjectId head;
		try {
			head = this.resolve(Constants.HEAD);
			
			if (head==null)
				return "";
			
	        return head.getName();
		} catch (Exception e) {
			throw new GitException("unable to get HEAD for repository:" + this.getDirectory(), e);
		}
	}
	
	/**
	 * get latest commit by 'git log'
	 * 
	 * @return
	 * @throws GitException
	 */
	public RevCommit getHeadCommit() throws GitException{
		try {
			return this.getAllCommits().get(0);
		} catch (GitException e) {
			throw new GitException("fail to get commit history for repository" + this.getDirectory().getParent(), e);
		}
	}
	
	/**
	 * get all branches
	 * 
	 * @return branch list
	 */
	public List<String> getAllBranches(){
		Iterable<Ref> refs;

		// get the sorted refs
		Map<String, Ref> all = this.getAllRefs();
		if (all instanceof RefMap
				|| (all instanceof SortedMap && ((SortedMap) all).comparator() == null)) {
			refs = all.values();
		} else {
			refs = RefComparator.sort(all.values());
		}

		List<String> branches = new ArrayList<String>();
		for (final Ref r : refs) {
			String branch = this.shortenRefName(r.getName());

			// ignore branches with name 'HEAD' or ""(not remote branch)
			if (branch.endsWith("HEAD") || branch.startsWith("heads")
					|| branch.startsWith(Constants.R_TAGS) || branch.equals("") || !branch.startsWith("origin")) {
				continue;
			} else {
				branches.add(branch);
			}
		}

		return branches;
	}
	
	/**
	 * check whether specified branch existed or not.
	 * 
	 * @param branchName
	 * @return existed or not
	 */
	public boolean isBranchExisted(String branchName){
		boolean result = false;
    	
    	List<String> branches = getAllBranches();
    	
    	for( String b : branches ){
    		if( b.contains(branchName)){
    			result = true;
    			break;
    		}
    	}
    	return result;
	}
	
	//TODO
	public List<File> getChangedFiles(String commitHash){
		return Collections.emptyList();
	}
	
	/**
	 * checkout local branch.
	 * 
	 * @param branchName
	 * @return
	 * @throws GitException
	 */
	public CheckoutResult checkoutBranch(String branchName) throws GitException{
		CheckoutCommand checkoutCmd = git.checkout();
		Ref ref;
		try {
			ref = checkoutCmd.setName(branchName).call();
			logger.info("checkout is complete");
			if( ref != null ){
				//System.out.println("ref " + ref.getName() );
				return checkoutCmd.getResult();
			}
		} catch (Exception e) {
			throw new GitException("unable to checkout branch " + branchName, e);
		}
		
		return null;
	}
	
	/**
	 * checkout remote branch.
	 * it will create local branch and set tracking for remote branch.
	 * 
	 * @param branchName
	 * @return
	 * @throws GitException
	 */
	public CheckoutResult checkoutRemoteBranch(String branchName) throws GitException{
		CheckoutCommand checkoutCmd = git.checkout();
		checkoutCmd.setCreateBranch(true);
		checkoutCmd.setName( branchName);
		checkoutCmd.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK );
		checkoutCmd.setStartPoint( "origin/" + branchName );

		System.out.println("checking out branch " + branchName );
		
		try {
            //Ref branch = branchCmd.call();
			Ref ref = checkoutCmd.call();
			System.out.println("checkout is complete" );
			if( ref != null ){
				//System.out.println("ref " + ref.getName() );
				return checkoutCmd.getResult();
			}
			
			return null;
		} catch (Exception e) {
			throw new GitException("unable to checkout branch " + branchName, e);
		}
	}
	
	/**
	 * git reset --hard <SHA>
	 * but if after reset, it's better to git pull again to rollback to latest version.
	 * 
	 * @param commitHash
	 * @throws GitException 
	 */
	public void reset(String commitHash) throws GitException{
		try {
			git.reset().setMode(ResetType.HARD).setRef(commitHash).call();
		} catch (Exception e) {
			throw new GitException("fail to reset commit:"+commitHash+" to repository:"+this.getDirectory().getParent(), e);
		}
	}
	
	/**
	 * get current branch's all commits
	 * @return commit list
	 * @throws GitException
	 */
	public List<RevCommit> getAllCommits() throws GitException{
		LogCommand logCmd = git.log();
    	
    	try {
    		List<RevCommit> commitList = new ArrayList<RevCommit>();
    		
    		Iterable<RevCommit> commits = logCmd.call();
    		Iterator<RevCommit> iterator = commits.iterator();
    		while(iterator.hasNext()){
    			commitList.add(iterator.next());
    		}
    		
    		Collections.reverse(commitList);
    		
    		return commitList;
		} catch (Exception e) {
			throw new GitException("", e);
		}
	}
	
	/**
	 * add remote url into git config.
	 * 
	 * @param remoteUrl
	 * @throws GitException 
	 */
	public void addRemoteUrl(String remoteUrl) throws GitException{
		this.getConfig().setString("remote", "origin", "url", remoteUrl);
		this.getConfig().setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
		
        try {
			this.getConfig().save();
		} catch (IOException e) {
			throw new GitException("fail to add remote url: "+remoteUrl+" to repository: "+ this.getDirectory(), e);
		}
	}
	
	/**
	 * add new branch into git config
	 * 
	 * @param branchName
	 * @throws GitException 
	 */
	public void addRemoteBranch(String branchName) throws GitException{
		FileBasedConfig config = this.getConfig();
		config.setString("branch", branchName, "remote", "origin");
		config.setString("branch", branchName, "merge", "refs/heads/"+ branchName);
		try {
			config.save();
		} catch (IOException e) {
			throw new GitException("fail to add branch: "+branchName+" to repository: "+ this.getDirectory(), e);
		}
	}
	
	/**
	 * check whether this branch has this commit hash
	 * 
	 * @param commitHash
	 * @return
	 * @throws GitException
	 */
	public boolean hasCommit(String commitHash) throws GitException{
		if (commitHash == null || "".equals(commitHash)){
			throw new GitException("commit SHA shouldn't be empty.");
		}
		
		List<RevCommit> allCommits = getAllCommits();
		for (RevCommit commit:allCommits){
			
			//previous create/update will put source repo's commit hash as binary repo's commit message.
			if (commitHash.equals(commit.getFullMessage())){
				return true;
			}
		}
		
		return false;
	}
}
