package com.ebay.zeus.repository;

import java.io.IOException;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.Constants;
import com.ebay.zeus.utils.FileUtil;
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
		// TODO: download dependencies
		
		// read the source project
        // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
        // root = new File("D:\\dev\\devex\\binrepo-devex");
		BranchGraphEntry branchEntry = new BranchGraphEntry();
		branchEntry.setBranchName(srcRepo.getBranch());
		
		if (ZeusUtil.isBinaryRepositoryExisted(srcRepo)) {
			checkoutBinaryBranch(branchEntry);
			copyClassesFromBinaryRepoToSourceRepo();
			
			logger.info("setup is complete");
		} else if (ZeusUtil.isRemoteBinaryRepositoryExisted(srcRepo
				.getRemoteUrl())) {
			this.binRepo = ZeusUtil.cloneBinaryRepository(true, srcRepo);
			checkoutBinaryBranch(branchEntry);
			copyClassesFromBinaryRepoToSourceRepo();
			
			logger.info("setup is complete");
		} else {
			// TODO: anything we can do?
			logger.info("Binary repository not available. exiting...");
		}
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
