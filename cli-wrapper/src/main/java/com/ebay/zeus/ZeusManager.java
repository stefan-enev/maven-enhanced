package com.ebay.zeus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FileUtils;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.exceptions.ProcessException;
import com.ebay.zeus.github.GitHubClient;
import com.ebay.zeus.mappingservice.MappingServiceClient;
import com.ebay.zeus.repositorys.BinaryZeusRepository;
import com.ebay.zeus.repositorys.SourceZeusRepository;
import com.ebay.zeus.utils.FileUtil;
import com.ebay.zeus.utils.GitUtils;
import com.ebay.zeus.utils.MavenUtil;
import com.ebay.zeus.utils.ZeusUtil;

/**
 * it manage mail logic flow.
 * 
 * @author snambi, yunfwang@ebay.com
 *
 */
public class ZeusManager {
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
    private File root;
	private SourceZeusRepository sourceRepository;
    private BinaryZeusRepository binaryRepository;
    
    private static MappingServiceClient client;

	public ZeusManager(File root) throws IOException {
		
        if (root.canRead() && root.isDirectory()){
            this.root = root;
			this.sourceRepository = new SourceZeusRepository(root);
        } else{
			// TODO: throw exception
		}
    }

	/**
	 * Pull latest from 'source' and 'binary' repository.
	 */
	public boolean gitpull(){
		try{
			sourceRepository.pull();
			binaryRepository.pull();
			
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	public boolean build(String command) {
		boolean result;
		try {
			result = MavenUtil.executeMvnCommand(command, root, System.out);
		} catch (ProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = false;
		}
		
		System.out.println("maven command " + command + " exited with result " + result );
		return result;
	}

	public void createBinaryRepository() throws Exception {
		logger.info("start to create binary repository.");
		
        // check whether "binary repository" exists
		if (ZeusUtil.isBinaryRepositoryExisted(sourceRepository)) throw new GitException("Repository already exists");

		// find the name of the "source repository"
		//String sourceRepoName = getRepositoryName();

        // find where ".git" folder is found
		File sourceRepoRoot = sourceRepository.getDirectory();
		File sourceRepoFolder = sourceRepoRoot.getParentFile();

		String sourceRepoFolderName = sourceRepoRoot.getParentFile().getName();

		// calculate binary repository folder
		File parent = sourceRepoRoot.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + sourceRepoFolderName) );

		// create binary repository folder
		FileUtils.mkdir(binaryRepoFolder, true);

        // initialize "git" repository
		GitUtils.initRepository(binaryRepoFolder);
		
		logger.info("adding readme.md file");
        
        ZeusUtil.createReadMeFile(binaryRepoFolder, sourceRepository.getRemoteUrl());

        binaryRepository = new BinaryZeusRepository(binaryRepoFolder);
        binaryRepository.addAll();

        // Calculate the remote url for binary repository
        String remoteUrl = ZeusUtil.calculateBinaryRepositoryUrl(sourceRepository.getRemoteUrl());

        // TODO: check whether the remote exists, if not create it, else fail
        ZeusUtil.createRemoteRepository(remoteUrl);

        // add "remote" repository
        binaryRepository.getConfig().setString("remote", "origin", "url", remoteUrl);
        binaryRepository.getConfig().save();

		// push
        logger.info("pushing to remote");
		binaryRepository.push();
		
		// read the branch from "source" repository
		String branchname = sourceRepository.getBranch();

		// create a "branch"
		if( !branchname.toLowerCase().equals("master") ){
			binaryRepository.createNDCheckoutBranch(branchname);
		}

		// find the "localobr" folders and exclude them during copy
		List<String> excludes = new ArrayList<String>();
		Collection<File> excludeFiles = FileUtil.findDirectoriesThatEndWith(sourceRepoFolder, "localobr");
		for( File file: excludeFiles ){
			excludes.add( file.getCanonicalPath() );
		}

		// copy the classes
		logger.info("copying binary files");
		FileUtil.copyBinaryFolders("target", excludes, sourceRepository.getDirectory(), binaryRepoFolder);

		binaryRepository.addAll();
		
		//TODO: need to use source repo's commit hash
		binaryRepository.commit("Source commit hash");
		
		binaryRepository.push();

        uploadMappingData(sourceRepository.getRemoteUrl(), sourceRepoRoot, binaryRepoFolder, remoteUrl, branchname);
        
        logger.info("updated the map service");
    }

	private void uploadMappingData(String sourceRepoUrl, File sourceRepoRoot, File binaryRepoRoot,
			String binRepoUrl, String branchname) throws Exception {
		
        // branchName was computed above
		final String sourceRepoHeadHash = sourceRepository.getHead();
		final String binRepoHeadHash = binaryRepository.getHead();
        final String binRepoBranchName = binaryRepository.getBranch();

        System.out.println("Update Bin Repo Service with the new changes - POST new object to service");
        
   	    client.post(sourceRepoUrl, branchname, sourceRepoHeadHash, binRepoUrl, binRepoBranchName, binRepoHeadHash);
	}

	public void cloneBinaryRepository( boolean readonly ) throws GitException {

		// find the name of the "source repository"
		String srcRepoUrl = sourceRepository.getRemoteUrl();;
		String org = GitUtils.getOrgName(srcRepoUrl);
		String repoName = GitUtils.getRepositoryName(srcRepoUrl);
		String binaryRepoName = ZeusUtil.calculateBinaryRepositoryName(org, repoName);
		

		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();

		String sourceRepoFolderName = f.getParentFile().getName();

		// construct the binary repository URL
		String giturl;
		if( readonly == true ){
			giturl = "git://github.scm.corp.ebay.com/Binary/" + binaryRepoName + ".git";
		}else{
			giturl = "git@github.scm.corp.ebay.com:Binary/" + binaryRepoName + ".git";
		}

		// calculate binary repository folder
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + sourceRepoFolderName) );

		// clone the binary repository
		CloneCommand cloneCmd = Git.cloneRepository();
		cloneCmd.setURI( giturl );
		cloneCmd.setDirectory(binaryRepoFolder);
		cloneCmd.setCloneAllBranches(true);

		Git binrepository = null;
		
		try {
			
			System.out.println("cloning repository " + giturl );
			binrepository = cloneCmd.call();
			
			binaryRepository = new BinaryZeusRepository( binrepository.getRepository().getDirectory());
            
        } catch (InvalidRemoteException e) {
			throw new GitException("unable to clone " + giturl, e);
		} catch (TransportException e) {
			throw new GitException("unable to clone " + giturl, e);
		} catch (GitAPIException e) {
			throw new GitException("unable to clone " + giturl, e);
		} catch (IOException e) {
			throw new GitException("unable assign " + giturl, e);
		}
		
		checkoutAndCopyFromBinaryRepository();

    }
	
	public void checkoutAndCopyFromBinaryRepository() throws GitException{
		// read the branch from "source" repository
		String branchName = "master";
		try {
			branchName = sourceRepository.getBranch();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Checkout the "branch" if it is not equal to "master"
		if( !branchName.toLowerCase().equals("master") ){
			
			// check whether the branch exists
			boolean remoteBranchExists = GitUtils.isRemoteBranchExists(binaryRepository, branchName);
			
			CheckoutResult result =null;
			
			if( !remoteBranchExists ){
				
				try {
					binaryRepository.createBranch(branchName);
					result = binaryRepository.checkoutBranch(branchName);
					
					FileBasedConfig config = binaryRepository.getConfig();
					config.setString("branch", branchName, "remote", "origin");
					config.setString("branch", branchName, "merge", "refs/heads/"+ branchName);
					config.save();
					
					// push this branch to remote
					binaryRepository.push();
				} catch (IOException e) {
					throw new GitException("unable to save git config " + branchName, e);
				}
				
			}else{
				
				// check the current branch in binary repository
				try {
					if( !binaryRepository.getBranch().equals(branchName) ){
						binaryRepository.checkoutRemoteBranch(branchName);
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
			
			if( result != null && result.getStatus().equals(CheckoutResult.OK_RESULT)){
				logger.info("checkout is OK");
			}else{
				// TODO: handle the error.
			}
		}
		
		try {
			FileUtil.copyBinaryFolders(binaryRepository.getDirectory().getParentFile(), 
										sourceRepository.getDirectory().getParentFile(), 
										".git");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public void updateBinaryRepository() 
    		throws Exception {
    	
        // 1. Check if repository exists remotely git@github.scm.corp.ebay.com/Binary/Repo_Binary.git
        // find the name of the "source repository"
        final String repoUrl = sourceRepository.getRemoteUrl();

        // find where ".git" folder is found  
        // SourceRepository = D:\dev\devex\binrepo-devex 
        // BinaryRepository = D:\dev\devex\.binrepo-devex
        final File srcRepoDir = sourceRepository.getDirectory();
        final File sourceDir = srcRepoDir.getParentFile();

        final String sourceRepoFolder = srcRepoDir.getParentFile().getCanonicalPath();

        final File parent = srcRepoDir.getParentFile().getParentFile();
        final File binaryRepoDir = new File(parent, "." + srcRepoDir.getParentFile().getName());
        System.out.println("SourceRepository = " + sourceRepoFolder + "\nBinaryRepository = " + binaryRepoDir.getCanonicalPath());

        // 2. Get branch/commit hash for the source repo - the actual source code
        final String branch = sourceRepository.getBranch();
        String headHash = sourceRepository.getHead();

        // 3. Call the BinRepo service and check if a corresponding BinRepo entry exists
        // 4. If not copy all the target folders from the source repo to the binary repo - root to root copy of artifacts
        if (client.isEntryExisted(repoUrl, branch, headHash)) {
            System.out.println("Source Directory:'" + sourceDir.getCanonicalPath() + "' Destination Directory:'" + binaryRepoDir.getCanonicalPath() + "'");
            FileUtil.copyBinaries(sourceDir, binaryRepoDir);
        }

        // 5. Call git status to get the delta (Use StatusCommand and refine it)
        binaryRepository.addAll();
        
        String msg = "Saving Repo:%s Branch:%s CommitHash:%s Time:%s";
        final String formattedMsg = String.format(msg, repoUrl, branch, headHash, new Date().toString());
        String commitHashBinRepo = binaryRepository.commit(formattedMsg).getName();
        
        binaryRepository.push();
        
        // Calculate the remote url for binary repository
        String binRepoUrl = ZeusUtil.calculateBinaryRepositoryUrl(sourceRepository.getRemoteUrl());

        // 7. Call the BinRepo service and create a new entity for this change - repoUrl, branch, and commit
        client.post(repoUrl, branch, headHash, binRepoUrl, binaryRepository.getBranch(), commitHashBinRepo);
    }
    
    public void setBaseServiceUrl(String url){
    	client.setBaseServiceUrl(url);
    }

}
