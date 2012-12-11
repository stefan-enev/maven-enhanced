package com.ebay.zeus.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.utils.GitUtils;

public class ZeusRepositoryTest {

	File repoRoot = new File(System.getProperty("user.home"), "repotest");
	File gitDir = new File(repoRoot, ".git");
	String message1 = "commit1";
	String message2 = "commit2";
	String content1 = "blabla";
	String content2 = "blablaBIS";
	Git git;
	File myfile1 = new File(repoRoot, "file1.txt");
	File myfile2 = new File(repoRoot, "file2.txt");
	BinaryZeusRepository repo;
	RevCommit commit1;
	RevCommit commit2;
	
	@Before
	public void setup() throws Exception{
		deleteDirectory(repoRoot);
		
		GitUtils.initRepository(repoRoot);
		repo = new BinaryZeusRepository(gitDir);
		git = repo.git;
	}
	
	@After
	public void tearDown(){
		deleteDirectory(repoRoot);
	}
	
	private void addCommits() throws Exception{
		writeToFile(myfile1, "content1");
		commit1 = addAndCommit(git, "commit1", ".");
		
		writeToFile(myfile2, "content2");
		commit2 = addAndCommit(git, "commit2", ".");
	}
	
	private RevCommit addAndCommit(Git git, String message, String pathToAdd) throws Exception {
		add(git, pathToAdd);
		return commit(git, message);
	}

	private RevCommit commit(Git git, String message) throws Exception, GitAPIException {
		CommitCommand commit = git.commit();
		try {
			return commit.setMessage(message).call();
		} catch (NoHeadException e) {
			throw new RuntimeException(e);
		} catch (NoMessageException e) {
			throw new RuntimeException(e);
		} catch (ConcurrentRefUpdateException e) {
			throw new RuntimeException(e);
		} catch (JGitInternalException e) {
			throw new RuntimeException(e);
		} catch (WrongRepositoryStateException e) {
			throw new RuntimeException(e);
		}
	}

	private void add(Git git, String pathToAdd) throws Exception {
		AddCommand add = git.add();
		try {
			add.addFilepattern(pathToAdd).call();
		} catch (NoFilepatternException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeToFile(File myfile, String string) {
		FileWriter writer;
		try {
			if (!myfile.exists()){
				myfile.getParentFile().mkdirs();
				myfile.createNewFile();
			}
			writer = new FileWriter(myfile);
			writer.write(string);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void deleteDirectory(File dirPath) {
		if (!dirPath.exists()) {
			return;
		}

		for (String filePath : dirPath.list()) {
			File file = new File(dirPath, filePath);
			if (file.isDirectory())
				deleteDirectory(file);
			file.delete();
		}
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
