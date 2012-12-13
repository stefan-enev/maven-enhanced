package com.ebay.zeus.repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

import com.ebay.zeus.utils.GitUtils;

public class BaseZeusRepositoryTestCase {
	File repoRoot = new File(System.getProperty("user.home"), "repotest");
	File gitDir = new File(repoRoot, ".git");
	String message1 = "commit1";
	String message2 = "commit2";
	String content1 = "blabla";
	String content2 = "blablaBIS";
	Git git;
	File myfile1 = new File(repoRoot, "file1.txt");
	File myfile2 = new File(repoRoot, "file2.txt");

	RevCommit commit1;
	RevCommit commit2;
	
	protected void setup() throws Exception{
		deleteDirectory(repoRoot);
		
		GitUtils.initRepository(repoRoot);
	}
	
	protected void tearDown(){
		deleteDirectory(repoRoot);
	}
	
	protected void addCommits() throws Exception{
		writeToFile(myfile1, "content1");
		commit1 = addAndCommit(git, "commit1", ".");
		
		writeToFile(myfile2, "content2");
		commit2 = addAndCommit(git, "commit2", ".");
	}
	
	protected RevCommit addAndCommit(Git git, String message, String pathToAdd) throws Exception {
		add(git, pathToAdd);
		return commit(git, message);
	}

	protected RevCommit commit(Git git, String message) throws Exception, GitAPIException {
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

	protected void add(Git git, String pathToAdd) throws Exception {
		AddCommand add = git.add();
		try {
			add.addFilepattern(pathToAdd).call();
		} catch (NoFilepatternException e) {
			throw new RuntimeException(e);
		}
	}

	protected void writeToFile(File myfile, String string) {
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

	protected void deleteDirectory(File dirPath) {
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
}
