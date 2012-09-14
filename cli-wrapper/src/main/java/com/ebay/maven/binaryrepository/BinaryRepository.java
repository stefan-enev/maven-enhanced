package com.ebay.maven.binaryrepository;

import java.io.File;
import java.net.URL;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.storage.file.FileRepository;

public class BinaryRepository {
	
	private URL remoteUrl;
	private String branch;
	private String tag;
	private String commithash;
	
	private File root;
	private File destination;
	
	public BinaryRepository( File root ){
		
		if( root.canRead() == true && 
			root.isDirectory() == true){
			
			this.root = root;
			
		}else{
			// TODO: throw exception
		}
	
	}
	
	public void create() throws Exception{
			
		// check whether ".git" folder exists at the root
		File gitfolder = new File( root, ".git");
		if( gitfolder.exists() == false || 
				gitfolder.isDirectory() == false || 
				gitfolder.canRead() == false){
			
			throw new Exception("Not a valid git repository. Binary repository can be created only for git projects");
		}
		
		// get the repository name.
		FileRepository repository = new FileRepository(gitfolder);
		Git git = Git.wrap(repository);
		
		
		
		// check whether "repository-binary" exists parallel to "root" folder
		
	}

}
