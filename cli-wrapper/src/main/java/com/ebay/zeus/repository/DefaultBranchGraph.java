package com.ebay.zeus.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.Constants;

public class DefaultBranchGraph implements BranchGraph {
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private List<BranchGraphEntry> branchList = new ArrayList<BranchGraphEntry>();
	
	public void add(BranchGraphEntry entry){
		branchList.add(entry);
	}
	
	public void clear(){
		branchList.clear();
	}
	
	/**
	 * it will sort branches by level.
	 * Such as branch A is cut from master.
	 * so master should be lv 1, and branch A should be lv 2.
	 * Branches in the same level, the order doesn't matter.
	 * 
	 * @return
	 * @throws GitException 
	 */
	public List<BranchGraphEntry> getSortedBranches() throws GitException{
		List<BranchGraphEntry> orderedList = new ArrayList<BranchGraphEntry>();
		BranchGraphEntry rootEntry = getRootEntry(Constants.MASTER_BRANCH);
		if (rootEntry == null){
			throw new GitException("Haven't found root branch graph entry, must have it.");
		}
		
		orderedList.add(rootEntry);
		
		getToBranches(orderedList);
		
		return branchList;
	}
	
	private BranchGraphEntry getRootEntry(String fromBranchName){
		for (BranchGraphEntry entry:branchList){
			if (entry.getFromBranchName() == null){
				return entry;
			}
		}
		
		return null;
	}

	private List<BranchGraphEntry> getToBranches(List<BranchGraphEntry> orderedList) {
		List<BranchGraphEntry> subOrderedList = new ArrayList<BranchGraphEntry>();
		
		for (BranchGraphEntry orderedEntry:orderedList){
			String fromBranchName = orderedEntry.getBranchName();
			
			for (BranchGraphEntry entry:branchList){
				if (entry.getFromBranchName() == null){
					continue;
				}
				
				if (entry.getFromBranchName().equals(fromBranchName)){
					subOrderedList.add(entry);
				}
			}
			
			subOrderedList.addAll(getToBranches(subOrderedList));
		}
		
		if (subOrderedList.size() == 0){
			return Collections.emptyList();
		}
		
		orderedList.addAll(subOrderedList);
		
		return orderedList;
	}
}
