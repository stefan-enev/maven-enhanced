package com.ebay.zeus.repository;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ebay.zeus.exceptions.GitException;

public class SourceZeusRepositoryTest extends BaseZeusRepositoryTestCase{
	SourceZeusRepository repo; 
	
	@Before
	public void setup() throws Exception{
		super.setup();
		
		repo = new SourceZeusRepository(gitDir);
		git = repo.git;
	}
	
	@After
	public void tearDown(){
		super.tearDown();
	}
	
	@Test
	public void getNewCommits() throws GitException{
		repo.addRemoteUrl("git@github.scm.corp.ebay.com:yunfwang/zeus-test.git");
		repo.addRemoteBranch("master");

		//pull
		repo.pull();
		assertEquals("0066255c3e06f8ba2e61755033f4252f04f2fc1c", repo.getHead());
		
		List<RevCommit> allcommits = repo.getAllCommits();
		
		assertEquals("9ce0aaf2ffd10192ebac6122a07055e2910f49de", allcommits.get(1).getName());
		List<RevCommit> commits = repo.getNewCommits(allcommits.get(1));
		
		assertEquals(1, commits.size());
	}
}
