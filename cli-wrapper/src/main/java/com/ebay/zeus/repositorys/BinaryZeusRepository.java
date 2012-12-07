package com.ebay.zeus.repositorys;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.revwalk.RevCommit;

import com.ebay.zeus.exceptions.GitException;

public class BinaryZeusRepository extends ZeusRepository{

	public BinaryZeusRepository(File gitDir) throws IOException {
		super(gitDir);
	}

	/**
	 * Same to "git add . -u"
	 * Add all changes into staging area include removed files
	 * 
	 * @throws GitException
	 */
	public void addAll() throws GitException{
		add(".");
	}
	
	/**
	 * Same to "git add -u <filePattern>"
	 * 
	 * @param filePattern
	 * @throws GitException
	 */
	public void add(String filePattern) throws GitException{
		try {
			git.add()
			.addFilepattern(filePattern)
			.setUpdate(true).call();
		} catch (Exception e) {
			throw new GitException("fail to add all changes.", e);
		}
	}
	
	/**
	 * same to "git commit -m "<message>"
	 * normally, will use source repository's commit hash as commit message.
	 * 
	 * @param sourceCommitHash
	 * @throws GitException
	 */
	public RevCommit commit(String sourceCommitHash) throws GitException{
		try {
			return git.commit().setMessage(sourceCommitHash).call();
		} catch (Exception e) {
			throw new GitException("fail to commit changes to repository:"+this.getDirectory(), e);
		} 
	}
	
	/**
	 * same to "git push"
	 * 
	 * @throws GitException
	 */
	public void push() throws GitException{
		try {
			git.push().setPushAll().call();
		} catch (Exception e) {
			throw new GitException("fail to push changes to repository:"+this.getRemoteUrl(), e);
		}
	}
	
	/**
	 * create branch with specified branch name.
	 * 
	 * @param branchName
	 * @throws GitException
	 */
	public void createBranch(String branchName) throws GitException{
		try {
			git.branchCreate().setName(branchName).call();
		} catch (Exception e) {
			throw new GitException("fail to create branch:"+branchName+" for repository: "+ this.getDirectory(), e);
		} 
	}
	
	/**
	 * same to "git checkout -b <branchName>"
	 * 
	 * @param branchName
	 * @throws GitException
	 */
	public void createNDCheckoutBranch(String branchName) throws GitException{
		this.createBranch(branchName);
		this.checkoutBranch(branchName);
	}
}
