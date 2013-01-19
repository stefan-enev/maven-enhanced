package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;

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
		
		// read the source project
        // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
        // root = new File("D:\\dev\\devex\\binrepo-devex");
		
		if (!ZeusUtil.isRemoteBinaryRepositoryExisted(srcRepo.getRemoteUrl())) {
			throw new GitException("Remote binary repository not available("+srcRepo.getRemoteUrl()+"). exiting...");
		}
		
		if (ZeusUtil.targetClassesExisted(srcRepoRoot)){
			throw new GitException("Have existed classes, needn't Zeus. Existing...");
		}
		
		if (ZeusUtil.haveLocalChangedJavaFiles(srcRepo)){
			throw new GitException("Have local changed java files, needn't Zeus. Existing...");
		}
		
		if (ZeusUtil.isLocalBinaryRepositoryExisted(srcRepoRoot)) {
			File binaryRepoRoot= ZeusUtil.getExistedBinaryRepositoryRoot(srcRepoRoot);
			File binGit = new File(binaryRepoRoot, Constants.DOT_GIT);		
			binRepo = new BinaryZeusRepository(binGit);
			binRepo.pull();
		}else{
			RevCommit headCommit = srcRepo.getHeadCommit(srcRepo.getBranch());
			String binRepoRemoteUrl = ZeusUtil.getBinaryRemoteUrl(true, srcRepo);
			boolean existed = ZeusUtil.isExistedBranchCommit(binRepoRemoteUrl,
					srcRepo.getBranch(), headCommit.getName());
			
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
		RevCommit headCommit = srcRepo.getHeadCommit(srcRepo.getBranch());
		String binaryCommitHash = this.getBinaryStartCommitHash(
				srcRepo.getBranch(), headCommit.getName());
		
		if (binaryCommitHash == null){
			binRepo.checkoutBranch(Constants.MASTER_BRANCH);
			throw new GitException("binary repository hasn't specified commit:"+headCommit.getName()+". Exiting Zeus...");
		}
		
		binRepo.reset(binaryCommitHash);
		
		//copy classes from binary repo to source repo.
		copyClassesFromBinaryRepoToSourceRepo();
		
		tracker.stop();
		logger.info("setup is completed, takes " + tracker.getDurationString());
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
