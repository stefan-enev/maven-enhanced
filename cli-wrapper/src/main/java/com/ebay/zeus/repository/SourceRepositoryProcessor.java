package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.Constants;
import com.ebay.zeus.utils.FileUtil;
import com.ebay.zeus.utils.TimeTracker;
import com.ebay.zeus.utils.ZeusUtil;

/**
 * it major in "setup" case for source repository according to binary repository.
 * 
 * @author yunfwang@ebay.com
 *
 */
public class SourceRepositoryProcessor extends ZeusRepositoryProcessor{
	
	public SourceRepositoryProcessor(SourceZeusRepository srcRepo, BinaryZeusRepository binRepo){
		super(srcRepo, binRepo);
	}

	/**
	 * setup project, it will try to get pre-compiled classes
	 * and copy them into source repository's target folders.
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception{
		TimeTracker tracker = new TimeTracker();
		tracker.start();
		
		// TODO: download dependencies
		
		if (!ZeusUtil.isRemoteBinaryRepositoryExisted(srcRepo.getRemoteUrl())) {
			throw new GitException("Remote binary repository not available("+srcRepo.getRemoteUrl()+"). exiting...");
		}
		
		if (ZeusUtil.targetClassesExisted(srcRepoRoot)){
			throw new GitException("Have existed classes, needn't Zeus. Existing...");
		}
		
		if (ZeusUtil.haveLocalChangedJavaFiles(srcRepo)){
			throw new GitException("Have local changed java files, needn't Zeus. Existing...");
		}
		
		RevCommit targetCommit = getJavaTargetCommit(srcRepo);
		
		if (ZeusUtil.isLocalBinaryRepositoryExisted(srcRepoRoot)) {
			File binaryRepoRoot= ZeusUtil.getExistedBinaryRepositoryRoot(srcRepoRoot);
			File binGit = new File(binaryRepoRoot, Constants.DOT_GIT);		
			binRepo = new BinaryZeusRepository(binGit);
			binRepo.pull();
		}else{
			String binRepoRemoteUrl = ZeusUtil.getBinaryRemoteUrl(true, srcRepo);
			boolean existed = ZeusUtil.isExistedBranchCommit(binRepoRemoteUrl,
					srcRepo.getBranch(), targetCommit.getName());
			
			if (!existed){
				return;
			}
			
			this.binRepo = ZeusUtil.cloneBinaryRepository(true, srcRepo);
		}
		
		//checkout binary branch.
		if (!binRepo.isBranchExisted(srcRepo.getBranch())){
			binRepo.checkoutBranch(Constants.MASTER_BRANCH);
			throw new GitException("binary repository hasn't branch:"+srcRepo.getBranch() +". Exiting Zeus...");
		}
		
		//reset binary repo's commit
		String binaryCommitHash = this.getBinaryStartCommitHash(
				srcRepo.getBranch(), targetCommit.getName());
		
		if (binaryCommitHash == null){
			binRepo.checkoutBranch(Constants.MASTER_BRANCH);
			throw new GitException("binary repository hasn't specified commit:"+targetCommit.getName()+". Exiting Zeus...");
		}
		
		binRepo.reset(binaryCommitHash);
		
		//copy classes from binary repo to source repo.
		copyClassesFromBinaryRepoToSourceRepo();
		
		tracker.stop();
		logger.info("setup is completed, takes " + tracker.getDurationString());
	}
	
	/**
	 * it will loop srcRepo's commits (From HEAD to previous)
	 * to find latest commit that contains java files changes.
	 * 
	 * @param srcRepo
	 * @return
	 * @throws Exception
	 */
	private RevCommit getJavaTargetCommit(SourceZeusRepository srcRepo) throws Exception {
		List<RevCommit> commits = srcRepo.getAllCommits(srcRepo.getBranch(), false);
		for (RevCommit commit:commits){
			List<DiffEntry> entries = srcRepo.getChangedFiles(commit);
			for (DiffEntry entry:entries){
				if (entry.getNewPath().endsWith(".java")){
					return commit;
				}
			}
		}
		
		return commits.get(0);
	}

	private void copyClassesFromBinaryRepoToSourceRepo() throws GitException{
		try {
			FileUtil.copyBinaryFolders(binRepo.getDirectory()
					.getParentFile(), srcRepo.getDirectory()
					.getParentFile(), Constants.DOT_GIT);
		} catch (IOException e) {
			throw new GitException(
					"Fail to copy binary repository's folders into source repository.",	e);
		}
	}
	
}
