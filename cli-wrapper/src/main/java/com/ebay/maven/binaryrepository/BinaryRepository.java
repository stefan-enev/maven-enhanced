package com.ebay.maven.binaryrepository;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class BinaryRepository {
	
	private File root;
	private File destination;
	
	private FileRepository sourceRepository;
	private FileRepository binaryRepository;
	
	public BinaryRepository( File root ) throws IOException{
		
		if( root.canRead() == true && 
			root.isDirectory() == true){
			
			this.root = root;
			
			// get the repository name.
			FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
			this.sourceRepository = repobuiler.findGitDir(root).build();
			
		}else{
			// TODO: throw exception
		}
	
	}
	
	public String getRepositoryName(){
		String remoteUrl = sourceRepository.getConfig().getString("remote", "origin", "url");
		return remoteUrl;
	}

}
