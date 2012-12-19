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
	protected void checkoutBinaryBranch(String branchName) throws GitException {
		logger.info("checking out binary repository's branch:"+branchName+"...");
		
		branchName = GitUtils.getShortBranchName(branchName);
		
		if (branchName.toLowerCase().equals(Constants.MASTER_BRANCH)) {
			return;
		}
		
		// check whether the branch exists
		boolean isBranchExisted = binRepo.isBranchExisted(branchName);
		
		if( !isBranchExisted ){
			binRepo.checkoutNewBranch(branchName);
		}else{
			// check the current branch in binary repository
			try {
				if( !binRepo.getBranch().equals(branchName) ){
					binRepo.checkoutBranch(branchName);
				}
			} catch (IOException e) {
				throw new GitException("can't checkout remote branch("+branchName+") into local.", e);
			}
		}
	}
	
	abstract public void process() throws Exception;
	
}
