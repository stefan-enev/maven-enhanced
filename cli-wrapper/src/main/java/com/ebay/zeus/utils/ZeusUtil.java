package com.ebay.zeus.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.github.GitHubClient;
import com.ebay.zeus.repository.BinaryZeusRepository;
import com.ebay.zeus.repository.SourceZeusRepository;

public class ZeusUtil {
	public final static Logger logger = LoggerFactory.getLogger(ZeusUtil.class);
	
	/**
	 * create readme file in local binary repository.
	 * 
	 * @param binaryRepoFolder
	 * @param sourceRepoUrl
	 * @throws IOException
	 */
    public static void createReadMeFile(final File binaryRepoFolder, String sourceRepoUrl) throws IOException {
        // add a "README.md" file and commit
        final File readmeFile = new File(binaryRepoFolder, "README.md");
        final List<String> readmeContent = new ArrayList<String>();
        readmeContent.add("Binary Repository For " + sourceRepoUrl);
        readmeContent.add("=======================================================");
        readmeContent.add("");
        readmeContent.add("Stores the class files for the above source repository");
        org.apache.commons.io.FileUtils.writeLines(readmeFile, readmeContent, "\n");
    }
   
    /**
     * 
     * @param binaryRepoRoot
     * @param binRemoteUrl
     * @return
     * @throws IOException
     * @throws GitException
     */
	public static BinaryZeusRepository createRemoteBinaryRepository(File binaryRepoRoot,
			String srcRemoteUrl) throws IOException, GitException {
		String binRemoteUrl = calculateBinaryRepositoryUrl(srcRemoteUrl);
		createRemoteRepository(binRemoteUrl);

		BinaryZeusRepository binaryRepository = new BinaryZeusRepository(new File(binaryRepoRoot, Constants.DOT_GIT));
        // add "remote" repository
        binaryRepository.addRemoteUrl(binRemoteUrl);
        binaryRepository.addRemoteBranch(Constants.MASTER_BRANCH);
        
        return binaryRepository;
	}
	
    /**
     * create new remote git repository with speicified git url.
     * if has existed remote repository, do nothing. 
     * 
     * @param remoteUrl
     * @throws IOException
     */
	public static void createRemoteRepository(String remoteUrl) throws IOException {
		GitHub github = new GitHubClient().getGithub();
        GHOrganization githubOrg = github.getOrganization(Constants.ORGNAME_BINARY);
        GHRepository repository = githubOrg.getRepository( GitUtils.getRepositoryName(remoteUrl) );

        if (repository == null ) {
        	logger.info("creating remote repository : " + remoteUrl);
        	
            GHRepository repo = githubOrg.createRepository(GitUtils.getRepositoryName(remoteUrl), 
            												"Binary repository", 
            												Constants.GITHUB_BASE_URL, 
            												Constants.OWNERS, 
            												true);
            
            logger.info(repo.getUrl() + " created successfully ");
        }
	}
	
	private static final long ONE_MONTH = 1000L * 60 * 60 * 24 * 30;
//	private static final long THREE_MONTH = 1000L * 60 * 60 * 24 * 30 * 3;
	
	public static List<String> getActiveBranches(SourceZeusRepository srcRepo, File BinaryRepoRoot) throws Exception {
		logger.info("getting active branch list...");
		
		List<String> activeBranches = new ArrayList<String>();
		activeBranches.add(Constants.MASTER_BRANCH);
		
		GHRepository repository = getGHRepository(srcRepo.getRemoteUrl());
        
		if (repository == null){
			throw new GitException("cat's find repository from github server:"+srcRepo.getRemoteUrl());
		}
		
		Map<String, GHBranch> allBranches = repository.getBranches();
		for (String branch:allBranches.keySet()){
			
			RevCommit commit = srcRepo.getHeadCommit(branch);
			long commitTime = commit.getCommitTime() * 1000L;
			long sinceDate = System.currentTimeMillis() - ONE_MONTH; //ONE MONTH ago
			if (commitTime > sinceDate && !activeBranches.contains(branch)){
				activeBranches.add(branch);
				logger.debug("active branch:"+branch);
			}
			
		}
		
		return activeBranches;
	}

	public static GHRepository getGHRepository(String remoteUrl)
			throws IOException, GitException {
		GitHub github = new GitHubClient().getGithub();
		GHOrganization githubOrg = null;
		GHRepository repository = null;
		try{
			githubOrg = github.getOrganization(GitUtils.getOrgName(remoteUrl));
			repository = githubOrg.getRepository( GitUtils.getRepositoryName(remoteUrl) );
		}catch(Exception e){
			//should try user/repoName
			repository = github.getRepository(GitUtils.getOrgName(remoteUrl)+"/"+GitUtils.getRepositoryName(remoteUrl));
		}
		
		if (repository == null){
			throw new GitException("cat's find repository from github server:"+remoteUrl);
		}
		
		return repository;
	}
	
	public static boolean isExistedBranchCommit(String remoteUrl, String branch, String commitSHA1) throws Exception{
		logger.debug("checking whether specified branch & commit existed in binary repository...");
		TimeTracker tracker = new TimeTracker();
		
		tracker.start();
		if (!isExistedBranch(remoteUrl, branch)){
			return false;
		}
		
		boolean ret = isExistedCommit(remoteUrl, commitSHA1);
		
		tracker.stop();
		logger.debug("Checking takes "+ tracker.getDurationString());
		
		return ret;
	}
	
	public static boolean isExistedBranch(String remoteUrl, String branch) throws Exception{
		GHRepository repository = getGHRepository(remoteUrl);
        
		try{
			GHBranch ghBranch = repository.getBranch(branch);
			if (ghBranch == null){
				logger.warn("Haven't find any branch in github server:"+branch);
				return false;
			}
		}catch(Exception e){
			logger.error("Encunter exception when trying to get branch:"+branch, e);
			return false;
		}
		return true;
	}
	
	public static boolean isExistedCommit(String remoteUrl, String commitSHA1) throws Exception{
		GHRepository repository = getGHRepository(remoteUrl);
        
		try{
			List<GHCommit> ghCommits = repository.getCommits();
			if (ghCommits != null && ghCommits.size() == 0){
				logger.warn("Haven't find any commit in binary repository.");
				return false;
			}
			
			for (GHCommit ghCommit:ghCommits){
				if (commitSHA1.equals(ghCommit.getCommitMessage())){
					return true;
				}
			}
			
		}catch(Exception e){
			logger.error("Encunter exception when trying to get specific commit:"+commitSHA1, e);
			return false;
		}
		
		logger.warn("Haven't find specified commit in binary repository.");
		return false;
	}
	
	/**
	 * if local binary repository not existed, clone binary repository from remote.
	 * 
	 * @param readonly
	 * @throws GitException
	 */
	public static BinaryZeusRepository cloneBinaryRepository( boolean readonly, SourceZeusRepository sourceRepository) throws Exception {
		logger.info("cloning binary repository....");
		
		// find the name of the "source repository"
		String giturl = getBinaryRemoteUrl(readonly, sourceRepository);

		// calculate binary repository folder
		File binaryRepoFolder = ZeusUtil.getBinaryRepositoryRoot(sourceRepository.getDirectory().getParentFile());

		logger.debug("gitUrl:"+giturl+"\n"+"binary repo root:"+binaryRepoFolder.getAbsolutePath());
		
		// clone the binary repository
		Git binGit = GitUtils.cloneRepository(giturl, binaryRepoFolder, sourceRepository.getBranch());
		
		logger.info("binary repository cloned");
		
		try {
			return new BinaryZeusRepository(binGit.getRepository().getDirectory());
		} catch (IOException e) {
			throw new GitException("Fail to initialize BinaryZeusRepository.", e);
		}
		
    }

	public static String getBinaryRemoteUrl(boolean readonly, SourceZeusRepository sourceRepository) {
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
		return giturl;
	}
	
	/**
	 * check whether local & remote repository existed or not.
	 * 
	 * @param sourceRepository
	 * @return
	 * @throws GitException
	 */
	public static boolean isBinaryRepositoryExisted(SourceZeusRepository sourceRepository) throws GitException{
        boolean result = isLocalBinaryRepositoryExisted(sourceRepository.getDirectory());
		
		// return result && isRepoPresentInGit();
        boolean remoteRepoCheck = false;
        remoteRepoCheck = isRemoteBinaryRepositoryExisted(sourceRepository.getRemoteUrl());
        
        return result && remoteRepoCheck;
	}
	
	/**
	 * whether local binary repository existed according to source repository 
	 * 
	 * @param sourceRepoDir
	 * @return
	 */
	public static boolean isLocalBinaryRepositoryExisted(File sourceRepoDir) {
		File binaryRepoRoot = getExistedBinaryRepositoryRoot(sourceRepoDir);
		if (binaryRepoRoot == null){
			return false;
		}
		
		return true;
	}
	
	/**
	 * get binary repository's root according to source repository's root
	 * if not existed in disk, return null.
	 * 
	 * @param sourceRepoDir
	 * @return
	 */
	public static File getExistedBinaryRepositoryRoot(File sourceRepoDir) {
        
		File binaryRepoFolder = getBinaryRepositoryRoot(sourceRepoDir);
		
        // check whether ".SourceRepo.git" folder exists
		if (isLocalRepoExisted(binaryRepoFolder)){
			return binaryRepoFolder;
		}
		
		return null;
	}

	/**
	 * check whether specified git repo existed or not.
	 * 
	 * @param repoRoot : git repo's root directory
	 * @return
	 */
	public static boolean isLocalRepoExisted(File repoRoot) {
		if (repoRoot.exists() && repoRoot.isDirectory() && repoRoot.canRead()) {
            // check whether ".SourceRepo.git/.git" exists
			File binGit = new File(repoRoot, Constants.DOT_GIT);
			if( binGit.exists() && binGit.isDirectory() && binGit.canRead() ){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * simply get binary repository's root according to source repository's root.
	 * won't do any check.
	 * 
	 * @param sourceRepoDir
	 * @return
	 */
	public static File getBinaryRepositoryRoot(File sourceRepoDir) {
		// repository foldername
		String repositoryFolderName = sourceRepoDir.getName();
        
		// go to parent directory
		File parent = sourceRepoDir.getParentFile();
		return new File( parent , ( "." + repositoryFolderName) );
	}
	
	/**
	 * check whether remote binary repository existed according to source repository url.
	 * it will use GitHub API to do this check.
	 * 
	 * @param sourceRepoUrl
	 * @return
	 * @throws GitException
	 */
    public static boolean isRemoteBinaryRepositoryExisted(String sourceRepoUrl) throws GitException {
        boolean result = false;
		
		String org = GitUtils.getOrgName(sourceRepoUrl);
		String repoName = GitUtils.getRepositoryName(sourceRepoUrl);
		
		String binaryRepoName = calculateBinaryRepositoryName(org, repoName);
		
		// TODO: use github apis to check whether the repository is available
		try {
			GitHub github = new GitHubClient().getGithub();
			GHOrganization ghOrg = github.getOrganization(Constants.ORGNAME_BINARY);
			GHRepository repository = ghOrg.getRepository(binaryRepoName);
			
			if (repository != null ){
				// TODO: any additional check needs to be done.
				result = true;
			}
		} catch (IOException e) {
			throw new GitException("unable to query the repository: " + binaryRepoName , e);
		}

		return result;
	}
    
    public static String calculateBinaryRepositoryName(String org , String repoName ){
    	return org + "_" + repoName + "_" + Constants.ORGNAME_BINARY_LOWERCASE;
    }
    
    /**
     * get binary repository's url by source repository's remote url.
     * 
     * @param srcRemoteUrl
     * @return
     */
    public static String calculateBinaryRepositoryUrl(String srcRemoteUrl){
    	String remoteUrl=null;
    	String srcUrl = srcRemoteUrl;
    	if (srcUrl.contains(Constants.EBAY_DOMAIN_SUFFIX) ) {
    		String org = GitUtils.getOrgName(srcUrl);
    		String repoName = GitUtils.getRepositoryName(srcUrl);
			remoteUrl = Constants.GITURL_BINARY_SSH_PREFIX
					+ ZeusUtil.calculateBinaryRepositoryName(org, repoName)
					+ Constants.DOT_GIT;
    	} else {
    		
    	}
    	return remoteUrl;
    }
    
//    /**
//     * check whether binary repo's changed files contains source repo's changed files.
//     * 
//     * @param binChangedFiles
//     * @param srcChangedFiles
//     * @return
//     * @throws GitException
//     */
//	public static boolean containsSourceChanges(List<String> binChangedFiles, List<File> srcChangedFiles) throws GitException{
//		for (File file:srcChangedFiles){
//			String srcFileName = file.getName();
//			if (srcFileName.endsWith(".java")){
//				srcFileName = srcFileName.substring(0, srcFileName.length()-5)+".class";
//			}
//			
//			if (containsFile(binChangedFiles, srcFileName)){
//				return true;
//			}
//		}
//		
//		return false;
//	}
//
//	/**
//	 * check whether files contains specified fileName.
//	 * 
//	 * @param files
//	 * @param fileName
//	 * @return
//	 * @throws GitException
//	 */
//	public static boolean containsFile(List<String> files, String fileName) throws GitException {
//		for (String filePath:files){
//			if (filePath.contains(fileName)){
//				return true;
//			}
//		}
//		
//		return false;
//	}
}
