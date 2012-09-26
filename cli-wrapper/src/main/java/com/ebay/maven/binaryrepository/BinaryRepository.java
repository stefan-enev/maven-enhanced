package com.ebay.maven.binaryrepository;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;
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
	
	public String getSourceRemoteUrl(){
		String remoteUrl = sourceRepository.getConfig().getString("remote", "origin", "url");
		return remoteUrl;
	}
	
	public boolean isBinaryRepositoryAvailable(){
		
		boolean result = false;
		
		// get the name of the source repository
		String repositoryName = getRepositoryName();
		
		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		
		// repository foldername
		String repositoryFolderName = f.getParentFile().getName();
		
		// go to parent directory
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + repositoryFolderName) );
		
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
	
	public boolean isRemoteBinaryRepositoryAvailable(){
		
		boolean result = false;
		
		String repositoryName = getRepositoryName();
		
		return result;
	}
	
	public void createBinaryRepository() throws IOException, GitException{
		
		// check whether "binary repository" exists
		if( isBinaryRepositoryAvailable() == true){
			throw new GitException("Repository already exists");
		}
		
		// find the name of the "source repository"
		String sourceRepoName = getRepositoryName();
		
		
		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		File sourceRepoFolder = f.getParentFile();
		
		String sourceRepoFolderName = f.getParentFile().getName();
		
		// go to parent directory
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + sourceRepoFolderName) );
		
		// create binary repository folder
		FileUtils.mkdir(binaryRepoFolder, true);
		
		// initialize "git" repository
		InitCommand initCmd = Git.init();
		initCmd.setDirectory(binaryRepoFolder);
		Git binaryRepo=null;
		try {
			binaryRepo = initCmd.call();
		} catch (GitAPIException e) {
			throw new GitException("unable to initialize repository", e);
		}
		
		// add a "README.md" file and commit
		File readmeFile = new File(binaryRepoFolder, "README.md");
		List<String> readmeContent = new ArrayList<String>();
		readmeContent.add("Binary Repository For " + getSourceRemoteUrl() );
		readmeContent.add("=======================================================");
		readmeContent.add("");
		readmeContent.add("Stores the class files for the above source repository");
		org.apache.commons.io.FileUtils.writeLines(readmeFile, readmeContent, "\n");
		
		// get "status"
		StatusCommand statusC = binaryRepo.status();
		Collection<String> toadd = GitUtils.getFilesToStage(statusC);
		
		// add "readme" file to staging
		if( toadd.size() > 0 ){
			AddCommand add = binaryRepo.add();
			for( String file : toadd ){
				add.addFilepattern(file);
			}
			
			try {
				
				add.call();
				
				CommitCommand commit = binaryRepo.commit();
				commit.setMessage("iniatial commit");
				
				commit.call();
				
			} catch (NoFilepatternException e) {
				throw new GitException("unable to add file(s)", e);
			} catch (GitAPIException e) {
				throw new GitException("Unable to add or commit", e);
			}
		}
		
		// read the branch from "source" repository
		String branchname = sourceRepository.getBranch();
		
		// create a "branch"
		if( !branchname.toLowerCase().equals("master") ){
			CreateBranchCommand branchCmd = binaryRepo.branchCreate();
			branchCmd.setName(branchname);
			try {
				// create branch
				branchCmd.call();
				
				// checkout the branch
				CheckoutCommand checkout = binaryRepo.checkout();
				checkout.setName(branchname);
				checkout.call();
				
			} catch (RefAlreadyExistsException e) {
				throw new GitException("unable to create a branch", e);
			} catch (RefNotFoundException e) {
				throw new GitException("unable to create a branch", e);
			} catch (InvalidRefNameException e) {
				throw new GitException("unable to create a branch", e);
			} catch (GitAPIException e) {
				throw new GitException("unable to create a branch", e);
			}
		}
		
		// TODO: add "remote" repository
		

		// find the "localobr" folders and exclude them during copy
		List<String> excludes = new ArrayList<String>();
		Collection<File> excludeFiles = FileUtil.findDirectoriesThatEndWith(sourceRepoFolder, "localobr");
		for( File file: excludeFiles ){
			excludes.add( file.getCanonicalPath() );
		}
		
		// copy the classes
		copyBinaryFolders("target", excludes, binaryRepoFolder);
		
		// get "status"
		StatusCommand statusCmd = binaryRepo.status();
		Collection<String> tobeAdded = GitUtils.getFilesToStage(statusCmd);
		
		// add files to "staging"
		if( tobeAdded.size() > 0 ){
			AddCommand addCmd = binaryRepo.add();
			for( String file : tobeAdded ){
				addCmd.addFilepattern(file);
			}
			
			try {
				addCmd.call();
			} catch (NoFilepatternException e) {
				throw new GitException("unable to add files", e);
			} catch (GitAPIException e) {
				throw new GitException("unable to add files", e);
			}
		}
		
		// commit
		CommitCommand commit = binaryRepo.commit();
		commit.setMessage("saving the files");
		try {
			commit.call();
		} catch (NoHeadException e) {
			throw new GitException("unable to commit", e);
		} catch (NoMessageException e) {
			throw new GitException("unable to commit", e);
		} catch (UnmergedPathsException e) {
			throw new GitException("unable to commit", e);
		} catch (ConcurrentRefUpdateException e) {
			throw new GitException("unable to commit", e);
		} catch (WrongRepositoryStateException e) {
			throw new GitException("unable to commit", e);
		} catch (GitAPIException e) {
			throw new GitException("unable to commit", e);
		}
		
		// TODO: push
	}
	
	public void copyBinaryFolders( String pattern, List<String> exclusionList, File destination ) throws IOException{
		
		File root = sourceRepository.getDirectory().getParentFile();
		
		
		Collection<File> files = FileUtil.findDirectoriesThatEndWith(root, pattern);
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return true;
			}
		};
		
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
