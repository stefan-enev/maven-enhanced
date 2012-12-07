package com.ebay.zeus.repositorys;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
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
	
	//TODO
	public List<String> getAllBranches(){
		return Collections.emptyList();
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
	
	//TODO
	public List<RevCommit> getNewCommits(String branchName) throws GitException{
		// get the history from binary repository
		Git bingit = Git.wrap(this);
		RevWalk binwalk = new RevWalk(this);
		
		Iterable<RevCommit> logs;
		try {
			logs = bingit.log().call();
			Iterator<RevCommit> i = logs.iterator();
			
			while( i.hasNext() ){
				RevCommit commit = binwalk.parseCommit(i.next() );
				System.out.println( commit.getFullMessage() );
			}
			
		} catch (NoHeadException e) {
			throw new GitException(e);
		} catch (GitAPIException e) {
			throw new GitException(e);
		} catch (MissingObjectException e) {
			throw new GitException(e);
		} catch (IncorrectObjectTypeException e) {
			throw new GitException(e);
		} catch (IOException e) {
			throw new GitException(e);
		}
		return Collections.emptyList();
	}
	
}
