package com.ebay.zeus.repository;

import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.CheckoutEntry;
import org.eclipse.jgit.storage.file.ReflogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.Constants;

public class BranchGraphBuilder {
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ZeusRepository repo;
	
	public BranchGraphBuilder(ZeusRepository repo){
		this.repo = repo;
	}
	
	/**
	 * loop repo's branches and commits to build out branch graph.
	 * 
	 * @return
	 * @throws GitException 
	 */
	public BranchGraph build() throws GitException{
		logger.info("building branch graph for repository:"+this.repo.getDirectory().getParent());
		
		List<String> branches = repo.getAllBranches();
		if (branches.size() == 0){
			logger.debug("Haven't found any branches in repository:"
					+ this.repo.getDirectory().getParent());
			return new EmptyBranchGraph();
		}
		
		BranchGraph branchGraph = new DefaultBranchGraph();
		
		//start from master branch.
		BranchGraphEntry entry = new BranchGraphEntry();
		entry.setBranchName(Constants.MASTER_BRANCH);
		entry.setFromBranchName(null);
		entry.setStartCommit(null);
		
		branchGraph.add(entry);
		
		for (String branch:branches){
			if (!branch.contains(Constants.MASTER_BRANCH)){
				entry = buildBranchGraphEntry(branch);
				if (entry != null){
					branchGraph.add(entry);
				}
			}
		}
		
		logger.info("Completed branch graph build for repository:"+this.repo.getDirectory().getParent());
		
		return branchGraph;
	}
	
	private BranchGraphEntry buildBranchGraphEntry(String branch) throws GitException {
		logger.info("building branch graph entry for branch:"+branch);
		
		repo.checkoutBranch(branch);
		
		List<RevCommit> allCommits = repo.getAllCommits();
		for (RevCommit commit:allCommits){
			String fromBranch = getFromBranch(commit);
			if (fromBranch != null){
				BranchGraphEntry entry = new BranchGraphEntry();
				entry.setBranchName(branch);
				entry.setFromBranchName(fromBranch);
				entry.setStartCommit(commit);
				
				logger.info("found branch graph entry:{" + branch + ", "
						+ commit.getName() + ", " + fromBranch + "}");
				
				return entry;
			}
		}
		
		logger.info("Haven't found any cutting commit for branch:"+branch);
		
		return null;
	}

	/**
	 * find out which branch that specified commit come from.
	 * 
	 * @param commit
	 * @return branch name.
	 * @throws GitException 
	 */
	public String getFromBranch(RevCommit commit) throws GitException{
		logger.debug("Trying to find cutting ref for commit:"+commit.getName());
		
		try {
			Collection<ReflogEntry> entries = repo.getGit().reflog().call();
			for (ReflogEntry entry:entries){
				if (!entry.getOldId().getName().equals(commit.getName())){
					continue;
				}
				
				CheckoutEntry checkOutEntry = entry.parseCheckout();
				if (checkOutEntry != null){
					
					logger.debug("found cutting ref, from branch:"+checkOutEntry.getFromBranch());
					return checkOutEntry.getFromBranch();
				}
			}
			
			return null;
		} catch (Exception e) {
			throw new GitException("fail to get ref log.", e);
		}
	}
}
