package com.ebay.zeus.repositorys;

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
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefComparator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
	public Git git = Git.wrap(this);
	
	public ZeusRepository(File gitDir) throws IOException {
		super(gitDir);
	}
	
	public String getName(){
		String remoteUrl = this.getConfig().getString("remote", "origin", "url");
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
		
		Git srcgit = Git.wrap(this);
		
		try {
			
			PullResult pullResult = srcgit.pull().call();
			
			if( pullResult.isSuccessful()){

				//comment out it, no updates, that's ok, pull is successful.
//				if( pullResult.getFetchResult() != null &&
//						( pullResult.getFetchResult().getTrackingRefUpdates().size() > 0 ) ){
//					result = true;
//				}
				if( pullResult.getMergeResult() != null && 
						(pullResult.getMergeResult().getMergeStatus() != MergeStatus.FAST_FORWARD ||
						pullResult.getMergeResult().getMergeStatus() != MergeStatus.MERGED )){
					throw new Exception("can't merge changes successfully.");
				}
				
				// TODO: rebase status needs to be checked but it is ignored for now
			}
			
		} catch (Exception e) {
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
        
        final RevWalk revWalk = new RevWalk(this);
        ObjectId resolve;
		try {
			resolve = this.resolve(Constants.HEAD);
			final RevCommit commitRev = revWalk.parseCommit(resolve);
	        return commitRev.getName();
		} catch (Exception e) {
			throw new GitException("unable to get HEAD for repository:" + this.getDirectory(), e);
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
					|| branch.startsWith(Constants.R_TAGS) || branch.equals("")) {
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
			System.out.println("checkout is complete" );
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
	
	//TODO
	public void checkoutCommit(String commitHash){
		
	}
	
	//TODO:
	public List<RevCommit> getNewCommits(String branchName) throws GitException{
    	return Collections.emptyList();
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
    		
    		return commitList;
		} catch (Exception e) {
			throw new GitException("", e);
		}
	}
	
}
