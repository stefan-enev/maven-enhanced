package com.ebay.zeus.repository;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ebay.zeus.exceptions.GitException;

public class BranchGraphBuilderTest extends BaseZeusRepositoryTestCase{
	SourceZeusRepository repo;
	
	@Before
	public void setup() throws Exception{
		super.setup();
		
		repo = new SourceZeusRepository(gitDir);
		git = repo.getGit();
	}
	
	@After
	public void tearDown(){
		super.tearDown();
	}
	
	@Test
	public void build() throws GitException{
		repo.addRemoteUrl("git@github.scm.corp.ebay.com:yunfwang/zeus-test.git");
		repo.addRemoteBranch("master");

		//pull
		repo.pull();
		
		BranchGraphBuilder builder = new BranchGraphBuilder(repo);
		
		BranchGraph graph = builder.build();
		assertTrue(graph.getSortedBranches().size()>0);
	}
}
