package com.ebay.zeus.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ebay.zeus.exceptions.GitException;

public class BinaryZeusRepositoryTest extends BaseZeusRepositoryTestCase {
	BinaryZeusRepository repo;
	
	@Before
	public void setup() throws Exception{
		super.setup();
		
		repo = new BinaryZeusRepository(gitDir);
		git = repo.git;
	}
	
	@After
	public void tearDown(){
		super.tearDown();
	}
	
	@Test
	public void getName() throws Exception{
		addCommits();
		
		assertNull(repo.getName());
		
		repo.addRemoteUrl("git://test/test.git");
		assertEquals("test", repo.getName());
	}
	
	@Test
	public void getRemoteUrl() throws Exception{
		addCommits();
		
		assertNull(repo.getRemoteUrl());
		repo.addRemoteUrl("git://test/test.git");
		
		assertEquals("git://test/test.git", repo.getRemoteUrl());
	}
	
	@Test
	public void getHead() throws Exception{
		addCommits();
		
		assertEquals(commit2.getName(), repo.getHead());
	}
	
	@Test
	public void otherOperationsTest() throws GitException{
		repo.addRemoteUrl("git@github.scm.corp.ebay.com:yunfwang/zeus-test.git");
		repo.addRemoteBranch("master");

		//pull
		repo.pull();
		assertEquals("0066255c3e06f8ba2e61755033f4252f04f2fc1c", repo.getHead());
		
		//getAllBranches
		List<String> branchList = repo.getAllBranches();
		assertEquals(4, branchList.size());
		
		//checkoutBranch
		repo.checkoutBranch("origin/branch2");
		List<RevCommit> commits = repo.getAllCommits();
		
		assertEquals(3, commits.size());
		
		//checkoutRemoteBranch
		repo.checkoutRemoteBranch("branch1");
		commits = repo.getAllCommits();
		assertEquals(3, commits.size());
		
		//commitNDPushAll
		writeToFile(myfile1, "add file");
		RevCommit commit = repo.commitNDPushAll("add new file");
		assertNotNull(commit);
		
		//rollback repo
		repo.reset("942689875268003992811f1612134484f7b194d8");
		repo.push(true);
	}
	
}
