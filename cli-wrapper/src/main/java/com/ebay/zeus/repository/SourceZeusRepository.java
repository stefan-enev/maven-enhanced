package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.Constants;

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
	public List<RevCommit> getNewCommits(RevCommit sinceCommit) throws GitException{
		List<RevCommit> allCommits = getAllCommits();
		
		if (sinceCommit==null || "".equals(sinceCommit.getName())){
			throw new GitException("'since' commit hash can't be empty.");
		}

		//if binary repo is bare, then return all commits.
		if (sinceCommit.getFullMessage().equals(Constants.FIRST_COMMIT_MESSAGE)){
			
			if (allCommits.size() > Constants.COMMIT_MAX_NUMBER){
				return allCommits.subList(0, Constants.COMMIT_MAX_NUMBER); 
			}
			
			return allCommits;
		}
		
		String sinceCommitHash = sinceCommit.getFullMessage();

		int idx = 0;
		for (int i=0; i<allCommits.size(); i++){
			RevCommit curCommit = allCommits.get(i);
			if (sinceCommitHash.equals(curCommit.getName())){
				idx = i;
				break;
			}
		}
		
		if (idx != 0){
			return allCommits.subList(idx+1, allCommits.size());
		}
		
		return Collections.emptyList();
	}
}
