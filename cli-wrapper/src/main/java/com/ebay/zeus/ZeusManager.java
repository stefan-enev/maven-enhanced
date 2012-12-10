package com.ebay.zeus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.mappingservice.MappingServiceClient;
import com.ebay.zeus.repositorys.BinaryZeusRepository;
import com.ebay.zeus.repositorys.SourceZeusRepository;
import com.ebay.zeus.utils.FileUtil;
import com.ebay.zeus.utils.GitUtils;
import com.ebay.zeus.utils.MavenUtil;
import com.ebay.zeus.utils.ZeusUtil;

/**
 * it manage main logic flow.
 * 
 * @author snambi, yunfwang@ebay.com
 *
 */
public class ZeusManager {
	public final Logger logger = LoggerFactory.getLogger(this.getClass());
	
    private File root;
	private SourceZeusRepository sourceRepository;
    private BinaryZeusRepository binaryRepository;
    
    private File srcRepoRoot;
    
    private boolean isLocalBinaryRepoExisted = false;
    
    private static MappingServiceClient mappingServiceClient;

	public ZeusManager(File root) throws GitException {
		if (root.canRead() && root.isDirectory()){
            this.root = root;
		} else {
			throw new GitException("Please make sure current directory is a valid source code git repository, can't read it.");
		}
		
		initialize();
    }
	
	private void initialize() throws GitException{
		try {
			this.sourceRepository = new SourceZeusRepository(root);
			File sourceRepoRoot = sourceRepository.getDirectory();

			if (ZeusUtil.isLocalBinaryRepositoryExisted(sourceRepoRoot)) {
				File BinaryRepoRoot = ZeusUtil.getBinaryRepositoryRoot(sourceRepoRoot);
				this.binaryRepository = new BinaryZeusRepository(BinaryRepoRoot);

				isLocalBinaryRepoExisted = true;
			}
			
			srcRepoRoot = getSourceRepoRoot();
	        
		} catch (IOException e) {
			throw new GitException("Fail to initialize source repository or binary repository.", e);
		}
	}

	private File getSourceRepoRoot(){
		return sourceRepository.getDirectory().getParentFile();
	}
	
	public void createOrUpdateBinaryRepository(String mapSvcUrl)
			throws GitException {

		// assume the current directory the "root" of the project
		try {

			if (mapSvcUrl != null) {
				setBaseServiceUrl(mapSvcUrl);
			}

			long starttime = 0l;
			long endtime = 0l;

			while (true) {

				if (isLocalBinaryRepoExisted) {

					// if previous run started in less then 1 minute before,
					// wait for a minute
					long begintime = Calendar.getInstance().getTimeInMillis();
					if ((begintime - starttime) < (60 * 1000)) {
						Thread.sleep(60 * 1000);
					}

					// calculate start time
					starttime = Calendar.getInstance().getTimeInMillis();

					// TODO: run 'show-ref' and keep the current status of src &
					// bin repos in memory before doing a 'fetch'

					// TODO: ideally we need 'git fetch' and record what is
					// fetched, which is then processed

					// TODO: calculate how many new branches/commits have been
					// created since the last fetch on source repo
					// zmanager.findNewCommits();

					// TODO: figure out the new commits and process each one of
					// them
					// zmanager.processNewCommits();

					// TODO: remove this after implementing above steps. this is
					// temporary
					// get the latest by "git pull" on "source" and "binary".
					gitpull();

					// TODO: perform maven build. Remove this after implementing
					// 'processNewCommits'
					// even if it fails, continue the loop
					
					MavenUtil.executeMvnCommand("compile", root, System.out);

					// update binary repo
					updateBinaryRepository();

					endtime = Calendar.getInstance().getTimeInMillis();

					logger.info("Updated in " + ((endtime - starttime) / 1000) + " seconds");

				} else {

					if (ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository.getRemoteUrl())) {
						cloneBinaryRepository(false);
						
						//TODO: 1. if branch existed, checkout it, if no, should checkout new local branch
						//      2. Then copy source repo's classes into binary repo and upload changes
						checkoutBinaryBranch();
						
						updateBinaryRepository();
					} else {

						// calculate start time
						starttime = Calendar.getInstance().getTimeInMillis();
						createBinaryRepository();

						endtime = Calendar.getInstance().getTimeInMillis();
						logger.info("Created in " + ((endtime - starttime) / 1000) + " seconds");
					}
				}

			}
		} catch (Exception e) {
			throw new GitException("Fail to create/update binary repository.", e);
		}
	}
	
	public void setupProject(String mapSvcUrl) throws GitException {
		// TODO: download dependencies
		
		// read the source project
        // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
        // root = new File("D:\\dev\\devex\\binrepo-devex");
		
		setBaseServiceUrl(mapSvcUrl);

		if (ZeusUtil.isBinaryRepositoryExisted(sourceRepository)) {
			checkoutBinaryBranch();
			copyClassesFromBinaryRepoToSourceRepo();
			
			logger.info("setup is complete");
		} else if (ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository
				.getRemoteUrl())) {
			cloneBinaryRepository(true);
			checkoutBinaryBranch();
			copyClassesFromBinaryRepoToSourceRepo();
			
			logger.info("setup is complete");
		} else {
			// TODO: anything we can do?
			logger.info("Binary repository not available. exiting...");
		}
		
		// Get the binary classes and populate project "target" folders
	}
	
	/**
	 * Pull latest from 'source' and 'binary' repository.
	 * @throws GitException 
	 */
	private void gitpull() throws GitException{
		sourceRepository.pull();
		binaryRepository.pull();
	}

	/**
	 * if both local & remote binary repository not existed, then need to create binary repository.
	 * 1. create a local repo
	 * 2. copy target classes from source repository to binary repository.
	 * 3. push it to remote
	 * 4. upload mapping data.
	 * 
	 * @throws Exception
	 */
	private void createBinaryRepository() throws Exception {
		logger.info("start to create binary repository.");
		
        // check whether "binary repository" exists
		if (ZeusUtil.isBinaryRepositoryExisted(sourceRepository)) 
			throw new GitException("Repository already exists");

		// find the name of the "source repository"
		//String sourceRepoName = getRepositoryName();

        // find where ".git" folder is found
		
        File binaryRepoRoot = ZeusUtil.getBinaryRepositoryRoot(srcRepoRoot);

		// create binary repository folder
		FileUtils.mkdir(binaryRepoRoot, true);

        // initialize "git" repository
		GitUtils.initRepository(binaryRepoRoot);
		
		logger.info("adding readme.md file");
        
        ZeusUtil.createReadMeFile(binaryRepoRoot, sourceRepository.getRemoteUrl());

        binaryRepository = new BinaryZeusRepository(binaryRepoRoot);
        binaryRepository.addAll();

        // Calculate the remote url for binary repository
        String binRemoteUrl = ZeusUtil.calculateBinaryRepositoryUrl(sourceRepository.getRemoteUrl());

        // TODO: check whether the remote exists, if not create it, else fail
        ZeusUtil.createRemoteRepository(binRemoteUrl);

        // add "remote" repository
        binaryRepository.addRemoteUrl(binRemoteUrl);

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
		Collection<File> excludeFiles = FileUtil.findDirectoriesThatEndWith(srcRepoRoot, "localobr");
		for( File file: excludeFiles ){
			excludes.add( file.getCanonicalPath() );
		}

		// copy the classes
		logger.info("copying binary files");
		FileUtil.copyBinaryFolders("target", excludes, sourceRepository.getDirectory(), binaryRepoRoot);

		binaryRepository.addAll();
		
		//TODO: need to use source repo's commit hash
		binaryRepository.commit("Source commit hash");
		
		binaryRepository.push();

        uploadMappingData(sourceRepository.getRemoteUrl(), binRemoteUrl, branchname);
        
        logger.info("updated the map service");
    }
	
	/**
	 * upload mapping data onto mapping service.
	 * 
	 * @param sourceRepoUrl
	 * @param binRepoUrl
	 * @param branchname
	 * @throws Exception
	 */
	private void uploadMappingData(String sourceRepoUrl, String binRepoUrl, String branchname) throws Exception {
		final String sourceRepoHeadHash = sourceRepository.getHead();
		final String binRepoHeadHash = binaryRepository.getHead();
        final String binRepoBranchName = binaryRepository.getBranch();

        logger.info("Update Bin Repo Service with the new changes - POST new object to service");
        
   	    mappingServiceClient.post(sourceRepoUrl, branchname, sourceRepoHeadHash, binRepoUrl, binRepoBranchName, binRepoHeadHash);
	}

	/**
	 * if local binary repository not existed, clone binary repository from remote.
	 * 
	 * @param readonly
	 * @throws GitException
	 */
	private void cloneBinaryRepository( boolean readonly ) throws GitException {
		logger.info("cloning binary repository....");
		
		// find the name of the "source repository"
		String srcRepoUrl = sourceRepository.getRemoteUrl();;
		String org = GitUtils.getOrgName(srcRepoUrl);
		String repoName = GitUtils.getRepositoryName(srcRepoUrl);
		String binaryRepoName = ZeusUtil.calculateBinaryRepositoryName(org, repoName);

		// construct the binary repository URL
		String giturl;
		if( readonly == true ){
			giturl = "git://github.scm.corp.ebay.com/Binary/" + binaryRepoName + ".git";
		}else{
			giturl = "git@github.scm.corp.ebay.com:Binary/" + binaryRepoName + ".git";
		}

		// calculate binary repository folder
		File binaryRepoFolder = ZeusUtil.getBinaryRepositoryRoot(srcRepoRoot);

		// clone the binary repository
		Git binGit = GitUtils.cloneRepository(giturl, binaryRepoFolder);
		try {
			this.binaryRepository = new BinaryZeusRepository(binGit.getRepository().getDirectory());
		} catch (IOException e) {
			throw new GitException("Fail to initialize BinaryZeusRepository.", e);
		}
		
		logger.info("binary repository cloned");
    }
	
	/**
	 * checkout binary repository's branch according to source repository's current branch.
	 * 
	 * @throws GitException
	 */
	private void checkoutBinaryBranch() throws GitException{
		// read the branch from "source" repository
		String branchName = null;
		try {
			branchName = sourceRepository.getBranch();
		} catch (IOException e) {
			throw new GitException("Fail to get source repository's current branch.", e);
		}

		// Checkout the "branch" if it is not equal to "master"
		if (!branchName.toLowerCase().equals("master")) {
			checkoutBinaryBranch(branchName);
		}
	}
	
	private void copyClassesFromBinaryRepoToSourceRepo() throws GitException{
		try {
			FileUtil.copyBinaryFolders(binaryRepository.getDirectory()
					.getParentFile(), sourceRepository.getDirectory()
					.getParentFile(), ".git");
		} catch (IOException e) {
			throw new GitException(
					"Fail to copy binary repository's folders into source repository.",	e);
		}
	}

	/**
	 * checkout specified branch for binary repository.
	 * it try to make binary repository ready for next step(like copy classes and push).
	 * if remote branch not existed, create it.
	 * 
	 * @param branchName
	 * @throws GitException
	 */
	private void checkoutBinaryBranch(String branchName) throws GitException {
		// check whether the branch exists
		boolean remoteBranchExists = GitUtils.isRemoteBranchExists(binaryRepository, branchName);
		
		CheckoutResult result =null;
		
		if( !remoteBranchExists ){
			binaryRepository.createBranch(branchName);
			result = binaryRepository.checkoutBranch(branchName);

			binaryRepository.addRemoteBranch(branchName);

			// push this branch to remote
			binaryRepository.push();
		}else{
			
			// check the current branch in binary repository
			try {
				if( !binaryRepository.getBranch().equals(branchName) ){
					binaryRepository.checkoutRemoteBranch(branchName);
				}
			} catch (IOException e) {
				throw new GitException("can't checkout remote branch("+branchName+") into local.", e);
			}

		}
		
		if( result != null && result.getStatus().equals(CheckoutResult.OK_RESULT)){
			logger.info("checkout is OK");
		}else{
			// TODO: handle the error.
			throw new GitException("Fail to checkout binary repository's branch:"+branchName);
		}
	}

	/**
	 * complete following works:
	 * 1. copy source repo's classes into binary repo.
	 * 2. push binary repo's changes into remote. 
	 * 
	 * FIXME: 
	 *   haven't check whether binary repo is the same branch to source repo.
	 * 
	 * @throws Exception
	 */
    private void updateBinaryRepository() 
    		throws Exception {
    	
    	logger.info("updating binary repository...");
    	
        // 1. Check if repository exists remotely git@github.scm.corp.ebay.com/Binary/Repo_Binary.git
        final String repoUrl = sourceRepository.getRemoteUrl();

        final File binRepoRoot = ZeusUtil.getBinaryRepositoryRoot(srcRepoRoot);
        
        logger.info("SourceRepository = " + srcRepoRoot.getCanonicalPath() + "\nBinaryRepository = " + binRepoRoot.getCanonicalPath());

        // 2. Get branch/commit hash for the source repo - the actual source code
        final String branch = sourceRepository.getBranch();
        String headHash = sourceRepository.getHead();

        // 3. Call the BinRepo service and check if a corresponding BinRepo entry exists
        // 4. If not copy all the target folders from the source repo to the binary repo - root to root copy of artifacts
        if (mappingServiceClient.isEntryExisted(repoUrl, branch, headHash)) {
			logger.info("Source Directory:'" + srcRepoRoot.getCanonicalPath()
					+ "' Destination Directory:'"
					+ binRepoRoot.getCanonicalPath() + "'");
			
            FileUtil.copyBinaries(srcRepoRoot, binRepoRoot);
        }else{
        	//FIXME:? if no entry existed, why still update binary repo?
        }

        // 5. Call git status to get the delta (Use StatusCommand and refine it)
        binaryRepository.addAll();
        
        //TODO: should use source repo's HEAD as commit message.
        binaryRepository.commit("update binary repo");
        binaryRepository.push();
        
        String binRepoUrl = ZeusUtil.calculateBinaryRepositoryUrl(sourceRepository.getRemoteUrl());

        // 7. Call the BinRepo service and create a new entity for this change - repoUrl, branch, and commit
        uploadMappingData(sourceRepository.getRemoteUrl(), binRepoUrl, binaryRepository.getBranch());
        
        logger.info("Binary repository updated.");
    }
    
    public void setBaseServiceUrl(String url){
    	mappingServiceClient.setBaseServiceUrl(url);
    }
    
//    //TODO: haven't start it yet.
//	public void downloadDependencies(){
//		
//		// read the pom.xml
//		// TODO: get the pom.xml path from -f argument
//		Model model = PomUtils.readModel("pom.xml");
//		
//		// collect the repositories in the correct order
//		List<Repository> repositories= model.getRepositories();
//		
//		// collect the dependencies
//		List<Dependency> dependencies = model.getDependencies();
//		
//		System.out.println(repositories.toString() + dependencies.toString() );
//		// TODO: read the settings.xml to collect the repositories
//		
//		// construct the JSON request 
//		
//		// call nexus repository with JSON post
//		
//		// download the dependencies
//		
//		// invoke maven with dependencies
//	}

}
