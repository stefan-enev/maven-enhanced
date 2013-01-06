package com.ebay.zeus.repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.IO;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.exceptions.ProcessException;
import com.ebay.zeus.utils.Constants;
import com.ebay.zeus.utils.FileUtil;
import com.ebay.zeus.utils.MavenUtil;
import com.ebay.zeus.utils.ProjectEntry;
import com.ebay.zeus.utils.ZeusUtil;

/**
 * it major in "create/update" binary repository according to source repository's changes.
 * 
 * @author yunfwang@ebay.com
 *
 */
public class BinaryRepositoryProcessor extends ZeusRepositoryProcessor{
//	private BranchGraphBuilder branchGrapthBuilder;
	
	private List<String> notNeedProcessedCommits = new ArrayList<String>();
	
	public BinaryRepositoryProcessor(SourceZeusRepository srcRepo, BinaryZeusRepository binRepo){
		super(srcRepo, binRepo);
		
//		this.branchGrapthBuilder = new BranchGraphBuilder(srcRepo);
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

		List<String> activeBranches = ZeusUtil.getActiveBranches(srcRepo.getRemoteUrl());
		
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
		
//		BranchGraph branchGraph = branchGrapthBuilder.build();
//		List<BranchGraphEntry> branchGraphEntries = branchGraph.getSortedBranches();
//		
//		for (BranchGraphEntry branchEntry:branchGraphEntries){
//			processBranch(branchEntry);
//		}
		
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
	private void processBranch(String branchName) {
		logger.info("updating binary repository's branch:"+branchName);
		
//		String branchName = branchEntry.getBranchName();
		
		try {
			srcRepo.checkoutBranch(branchName);
			
			boolean newBinBranch = checkoutBinaryBranch(branchName);
			
			if (newBinBranch){
				RevCommit headCommit = srcRepo.getHeadCommit();
				if (needProcess(headCommit)){
	        		processNewBranchCommit(headCommit);
	        	}else{
	        		logger.debug("needn't handle this commit:"+headCommit.getName()+"--"+headCommit.getFullMessage());
	        	}
				
				return;
			}
			
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
	 * process commit for new created binary branch.
	 * Only take care of head commit.
	 * 
	 * @param commit
	 * @throws GitException
	 */
	private void processNewBranchCommit(RevCommit commit) throws GitException {
		logger.info("creating binary repository with commit:"+commit.getFullMessage());
		
		try {
			srcRepo.reset(commit.getName());

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
		} finally {
			// rollback
			srcRepo.reset();
			srcRepo.pull();
			binRepo.reset();
		}

		logger.info("created binary repository with commit:" + commit.getFullMessage());
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
		List<File> firstCommitChangedFiles = srcRepo.getFirstCommitChangedFiles(commit);
		
		List<DiffEntry> srcChangedFiles = srcRepo.getChangedFiles(commit);
		if (firstCommitChangedFiles.size()==0 
				&& srcChangedFiles.size() == 0){
			return;
		}
		
		if (srcChangedFiles.size() != 0){
			processChangedFiles(srcRepoRoot, srcChangedFiles, binRepo.getDirectory().getParentFile());
		}else{
			FileUtil.copyOutputFiles(srcRepoRoot, firstCommitChangedFiles, binRepo.getDirectory().getParentFile());
		}
		
	}
	
	/**
	 * process changed files (ADD/MODIFY/DELETE/RENAME/COPY)
	 * 
	 * @param srcRepoRoot
	 * @param srcChangedFiles
	 * @param parentFile
	 */
	private void processChangedFiles(File srcRepoRoot,	List<DiffEntry> srcChangedFiles, File parentFile) {
		List<File> addedFiles = new ArrayList<File>();
		List<String> deletedFiles = new ArrayList<String>();
		Map<String, String> renamedFiles = new HashMap<String, String>();
		Map<String, String> copiedFiles = new HashMap<String, String>();
		
		for (DiffEntry entry : srcChangedFiles) {
			ChangeType type = entry.getChangeType();
			switch (type) {
			case ADD:
				addedFiles.add(new File(srcRepoRoot, entry.getNewPath()));
				break;
			case MODIFY:
				addedFiles.add(new File(srcRepoRoot, entry.getNewPath()));
				break;
			case DELETE:
				deletedFiles.add(entry.getOldPath());
				break;
			case RENAME:
				renamedFiles.put(entry.getOldPath(), entry.getNewPath());
				break;
			case COPY:
				copiedFiles.put(entry.getOldPath(), entry.getNewPath());
			}
		}
		
		processAddedFiles(addedFiles);
		processDeletedFiles(deletedFiles);
		processRenamedFiles(renamedFiles);
		processCopiedFiles(copiedFiles);
	}

	private void processCopiedFiles(Map<String, String> copiedFiles) {
		for (Map.Entry<String, String> fileEntry:copiedFiles.entrySet()){
			File oldFile = getBinaryTargetFile(fileEntry.getKey());
			if (oldFile!=null && oldFile.exists()){
				File newFile = getBinaryTargetFile(fileEntry.getValue());
				
				byte[] bs;
				try {
					bs = IO.readFully(oldFile);
					FileWriter fw = new FileWriter(newFile,	true);
					fw.write(new String(bs));
					fw.close();
				} catch (Exception e) {
					logger.error("fail to copy file from "+ oldFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
				}
				
			}
		}
	}

	private void processRenamedFiles(Map<String, String> renamedFiles) {
		for (Map.Entry<String, String> fileEntry:renamedFiles.entrySet()){
			File oldFile = getBinaryTargetFile(fileEntry.getKey());
			if (oldFile!=null && oldFile.exists()){
				File newFile = getBinaryTargetFile(fileEntry.getValue());
				oldFile.renameTo(newFile);
			}
		}
	}

	/**
	 * delete binary repo's target files according to source repo's deleted source files.
	 * 
	 * @param deletedFiles
	 */
	private void processDeletedFiles(List<String> deletedFiles) {
		
		for (String filePath:deletedFiles){
			File targetFile = getBinaryTargetFile(filePath);
			if (targetFile!=null && targetFile.exists()){
				targetFile.delete();
			}
		}
		
	}

	private List<ProjectEntry> projectEntries = null;
	
	/**
	 * get binary repo's target file by source repo's deleted file path.
	 * 
	 * @param filePath
	 * @return
	 */
	private File getBinaryTargetFile(String filePath) {
		File srcFile = new File(srcRepoRoot, filePath);
		if (projectEntries == null){
			projectEntries = FileUtil.getProjectEntries(srcRepoRoot);
		}
		
		File srcTargetFile = FileUtil.getTargetFile(srcFile, projectEntries);
		String targetFilePath = getRelativePath(srcTargetFile);
		return new File(binRepo.getDirectory().getParentFile(), targetFilePath);
	}
	
	private String getRelativePath(File file){
		String srcRepoRootPath = srcRepoRoot.getAbsolutePath();
		String filePath = file.getAbsolutePath();
		if (filePath.startsWith(srcRepoRootPath)){
			return filePath.substring(srcRepoRootPath.length());
		}
		
		return null;
	}

	/**
	 * for ADD/MODIFIED files, copy their target files simply.
	 * 
	 * @param addedFiles
	 */
	private void processAddedFiles(List<File> addedFiles) {
		FileUtil.copyOutputFiles(srcRepoRoot, addedFiles, binRepo.getDirectory().getParentFile());
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
