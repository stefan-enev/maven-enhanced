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
	private BranchGraphBuilder branchGrapthBuilder;
	
	private List<String> notNeedProcessedCommits = new ArrayList<String>();
	
	public BinaryRepositoryProcessor(SourceZeusRepository srcRepo, BinaryZeusRepository binRepo){
		super(srcRepo, binRepo);
		
		this.branchGrapthBuilder = new BranchGraphBuilder(srcRepo);
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
		
		BranchGraph branchGraph = branchGrapthBuilder.build();
		List<BranchGraphEntry> branchGraphEntries = branchGraph.getSortedBranches();
		
		for (BranchGraphEntry branchEntry:branchGraphEntries){
			processBranch(branchEntry);
		}
		
		binRepo.push(false);
		
		logger.debug("commit/pushed changes onto remote binary repo:"
				+ binRepo.getRemoteUrl());
		logger.info("Binary repository updated.");
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
	private void processBranch(BranchGraphEntry branchEntry) {
		logger.info("updating binary repository's branch:"+branchEntry.getBranchName());
		
		String branchName = branchEntry.getBranchName();
		
		try {
			srcRepo.checkoutBranch(branchName);
			
			checkoutBinaryBranch(branchName);
	        RevCommit headCommit = binRepo.getHeadCommit();
	        
	        List<RevCommit> newCommits = srcRepo.getNewCommits(headCommit);
	        
	        if (newCommits.size()==0){
				logger.debug("There isn't any new commits in source repository by binary repository's 'since' commit:"
						+ headCommit.getName());
	        }
	        
	        for (RevCommit commit:newCommits){
	        	if (needProcess(commit)){
	        		processCommit(commit);
	        	}else{
	        		logger.debug("needn't handle this commit:"+commit.getName()+"--"+commit.getFullMessage());
	        	}
	        }
	        
	        //rollback to "master"
        	srcRepo.checkoutBranch(Constants.MASTER_BRANCH);
        	binRepo.checkoutBranch(Constants.MASTER_BRANCH);
		} catch (Exception e) {
			//shouldn't break others branch.
			logger.error(e.getMessage(), e);
		}
        
        logger.info("updated binary repository's branch:"+branchName);
	}

	/**
	 * update binary repo with specified commit
	 * 
	 * @param commit
	 * @throws GitException
	 */
	private void processCommit(RevCommit commit) throws GitException {
		logger.info("updating binary repository with commit:"+commit.getFullMessage());
		
    	if (!binRepo.hasCommit(commit.getName())){
        	try {
        		srcRepo.reset(commit.getName());
        		
        		if (compile()){
        			
        			if (binRepo.isBare()){
        				copyTargetFolderFromSrcRepoToBinRepo();
        			}else{
        				copyChangedOutputsFromSrcRepoToBinRepo(commit);
        			}
    				
    				List<String> binChangedFiles = binRepo.getChangedFiles();
    				
    				//TODO: only take care UNTRACKED/MODIFIED cases, ignore DELETE or RENAME cases.
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
			}finally{
				//rollback
	        	srcRepo.reset();
	        	srcRepo.pull();
	        	binRepo.reset();
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
	 * only copy those output files according to changed source files.
	 * 
	 * @param commit
	 * @throws GitException
	 */
	private void copyChangedOutputsFromSrcRepoToBinRepo(RevCommit commit) throws GitException {
		List<File> srcChangedFiles = srcRepo.getChangedFiles(commit);
		if (srcChangedFiles.size() == 0){
			return;
		}
		
		FileUtil.copyOutputFiles(srcRepoRoot, srcChangedFiles, binRepo.getDirectory().getParentFile());
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
			MavenUtil.executeMvnCommand("compile", srcRepoRoot, System.out);
			
			return true;
		}
		
		return false;
	}
	
}
