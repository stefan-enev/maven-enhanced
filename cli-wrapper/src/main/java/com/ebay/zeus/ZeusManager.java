package com.ebay.zeus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.exceptions.ProcessException;
import com.ebay.zeus.repository.BinaryZeusRepository;
import com.ebay.zeus.repository.SourceZeusRepository;
import com.ebay.zeus.utils.Constants;
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
    
//    private static MappingServiceClient mappingServiceClient;

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
			logger.info("initializing basic information...");
			
			this.sourceRepository = new SourceZeusRepository(new File(root, Constants.DOT_GIT));
			File sourceRepoRoot = sourceRepository.getDirectory();

			if (ZeusUtil.isLocalBinaryRepositoryExisted(sourceRepoRoot)) {
				File binaryRepoRoot = ZeusUtil.getExistedBinaryRepositoryRoot(sourceRepoRoot);
				this.binaryRepository = new BinaryZeusRepository(new File(binaryRepoRoot, Constants.DOT_GIT));

				isLocalBinaryRepoExisted = true;
			}
			
			srcRepoRoot = sourceRepository.getDirectory().getParentFile();
			
//			mappingServiceClient = new MappingServiceClient();
			logger.info("initialized basic information.");
		} catch (IOException e) {
			throw new GitException("Fail to initialize source repository or binary repository.", e);
		}
	}
	
	public void createOrUpdateBinaryRepository()
			throws GitException {

		logger.info("starting to create/update binary repository");
		
		// assume the current directory the "root" of the project
		try {
			long starttime = 0l;
			long endtime = 0l;

			if (isLocalBinaryRepoExisted) {
				logger.info("local binary repository existed, update it with source repo's classes.");
				
				// if previous run started in less then 1 minute before,
				// wait for a minute
				long begintime = Calendar.getInstance().getTimeInMillis();
				if ((begintime - starttime) < (60 * 1000)) {
					Thread.sleep(60 * 1000);
				}

				// calculate start time
				starttime = Calendar.getInstance().getTimeInMillis();

				updateExistedLocalBinaryRepository();

				endtime = Calendar.getInstance().getTimeInMillis();
				logger.info("Updated in " + ((endtime - starttime) / 1000) + " seconds");
			} else {
				logger.info("local binary repository not existed.");
				if (ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository.getRemoteUrl())) {
					logger.info("remote binary repository existed.");
					cloneBinaryRepository(false);

					// TODO: 1. if branch existed, checkout it, if no, should
					// checkout new local branch
					// 2. Then copy source repo's classes into binary repo and
					// upload changes
					
					updateExistedLocalBinaryRepository();
				} else {
					logger.info("remote binary repository not existed, create it.");
					// calculate start time
					starttime = Calendar.getInstance().getTimeInMillis();
					createBinaryRepository();

					endtime = Calendar.getInstance().getTimeInMillis();
					logger.info("Created in " + ((endtime - starttime) / 1000) + " seconds");
				}
			}

		} catch (Exception e) {
			throw new GitException("Fail to create/update binary repository.", e);
		}
		
		logger.info("created/updated binary repository");
	}
	
	/**
	 * if local binary repository existed, update it.
	 * 1. git pull source repository/binary repository
	 * 2. "mvn compile" for source repository
	 * 3. copy classes from source repository to binary repository
	 * 
	 * FIXME: 
	 * need to pre-check logic before "mvn compile", 
	 * need to find whether there is root pom file in source repository.
	 * 
	 * @throws Exception
	 */
	private void updateExistedLocalBinaryRepository() throws Exception{
		logger.info("Updating existed binary repository...");
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

		// update binary repo
		updateBinaryRepository();
		
		logger.info("Updated existed binary repository");
	}

	/**
	 * execute "mvn compile" in root folder.
	 * if there aren't pom file in root folder, won't execute "mvn compile"
	 * 
	 * @return true:executed compile; false:hasn't executed compile.
	 * @throws ProcessException
	 */
	private boolean compile() throws ProcessException {
		File pomFile = new File(root, "pom.xml");
		if (pomFile.exists()){
			MavenUtil.executeMvnCommand("compile", root, System.out);
			
			return true;
		}
		
		return false;
	}
	
	public void setupProject() throws GitException, IOException {
		// TODO: download dependencies
		
		// read the source project
        // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
        // root = new File("D:\\dev\\devex\\binrepo-devex");
		
		if (ZeusUtil.isBinaryRepositoryExisted(sourceRepository)) {
			checkoutBinaryBranch(sourceRepository.getBranch());
			copyClassesFromBinaryRepoToSourceRepo();
			
			logger.info("setup is complete");
		} else if (ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository
				.getRemoteUrl())) {
			cloneBinaryRepository(true);
			checkoutBinaryBranch(sourceRepository.getBranch());
			copyClassesFromBinaryRepoToSourceRepo();
			
			logger.info("setup is complete");
		} else {
			// TODO: anything we can do?
			logger.info("Binary repository not available. exiting...");
		}
		
		// Get the binary classes and populate project "target" folders
	}
	
	private void copyClassesFromBinaryRepoToSourceRepo() throws GitException{
		try {
			FileUtil.copyBinaryFolders(binaryRepository.getDirectory()
					.getParentFile(), sourceRepository.getDirectory()
					.getParentFile(), Constants.DOT_GIT);
		} catch (IOException e) {
			throw new GitException(
					"Fail to copy binary repository's folders into source repository.",	e);
		}
	}
	
	/**
	 * Pull latest from 'source' and 'binary' repository.
	 * @throws GitException 
	 */
	private void gitpull() throws GitException{
		logger.info("Pulling source repository & binary repository...");
		
		sourceRepository.pull();
		binaryRepository.pull();
		
		logger.info("Pulled source repository & binary repository.");
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

        File binaryRepoRoot = ZeusUtil.getBinaryRepositoryRoot(srcRepoRoot);

        // initialize "git" repository
		GitUtils.initRepository(binaryRepoRoot);

		// Calculate the remote url for binary repository
        String binRemoteUrl = ZeusUtil.calculateBinaryRepositoryUrl(sourceRepository.getRemoteUrl());
        
        createRemoteBinaryRepository(binaryRepoRoot, binRemoteUrl);
		addReadmeForNewBinaryRepo(binaryRepoRoot);
		
		List<String> allBranches = sourceRepository.getAllBranches();
		
		//update master first.
		updateBinaryBranch(Constants.MASTER_BRANCH);
		
		for (String branch:allBranches){
			if (!branch.contains(Constants.MASTER_BRANCH)){
				updateBinaryBranch(branch);
			}
		}
		
//		addSrcRepoClassesToBinRepo(binaryRepoRoot);
		binaryRepository.push(false);
		
//        uploadMappingData(sourceRepository.getRemoteUrl(), binRemoteUrl, sourceRepository.getBranch());
        
        logger.info("Completed to create binary repository.");
    }

	/**
	 * add readme file into binary repo.
	 * 
	 * @param binaryRepoRoot
	 * @param binRemoteUrl
	 * @return
	 * @throws IOException
	 * @throws GitException
	 */
	private void addReadmeForNewBinaryRepo(File binaryRepoRoot)
			throws IOException, GitException {
		logger.info("adding readme.md file");
        
        ZeusUtil.createReadMeFile(binaryRepoRoot, sourceRepository.getRemoteUrl());

		// addall/commit/push
        binaryRepository.commitNDPushAll(Constants.FIRST_COMMIT_MESSAGE);
        
        logger.info("added readme.md file");
	}

	private void createRemoteBinaryRepository(File binaryRepoRoot,
			String binRemoteUrl) throws IOException, GitException {
		ZeusUtil.createRemoteRepository(binRemoteUrl);

        binaryRepository = new BinaryZeusRepository(new File(binaryRepoRoot, Constants.DOT_GIT));
        // add "remote" repository
        binaryRepository.addRemoteUrl(binRemoteUrl);
        binaryRepository.addRemoteBranch(Constants.MASTER_BRANCH);
	}
	
//	/**
//	 * add source repository's classes to binary repository.
//	 * 1. copy classes.
//	 * 2. addAll/commit/push
//	 * 
//	 * FIXME:
//	 * create new binary repo, needn't "mvn compile"?
//	 * 
//	 * @param binaryRepoRoot
//	 * @return
//	 * @throws IOException
//	 * @throws GitException
//	 */
//	private void addSrcRepoClassesToBinRepo(File binaryRepoRoot)
//			throws IOException, GitException {
//		logger.info("adding classes from source repository to binary repository...");
//		
//		// read the branch from "source" repository
//		String branchname = sourceRepository.getBranch();
//
//		// create a "branch"
//		if( !branchname.toLowerCase().equals(Constants.MASTER_BRANCH) ){
//			binaryRepository.checkoutNewBranch(branchname);
//		}
//
//		copyClassesFromSrcRepoToBinRepo(binaryRepoRoot);
//
//		binaryRepository.commitNDPushAll(sourceRepository.getHead());
//	}
	
//	/**
//	 * upload mapping data onto mapping service.
//	 * 
//	 * @param sourceRepoUrl
//	 * @param binRepoUrl
//	 * @param branchname
//	 * @throws Exception
//	 */
//	private void uploadMappingData(String sourceRepoUrl, String binRepoUrl, String branchname) throws Exception {
//		
//		final String sourceRepoHeadHash = sourceRepository.getHead();
//		final String binRepoHeadHash = binaryRepository.getHead();
//        final String binRepoBranchName = binaryRepository.getBranch();
//
//        logger.info("Updating Bin Repo Service with the new changes - POST new object to service");
//        
//   	    mappingServiceClient.post(sourceRepoUrl, branchname, sourceRepoHeadHash, binRepoUrl, binRepoBranchName, binRepoHeadHash);
//   	    
//   	    logger.info("Updated Bin Repo Service with the new changes");
//	}

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
			giturl = Constants.GITURL_BINARY_GIT_PREFIX + binaryRepoName + Constants.DOT_GIT;
		}else{
			giturl = Constants.GITURL_BINARY_SSH_PREFIX + binaryRepoName + Constants.DOT_GIT;
		}

		// calculate binary repository folder
		File binaryRepoFolder = ZeusUtil.getExistedBinaryRepositoryRoot(srcRepoRoot);

		logger.debug("gitUrl:"+giturl+"\n"+"binary repo root:"+binaryRepoFolder.getAbsolutePath());
		
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
	 * checkout specified branch for binary repository.
	 * it try to make binary repository ready for next step(like copy classes and push).
	 * if remote branch not existed, create it.
	 * 
	 * @param branchName
	 * @throws GitException
	 */
	private void checkoutBinaryBranch(String branchName) throws GitException {
		logger.info("checking out binary repository's branch:"+branchName+"...");
		
		branchName = GitUtils.getShortBranchName(branchName);
		
		if (branchName.toLowerCase().equals(Constants.MASTER_BRANCH)) {
			return;
		}
		
		// check whether the branch exists
		boolean isBranchExisted = binaryRepository.isBranchExisted(branchName);
		
		if( !isBranchExisted ){
			binaryRepository.checkoutNewBranch(branchName);
		}else{
			// check the current branch in binary repository
			try {
				if( !binaryRepository.getBranch().equals(branchName) ){
					binaryRepository.checkoutBranch(branchName);
				}
			} catch (IOException e) {
				throw new GitException("can't checkout remote branch("+branchName+") into local.", e);
			}
		}
	}

	 private List<String> notNeedProcessedCommits = new ArrayList<String>();
	 
	/**
	 * complete following works:
	 * 1. copy source repo's classes into binary repo.
	 * 2. push binary repo's changes into remote. 
	 * 
	 * 
	 * @throws Exception
	 */
    private void updateBinaryRepository() throws Exception {
    	logger.info("updating binary repository...");
    	notNeedProcessedCommits.clear();
    	
        final File binRepoRoot = ZeusUtil.getExistedBinaryRepositoryRoot(srcRepoRoot);
        
		logger.info("SourceRepository = " + srcRepoRoot.getCanonicalPath()
					+ "\nBinaryRepository = " + binRepoRoot.getCanonicalPath());

        List<String> allBranches = sourceRepository.getAllBranches();
        
        for (String branch:allBranches){
        	updateBinaryBranch(branch);
        }
        
		logger.debug("commit/pushed changes onto remote binary repo:"
					+ binaryRepository.getRemoteUrl());
        logger.info("Binary repository updated.");
    }
    
    /**
     * update specified branch by its source repo's compiled classes
     * 
     * @param branch
     */
	private void updateBinaryBranch(String branch) {
		logger.info("updating binary repository's branch:"+branch);
		
		try {
			sourceRepository.checkoutBranch(branch);
			
			checkoutBinaryBranch(branch);
	        RevCommit headCommit = binaryRepository.getHeadCommit();
	        
	        List<RevCommit> newCommits = sourceRepository.getNewCommits(headCommit);
	        
	        for (RevCommit commit:newCommits){
	        	if (needProcess(commit)){
	        		updateBinaryWithNewCommit(commit);
	        	}else{
	        		logger.debug("needn't handle this commit:"+commit.getName()+"--"+commit.getFullMessage());
	        	}
	        }
	        
	        //rollback to "master"
        	sourceRepository.checkoutBranch(Constants.MASTER_BRANCH);
        	binaryRepository.checkoutBranch(Constants.MASTER_BRANCH);
		} catch (Exception e) {
			//shouldn't break others branch.
			logger.error(e.getMessage(), e);
		}
        
        logger.info("updated binary repository's branch:"+branch);
	}

	/**
	 * if one commit has been processed, and prove to not need to be processed.
	 * then ignore this commit. 
	 * 
	 * @param commit
	 * @return
	 */
	private boolean needProcess(RevCommit commit) {
		return !notNeedProcessedCommits.contains(commit.getName());
	}

	/**
	 * update binary repo with specified commit
	 * 
	 * @param commit
	 * @throws GitException
	 */
	private void updateBinaryWithNewCommit(RevCommit commit) throws GitException {
		logger.info("updating binary repository with commit:"+commit.getFullMessage());
		
		final File binRepoRoot = ZeusUtil.getExistedBinaryRepositoryRoot(srcRepoRoot);
		
    	if (!binaryRepository.hasCommit(commit.getName())){
        	try {
        		//TODO: add precheck, whether changed files for this commit should be processed, if no, skip it.
//        		List<File> changedFiles = sourceRepository.getChangedFiles(commit);
        		
        		sourceRepository.reset(commit.getName());
        		
        		//TODO: should only compile changed files...
        		if (compile()){
        			//TODO: should only copy changed files instead of all of them.
    				copyClassesFromSrcRepoToBinRepo(binRepoRoot);
    				
    				//TODO: only take care UNTRACKED/MODIFIED cases, ignore DELETE or RENAME cases.
    				if (binaryRepository.getChangedFiles().size() > 0){
    					binaryRepository.commitNDPushAll(commit.getName());
    				}else{
    					logger.debug("Haven't found any changed files, needn't commit/push.");
    					notNeedProcessedCommits.add(commit.getName());
    				}
        		}else{
        			logger.debug("No pom file found in directory"+sourceRepository.getDirectory().getParent());
        			notNeedProcessedCommits.add(commit.getName());
        		}
        		
			} catch (Exception e) {
				//shouldn't break others commits
				logger.error(e.getMessage(), e);
				notNeedProcessedCommits.add(commit.getName());
			}finally{
				//rollback
	        	sourceRepository.pull();
			}
        }
    	
    	logger.info("updated binary repository with commit:"+commit.getFullMessage());
	}

	/**
	 * copy classes from source repo to binary repo, exclude "localobr"
	 * 
	 * @param binRepoRoot
	 * @throws IOException
	 */
	private void copyClassesFromSrcRepoToBinRepo(final File binRepoRoot)
			throws IOException {
		// find the "localobr" folders and exclude them during copy
		List<String> excludes = FileUtil.findExcludes(srcRepoRoot, "localobr");

		// copy the classes
		logger.info("copying binary files");
		FileUtil.copyBinaryFolders("target", excludes, sourceRepository.getDirectory(), binRepoRoot);
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
