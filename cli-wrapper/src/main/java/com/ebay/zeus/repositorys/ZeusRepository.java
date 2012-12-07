package com.ebay.zeus.repositorys;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;

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

	public ZeusRepository(File gitDir) throws IOException {
		super(gitDir);
	}
	
	//TODO:
	public void pull(){
		
	}
	
	//TODO
	public String getBranch(){
		return null;
	}
	
	//TODO
	public String getHead(){
		return null;
	}
	
	//TODO
	public List<String> getAllBranches(){
		return Collections.emptyList();
	}
	
	//TODO
	public List<File> getChangedFiles(String commitHash){
		return Collections.emptyList();
	}
	
	//TODO
	public void checkoutBranch(String branchName){
		
	}
	
	//TODO
	public void checkoutCommit(String commitHash){
		
	}
	
	//TODO
	public List<RevCommit> getNewCommits(String branchName){
		return Collections.emptyList();
	}
	
}
