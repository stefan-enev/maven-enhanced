package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.Constants;
import com.ebay.zeus.utils.GitUtils;

public abstract class ZeusRepositoryProcessor {
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected SourceZeusRepository srcRepo;
	protected BinaryZeusRepository binRepo;
	protected File srcRepoRoot;
	
	protected ZeusRepositoryProcessor(SourceZeusRepository srcRepo, BinaryZeusRepository binRepo){
		this.srcRepo = srcRepo;
		this.binRepo = binRepo;
		
		this.srcRepoRoot = srcRepo.getDirectory().getParentFile();
	}
	
	/**
	 * checkout specified branch for binary repository.
	 * it try to make binary repository ready for next step(like copy classes and push).
	 * if remote branch not existed, create it.
	 * 
	 * @param branchName
	 * @throws GitException
	 */
	protected void checkoutBinaryBranch(BranchGraphEntry branchEntry) throws GitException {
		String branchName = branchEntry.getBranchName();
		
		logger.info("checking out binary repository's branch:"+branchName+"...");
		
		branchName = GitUtils.getShortBranchName(branchName);
		
		if (branchName.toLowerCase().equals(Constants.MASTER_BRANCH)) {
			return;
		}
		
		// check whether the branch exists
		boolean isBranchExisted = binRepo.isBranchExisted(branchName);
		
		if( !isBranchExisted ){
			//checkout 'from' branch and reset to start commit hash first
			String fromBranchName = branchEntry.getFromBranchName();
			if (!binRepo.isBranchExisted(fromBranchName)){
				throw new GitException("Trying to checkout new binary branch:"
						+ branchName + ", but found its 'from' branch"
						+ fromBranchName + " doesn't existed.");
			}
			
			fromBranchName = GitUtils.getShortBranchName(fromBranchName);
			binRepo.checkoutBranch(fromBranchName);
			
			RevCommit startCommit = branchEntry.getStartCommit();
			String binaryStartCommitHash = getBinaryStartCommitHash(
					fromBranchName, startCommit.getName());
			binRepo.reset(binaryStartCommitHash);
			
			if (!getCurrentBranch(srcRepo).equals(branchName)){
				srcRepo.checkoutBranch(branchName);
			}
			
			//Then checkout new binary branch.
			binRepo.checkoutNewBranch(branchName);
		}else{
			if( !getCurrentBranch(binRepo).equals(branchName) ){
				binRepo.checkoutBranch(branchName);
			}
		}
	}
	
	private String getCurrentBranch(ZeusRepository repo) throws GitException{
		try {
			return repo.getBranch();
		} catch (IOException e) {
			throw new GitException(e);
		}
	}

	public String getBinaryStartCommitHash(String fromBranchName,
			String startCommitHash) throws GitException {
		String binaryStartCommitHash = binRepo.getBinaryCommit(startCommitHash);
		if (binaryStartCommitHash == null){
			//find previous commit for start commit.
			srcRepo.checkoutBranch(fromBranchName);
//			srcRepo.reset(startCommitHash);
			
			String prevCommit = srcRepo.getPreviousCommit(startCommitHash);
			
			return getBinaryStartCommitHash(fromBranchName, prevCommit);
		}
		
		return binaryStartCommitHash;
	}
	
	abstract public void process() throws Exception;
	
}
