package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

import com.ebay.zeus.exceptions.GitException;

public class SourceZeusRepository extends ZeusRepository {

	public SourceZeusRepository(File gitDir) throws IOException {
		super(gitDir);
	}

	//TODO
	public void processNewCommits(){
		
	}
	
	/**
	 * get new commits for current branch according to 'since' commit hash.
	 * 1. find all commits for current branch.
	 * 2. locate 'since' commit hash in #1's commit list.
	 * 3. could locate 'since' commit, get new commit list.
	 * 4. couldn't locate it, return empty list. it means binary commits are newer than source repo.
	 * 
	 * Normally, #4 should only happen in 'setup' mode.
	 * #1 ~ #3 should happen in 'create/update' mode.
	 * 
	 * @param sinceCommitHash
	 * @throws GitException 
	 */
	public List<RevCommit> getNewCommits(String sinceCommitHash) throws GitException{
		List<RevCommit> allCommits = getAllCommits();
		
		if (sinceCommitHash==null || "".equals(sinceCommitHash)){
			throw new GitException("'since' commit hash can't be empty.");
		}
		
		int idx = 0;
		
		for (int i=0; i<allCommits.size(); i++){
			RevCommit curCommit = allCommits.get(i);
			if (sinceCommitHash.equals(curCommit.getName())){
				idx = i;
			}
		}
		
		if (idx != 0){
			return allCommits.subList(0, idx);
		}
		
		return Collections.emptyList();
	}
}
