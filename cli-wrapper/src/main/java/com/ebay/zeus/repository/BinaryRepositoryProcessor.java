package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.exceptions.ProcessException;
import com.ebay.zeus.utils.Constants;
import com.ebay.zeus.utils.FileUtil;
import com.ebay.zeus.utils.MavenUtil;
import com.ebay.zeus.utils.ZeusUtil;

/**
 * it major in "create/update" binary repository according to source repository's changes.
 * 
 * @author yunfwang@ebay.com
 *
 */
public class BinaryRepositoryProcessor extends ZeusRepositoryProcessor{
	
	private List<String> notNeedProcessedCommits = new ArrayList<String>();
	
	public BinaryRepositoryProcessor(SourceZeusRepository srcRepo, BinaryZeusRepository binRepo){
		super(srcRepo, binRepo);
	}
	
	/**
	 * process source repo's changes(branches & commits) and update binary repo.
	 * @throws GitException 
	 */
	public void process() throws Exception{
		logger.info("updating binary repository...");
    	
        final File binRepoRoot = ZeusUtil.getExistedBinaryRepositoryRoot(srcRepoRoot);
        
		logger.info("SourceRepository = " + srcRepoRoot.getCanonicalPath()
					+ "\nBinaryRepository = " + binRepoRoot.getCanonicalPath());
		
		notNeedProcessedCommits.clear();

		List<String> activeBranches = ZeusUtil.getActiveBranches(srcRepo, binRepoRoot);
		
		if (activeBranches.size() == 0){
			logger.warn("haven't found any active branches, do nothing.");
			return;
		}
		
		//TODO: if binary branch isn't in active branch list, should retire it.
		
		//TODO: for candidate branches, if one branch is new, only process its HEAD commit.
		//      won't care branch's relations
		for (String branch:activeBranches){
			processBranch(branch);
		}
		
		binRepo.push(false);
		srcRepo.checkoutBranch(Constants.MASTER_BRANCH);
		logger.debug("commit/pushed changes onto remote binary repo:"
				+ binRepo.getRemoteUrl());
		logger.info("Binary repository updated.");
	}

	public boolean isBlackListChanged() throws GitException {
		List<String> changedFiles = binRepo.getChangedFiles();
		for (String file:changedFiles){
			if (file.contains(".blacklist")){
				return true;
			}
		}
		
		return false;
	}

	/**
	 * if one commit has been processed, and prove to not need to be processed.
	 * then ignore this commit. 
	 * 
	 * @param commit
	 * @return
	 */
	private boolean needProcess(RevCommit commit) {
		return !notNeedProcessedCommits.contains(commit.getName());
	}
	
	 /**
     * update specified branch by its source repo's compiled classes
     * 
     * @param branch
     */
	private void processBranch(String branchName) {
		logger.info("updating binary repository's branch:"+branchName);
		
		try {
			if (isNewBinaryBranch(branchName)){
				checkoutBinaryBranch(branchName);
				RevCommit headCommit = srcRepo.getHeadCommit(branchName);
				if (needProcess(headCommit)){
	        		processNewBranchCommit(headCommit);
	        	}else{
	        		logger.debug("needn't handle this commit:"+headCommit.getName()+"--"+headCommit.getFullMessage());
	        	}
				
				return;
			}
			
	        RevCommit headCommit = binRepo.getHeadCommit(branchName);
	        List<RevCommit> newCommits = srcRepo.getNewCommits(branchName, headCommit);
	        if (newCommits.size()==0){
				logger.debug("There isn't any new commits in source repository by binary repository's 'since' commit:"
						+ headCommit.getName());
	        }else{
	        	srcRepo.checkoutBranch(branchName);
	        	checkoutBinaryBranch(branchName);
		        
		        for (RevCommit commit:newCommits){
		        	if (needProcess(commit)){
		        		processCommit(branchName, commit);
		        	}else{
		        		logger.debug("needn't handle this commit:"+commit.getName()+"--"+commit.getFullMessage());
		        	}
		        }
		        
		        //rollback to "master"
	        	srcRepo.checkoutBranch(Constants.MASTER_BRANCH);
	        	binRepo.checkoutBranch(Constants.MASTER_BRANCH);
	        }
	       
		} catch (Exception e) {
			//shouldn't break others branch.
			logger.error(e.getMessage(), e);
		}
        
        logger.info("updated binary repository's branch:"+branchName);
	}

	/**
	 * process commit for new created binary branch.
	 * Only take care of head commit.
	 * 
	 * @param commit
	 * @throws GitException
	 */
	private void processNewBranchCommit(RevCommit commit) throws GitException {
		logger.info("creating binary repository with commit:"+commit.getFullMessage());
		
		try {
			srcRepo.checkout(commit.getName());

			if (compile()) {
				copyTargetFolderFromSrcRepoToBinRepo();
				binRepo.commitNDPushAll(commit.getName());

			} else {
				logger.debug("No pom file found in directory"
						+ srcRepo.getDirectory().getParent());
				notNeedProcessedCommits.add(commit.getName());
			}

		} catch (Exception e) {
			// shouldn't break others commits
			logger.error(e.getMessage(), e);
			notNeedProcessedCommits.add(commit.getName());
		}

		logger.info("created binary repository with commit:" + commit.getFullMessage());
	}

	/**
	 * update binary repo with specified commit
	 * 
	 * @param commit
	 * @throws GitException
	 */
	private void processCommit(String branch, RevCommit commit) throws GitException {
		logger.info("updating binary repository with commit:"+commit.getFullMessage());
		
    	if (!binRepo.hasCommit(branch, commit.getName())){
        	try {
        		srcRepo.checkout(commit.getName());
        		
        		if (compile()){
        			copyTargetFolderFromSrcRepoToBinRepo();
    				
    				List<String> binChangedFiles = binRepo.getChangedFiles();
    				
    				if (binChangedFiles.size() > 0){
    					binRepo.commitNDPushAll(commit.getName());
    				}else{
    					logger.debug("Haven't found any changed files, needn't commit/push.");
    					notNeedProcessedCommits.add(commit.getName());
    				}
    				
        		}else{
        			logger.debug("No pom file found in directory"+srcRepo.getDirectory().getParent());
        			notNeedProcessedCommits.add(commit.getName());
        		}
        		
			} catch (Exception e) {
				//shouldn't break others commits
				logger.error(e.getMessage(), e);
				notNeedProcessedCommits.add(commit.getName());
			}
        }
    	
    	logger.info("updated binary repository with commit:"+commit.getFullMessage());
	}
	
	/**
	 * copy classes from source repo to binary repo, exclude "localobr"
	 * 
	 * @param binRepoRoot
	 * @throws IOException
	 */
	private void copyTargetFolderFromSrcRepoToBinRepo()
			throws IOException {
		// find the "localobr" folders and exclude them during copy
		List<String> excludes = FileUtil.findExcludes(srcRepoRoot, "localobr");

		// copy the classes
		final File binRepoRoot = ZeusUtil
				.getExistedBinaryRepositoryRoot(srcRepoRoot);

		logger.info("copying binary files");
		FileUtil.copyBinaryFolders("target", excludes, srcRepoRoot, binRepoRoot);
	}
    
	/**
	 * execute "mvn compile" in root folder.
	 * if there aren't pom file in root folder, won't execute "mvn compile"
	 * 
	 * @return true:executed compile; false:hasn't executed compile.
	 * @throws ProcessException
	 */
	private boolean compile() throws ProcessException {
		File pomFile = new File(srcRepoRoot, "pom.xml");
		if (pomFile.exists()){
			MavenUtil.executeMvnCommand("clean compile", srcRepoRoot, System.out);
			
			return true;
		}
		
		return false;
	}
	
}
