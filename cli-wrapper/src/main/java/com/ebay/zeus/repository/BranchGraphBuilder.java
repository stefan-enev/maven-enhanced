package com.ebay.zeus.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
		
		BranchGraphEntry entry = getFromBranch(branch, allCommits);
		if (entry == null){
			logger.info("Haven't found any cutting commit for branch:"+branch);
			return null;
		}
		
		logger.info("found branch graph entry:{" + branch + ", "
				+ entry.getStartCommit().getName() + ", " + entry.getFromBranchName() + "}");
		
		return entry;
	}

	/**
	 * find out which branch that specified commit come from.
	 * 
	 * @param commit
	 * @return branch name.
	 * @throws GitException 
	 */
	public BranchGraphEntry getFromBranch(String curBranch, List<RevCommit> commits) throws GitException{
		logger.debug("Trying to find cutting ref for branch:"+curBranch);
		
		try {
			Stack<BranchGraphEntry> entries = new Stack<BranchGraphEntry>();
			
			RevCommit prevCommit = commits.get(0);
			String prevBranch = Constants.MASTER_BRANCH;
			int prevBranchesNumber = 0;
			
			for (RevCommit commit:commits){
				List<String> branches = getBranches(commit.getName());
				
				if (branches.size() == 0){
					continue;
				}
				
				if (prevBranchesNumber != 0 && branches.size() != prevBranchesNumber){
					if (prevCommit == commits.get(0)){
						return null;
					}
					
					BranchGraphEntry entry = new BranchGraphEntry();
					entry.setStartCommit(prevCommit);
					entry.setFromBranchName(prevBranch);
					entry.setBranchName(curBranch);
					
					entries.push(entry);
				}
				
				prevCommit = commit;
				prevBranch = getOrientedBranch(branches, curBranch);
				prevBranchesNumber = branches.size();
			}
			
			if (entries.size() > 0){
				return entries.pop();
			}
			
			return null;
		} catch (Exception e) {
			throw new GitException("fail to get ref log.", e);
		}
	}
	
	private String getOrientedBranch(List<String> branches, String curBranch) {
		for (String branch:branches){
			if (branch.contains(Constants.MASTER_BRANCH)){
				return branch;
			}
			
			if (branch.contains(curBranch)){
				continue;
			}
		}
		
		return branches.get(0)!=null?branches.get(0):null;
	}

	/**
	 * get branches those contains specified commit.
	 * 
	 * @param commitHash
	 * @return short branch name list.
	 * @throws GitException
	 */
	public List<String> getBranches(String commitHash) throws GitException{
		List<String> branchNames = new ArrayList<String>();
		List<Ref> refs = getBranchRefs(commitHash);
		for (Ref ref:refs){
			branchNames.add(getBranch(ref.getName()));
		}
		
		return branchNames;
	}
	
	/**
	 * get branches those contains specified commit.
	 * it only get those remotes refs
	 * 
	 * @param commit
	 * @return refs
	 * @throws GitException
	 */
	private List<Ref> getBranchRefs(String commitHash) throws GitException{
		List<Ref> refs = new ArrayList<Ref>();
		
		try{
			RevWalk walk = new RevWalk(repo);
			RevCommit commit = walk.parseCommit(repo.resolve(commitHash + "^0"));
			for (Map.Entry<String, Ref> e : repo.getAllRefs().entrySet()){
				if (e.getKey().startsWith(org.eclipse.jgit.lib.Constants.R_REMOTES)){
					if (walk.isMergedInto(commit, walk.parseCommit(e.getValue().getObjectId()))){
						refs.add(e.getValue());
						
//						logger.debug("Ref " + e.getValue().getName()+ " contains commit:" + commitHash);
					}
				}
			}
			
			return refs;
		}catch(Exception e){
			throw new GitException("fail to get branches that contains specified commit.", e);
		}
		
	}
	
	/**
	 * get short branch name from full branch name.
	 * from "refs/remotes/origin/branch1" to "origin/branch1"
	 * 
	 * @param fullBranchName
	 * @return
	 * @throws GitException
	 */
	private String getBranch(String fullBranchName) throws GitException {
		if (fullBranchName != null)
			return repo.shortenRefName(fullBranchName);
		return fullBranchName;
	}
}
