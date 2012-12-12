package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;

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
			.setUpdate(false).call();
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
	public void push(boolean force) throws GitException{
		try {
			git.push().setPushAll().setForce(force).call();
		} catch (Exception e) {
			throw new GitException("fail to push changes to repository:"+this.getRemoteUrl(), e);
		}
	}
	
	/**
	 * by now it's only for test.
	 * 
	 * @param commitHash
	 * @throws GitException
	 */
	public void reset(String commitHash) throws GitException{
		try {
			git.reset().setMode(ResetType.HARD).setRef(commitHash).call();
		} catch (Exception e) {
			throw new GitException("fail to reset commit:"+commitHash+" to repository:"+this.getDirectory().getParent(), e);
		}
	}
	
	public RevCommit commitNDPushAll(String commitHash) throws GitException{
		addAll();
        RevCommit commit = commit(commitHash);
        push(false);
        
        return commit;
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
	public CheckoutResult checkoutNewBranch(String branchName) throws GitException{
		this.createBranch(branchName);
		CheckoutResult result = this.checkoutBranch(branchName);

		this.addRemoteBranch(branchName);

		// push this branch to remote
		this.push(false);
		
		return result;
	}
	
	/**
	 * add remote url into git config.
	 * 
	 * @param remoteUrl
	 * @throws GitException 
	 */
	public void addRemoteUrl(String remoteUrl) throws GitException{
		this.getConfig().setString("remote", "origin", "url", remoteUrl);
		this.getConfig().setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
		
        try {
			this.getConfig().save();
		} catch (IOException e) {
			throw new GitException("fail to add remote url: "+remoteUrl+" to repository: "+ this.getDirectory(), e);
		}
	}
	
	/**
	 * add new branch into git config
	 * 
	 * @param branchName
	 * @throws GitException 
	 */
	public void addRemoteBranch(String branchName) throws GitException{
		FileBasedConfig config = this.getConfig();
		config.setString("branch", branchName, "remote", "origin");
		config.setString("branch", branchName, "merge", "refs/heads/"+ branchName);
		try {
			config.save();
		} catch (IOException e) {
			throw new GitException("fail to add branch: "+branchName+" to repository: "+ this.getDirectory(), e);
		}
	}
}