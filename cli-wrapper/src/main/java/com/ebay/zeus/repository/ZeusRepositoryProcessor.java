package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;

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
	protected boolean checkoutBinaryBranch(String branchName) throws GitException {
		logger.info("checking out binary repository's branch:"+branchName+"...");
		
		branchName = GitUtils.getShortBranchName(branchName);
		
		// check whether the branch exists
		boolean isBranchExisted = binRepo.isBranchExisted(branchName);
		if (isBranchExisted){
			binRepo.checkoutBranch(branchName);
			boolean firstCommit = binRepo.getHeadCommit(branchName).getFullMessage().equals(Constants.FIRST_COMMIT_MESSAGE);
			if (firstCommit ){
				return true;
			}
		}
		
		if( !isBranchExisted ){
			if (!getCurrentBranch(srcRepo).equals(branchName)){
				srcRepo.checkoutBranch(branchName);
			}
			
			binRepo.checkoutBranch(Constants.MASTER_BRANCH);
			
			//Then checkout new binary branch.
			binRepo.checkoutNewBranch(branchName);
			return true;
		}else{
			if( !getCurrentBranch(binRepo).equals(branchName) ){
				binRepo.checkoutBranch(branchName);
			}
		}
		
		return false;
	}
	
	protected boolean isNewBinaryBranch(String branchName) throws GitException{
		branchName = GitUtils.getShortBranchName(branchName);
		
		// check whether the branch exists
		boolean isBranchExisted = binRepo.isBranchExisted(branchName);
		if (isBranchExisted){
			return binRepo.getHeadCommit(branchName).getFullMessage().equals(Constants.FIRST_COMMIT_MESSAGE);
		}
		
		return !isBranchExisted;
	}
	
	private String getCurrentBranch(ZeusRepository repo) throws GitException{
		try {
			return repo.getBranch();
		} catch (IOException e) {
			throw new GitException(e);
		}
	}

	public String getBinaryStartCommitHash(String branch,
			String startCommitHash) throws GitException {
		
		if (startCommitHash == null){
			return null;
		}
		
		return binRepo.getBinaryCommit(branch, startCommitHash);
	}
	
	abstract public void process() throws Exception;
	
}
