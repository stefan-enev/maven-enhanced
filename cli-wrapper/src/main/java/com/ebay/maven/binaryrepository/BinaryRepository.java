package com.ebay.maven.binaryrepository;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FileUtils;

import com.ebay.git.utils.GitUtils;
import com.ebay.utils.FileUtil;

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
		String repository = GitUtils.getRepositoryName(remoteUrl);
		return repository;
	}
	
	public boolean isBinaryRepositoryAvailable(){
		
		boolean result = false;
		
		// get the name of the source repository
		String repositoryName = getRepositoryName();
		
		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		
		// go to parent directory
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + repositoryName) );
		
		// check whether ".SourceRepo.git" folder exists
		if( binaryRepoFolder.exists() && 
				binaryRepoFolder.isDirectory() &&
				binaryRepoFolder.canRead() ){

		// check whether ".SourceRepo.git/.git" exists
			File binGit = new File(binaryRepoFolder, ".git");
			if( binGit.exists() &&
					binGit.isDirectory() &&
					binGit.canRead() ){
				result = true;
			}
		}
		
		return result;
	}
	
	public void createBinaryRepository() throws IOException{
		
		// check whether "binary repository" exists
		if( isBinaryRepositoryAvailable() == true){
			// TODO: anything else to do?
			return;
		}
		
		// find the name of the "source repository"
		String sourceRepoName = getRepositoryName();
		
		// create binary repository
		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		
		// go to parent directory
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + sourceRepoName) );
		
		FileUtils.mkdir(binaryRepoFolder, true);
	}
	
	public void copyBinaryFolders( String pattern, File destination ) throws IOException{
		
		File root = sourceRepository.getDirectory().getParentFile();
		
		
		Collection<File> files = FileUtil.findDirectoriesThatEndWith(root, pattern);
		
		FilenameFilter filter = new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return true;
			}
		};
		
		List<String> exclusionList = new ArrayList<String>();
		int pathlength = root.getCanonicalPath().length();
		
		for( File f : files ){
			
			// construct the directory to copied
			if( f.getCanonicalPath().startsWith(root.getCanonicalPath() )){
				
				// get the path that is additional
				String pathfraction  = f.getCanonicalPath().substring(pathlength);
				
				File d = new File(destination, pathfraction );
				
				System.out.println( "copying " + f.getCanonicalPath() + " to " + d.getAbsolutePath() );
				FileUtil.doCopyDirectory(f, d, filter, true, exclusionList);
			}
		}
	}

}
