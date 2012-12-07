package com.ebay.zeus.git.commands;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.pgm.TextBuiltin;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.ebay.zeus.exceptions.GitException;

public class Branch extends TextBuiltin {
	
	private Repository repository;
	
	public Branch( Repository repository ){
		this.repository = repository;
		this.out = new PrintWriter(System.out);
	}
	
	public Branch( File root) throws GitException{
		this( root, new PrintWriter(System.out));
	}
	
	public Branch( File root, PrintWriter out ) throws GitException{
		FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
		try {
			this.repository = repobuiler.findGitDir( root ).build();
			this.out = out;
		} catch (IOException e) {
			throw new GitException( e );
		}
	}
	
	public List<String> listAll(){
		List<String> branches = new ArrayList<String>();
		
		return branches;
	}

	@Override
	protected void run() throws Exception {
		// TODO Auto-generated method stub
		
	}
    
	
}
