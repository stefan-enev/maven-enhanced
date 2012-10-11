package com.ebay.maven.binaryrepository;

import com.ebay.beans.BinRepoBranchCommitDO;
import com.ebay.git.utils.GitUtils;
import com.ebay.utils.FileUtil;
import com.google.common.io.Files;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.util.FileUtils;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class BinaryRepository {

    private File root;
    private File destination;
	private FileRepository sourceRepository;
    private FileRepository binaryRepository;
    private String serviceUrl = null;
    
    private static Client client = Client.create();

    public static final String SVC_BASE_URL = "http://localhost:10000/services/repo";
    public static final String BINREPOSVC_FINDBY_REPOURL_BRANCH_COMMITID = "http://localhost:10000/services/repo/search/byrepourlbranchandcommitid/?";
    public static final String SVC_BASE = "/services/repo";
    public static final String SVC_FINDBY_REPO_BRANCH_COMMITID = "/byrepourlbranchandcommitid/?";
    public static final String UTF_8 = "UTF-8";

	public BinaryRepository(File root) throws IOException{

		if (root.canRead() && root.isDirectory()){

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
		// TODO: Check that the repository exists both locally and remotely
		return result && isRepoPresentInGit();
	}

    public boolean isRepoPresentInGit() {
        boolean b = false;
        try {
            b = FileUtil.existsInGit(getRepositoryName());
        } catch (IOException e) {
            // TODO: Log it
            e.printStackTrace();
        }
        return b;
    }

	public boolean isRemoteBinaryRepositoryAvailable(){

		boolean result = false;

		String repositoryName = getRepositoryName();

		// TODO: use github apis to check whether the repository is available
		result = true;

		return result;
	}

	public void createBinaryRepository() throws IOException, GitException{
        // check whether "binary repository" exists
		if (isBinaryRepositoryAvailable()) throw new GitException("Repository already exists");

		// find the name of the "source repository"
		String sourceRepoName = getRepositoryName();

        // find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		File sourceRepoFolder = f.getParentFile();

		String sourceRepoFolderName = f.getParentFile().getName();

		// calculate binary repository folder
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

	public void copyBinaryFolders(  String pattern,
									List<String> exclusionList,
									File destination )
											throws IOException{

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

	public void cloneBinaryRepository() {

		// find the name of the "source repository"
		String sourceRepoName = getRepositoryName();

		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		File sourceDir = f.getParentFile();

		String sourceRepoFolderName = f.getParentFile().getName();

		// construct the binary repository URL
		String giturl = "git@github.scm.corp.ebay.com:Binary/" + sourceRepoName + "_binary.git";

		// calculate binary repository folder
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + sourceRepoFolderName) );

		// read the branch from "source" repository
		String branchname = "master";
		try {
			branchname = sourceRepository.getBranch();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// clone the binary repository
		CloneCommand cloneCmd = Git.cloneRepository();
		cloneCmd.setURI( giturl );
		cloneCmd.setDirectory( binaryRepoFolder );
		cloneCmd.setCloneAllBranches(true);

		try {

			Git binaryRepository = cloneCmd.call();

			CheckoutCommand checkoutCmd = binaryRepository.checkout();

			checkoutCmd.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM );
			checkoutCmd.setName("origin/" + branchname);
			// TODO: checkout is not happening properly for a branch. fix it.
			Ref branch = checkoutCmd.call();

			CheckoutResult result = checkoutCmd.getResult();
			System.out.println( result.getStatus());

			// copy the .class files from binaryrepository to source-repository
			FileUtil.copyBinaryFolders( binaryRepoFolder, sourceDir, ".git");

		} catch (InvalidRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

    public void updateBinaryRepository() throws IOException, GitException {
        // 1. Check if repository exists remotely git@github.scm.corp.ebay.com/Binary/Repo_Binary.git
        // find the name of the "source repository"
        final String repoUrl = getSourceRemoteUrl();

        // find where ".git" folder is found
        // SourceRepository = D:\dev\devex\binrepo-devex
        // BinaryRepository = D:\dev\devex\.binrepo-devex
        final File srcRepoDir = sourceRepository.getDirectory();
        final File sourceDir = srcRepoDir.getParentFile();

        final String sourceRepoFolder = srcRepoDir.getParentFile().getCanonicalPath();
        System.out.println("SourceRepository = " + sourceRepoFolder);

        final File parent = srcRepoDir.getParentFile().getParentFile();
        final File binaryRepoDir = new File(parent, "." + srcRepoDir.getParentFile().getName());
        System.out.println("BinaryRepository = " + binaryRepoDir.getCanonicalPath());

        // 2. Get branch/commit hash for the source repo - the actual source code
        final org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepository(srcRepoDir);
        final String branch = repository.getBranch();
        System.out.println("SourceRepository = " + sourceRepository + " RepoUrl = " + repoUrl + " Branch = " + branch);

        final RevWalk revWalk = new RevWalk(repository);
        final ObjectId resolve = repository.resolve(Constants.HEAD);
        final RevCommit commit = revWalk.parseCommit(resolve);
        String commitHash = commit.getName(); // Can pass this instead of just using HEAD always
        System.out.println("CommitHash:" + commitHash + "\tMessage:" + commit.getFullMessage());

        // 3. Call the BinRepo service and check if a corresponding BinRepo entry exists
//        final String url = BINREPOSVC_FINDBY_REPOURL_BRANCH_COMMITID +
//                "repourl=" + URLEncoder.encode(repoUrl, UTF_8) + "&branch=" + URLEncoder.encode(branch, UTF_8) +
//                "&commitid=" + URLEncoder.encode(commitHash, UTF_8);
        
        final String url = getUrlForFindByRepoBranchCommit();
        WebResource webResource = client.resource(url);
        boolean noContent = false;
        BinRepoBranchCommitDO binRepoBranchCommitDO1 = null;
        try {
            binRepoBranchCommitDO1 = webResource.accept(MediaType.APPLICATION_JSON).get(BinRepoBranchCommitDO.class);
        } catch (UniformInterfaceException e) {
            int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
            System.out.println("Url HERE" + url + " Status Code HERE: " + statusCode);
            noContent = (statusCode == 204);
        } catch (Exception e) { // Catch-all to deal with network problems etc.
            e.printStackTrace();
        }
        System.out.println(binRepoBranchCommitDO1 != null ? binRepoBranchCommitDO1.toString() : "Resource not found on server");

        // 4. If not copy all the target folders from the source repo to the binary repo - root to root copy of artifacts
        if (noContent) {
            File src = new File(sourceDir, "target");
            File dest = new File(binaryRepoDir, "target");
            copyDirectory(src, dest);
        }

        // 5. Call git status to get the delta (Use StatusCommand and refine it)
        Git srcRepo = Git.open(srcRepoDir);
        Git binaryRepo = Git.open(binaryRepoDir);

        final ListBranchCommand listBranchCommand = binaryRepo.branchList();
        System.out.println(listBranchCommand.getRepository().getFullBranch());
        // get "status"
        final StatusCommand statusCommand = binaryRepo.status();
        Collection<String> filesToStage = GitUtils.getFilesToStage(statusCommand);
        for (String file : filesToStage) {
            System.out.println("File to be added:" + file);
        }

        // add files to "staging"
        if (filesToStage.size() > 0) {
            AddCommand addCmd = binaryRepo.add();
            for (String file : filesToStage) {
                addCmd.addFilepattern(file);
            }
            try {
                addCmd.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 6. Commit the changes to local and call push after that (use JGit API for this)
        // 6a. COmmit message should use format "Saving url:branch:commit:UTC time"
        // commit
        final CommitCommand commitCommand = binaryRepo.commit();
        String msg = "Saving Repo:%s Branch:%s CommitHash:%s Time:%s";
        final String formattedMsg = String.format(msg, repoUrl, branch, commitHash, new Date().toString());
        commitCommand.setMessage(formattedMsg);
        try {
            final RevCommit call = commitCommand.call();
            commitHash = call.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // push to origin now
        final PushCommand push = binaryRepo.push();
        final String remote = push.getRemote();
        System.out.println("Remote to push to:'" + remote + "'");
        try {
            // TODO: RGIROTI 10/09/2012 This is failing at times?????????
            final Iterable<PushResult> call = push.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 7. Call the BinRepo service and create a new entity for this change - repoUrl, branch, and commit
        System.out.println("Update Bin Repo Service with the new changes - PUT new object to service");
        final BinRepoBranchCommitDO binRepoBranchCommitDO = newInstance(repoUrl, branch, commitHash);
        webResource = client.resource(SVC_BASE_URL);

        BinRepoBranchCommitDO put = null;
        try {
            put = webResource.accept(MediaType.APPLICATION_JSON).put(BinRepoBranchCommitDO.class, binRepoBranchCommitDO);
        } catch (UniformInterfaceException e) {
            int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
            System.out.println("status code: " + statusCode);
        }
        System.out.println(put != null ? put.toString() : "Put was null");

    }

    private BinRepoBranchCommitDO newInstance(final String repoUrl, final String branch, final String commitHash) throws UnsupportedEncodingException {
        BinRepoBranchCommitDO binRepoBranchCommitDO = new BinRepoBranchCommitDO();
        binRepoBranchCommitDO.setRepoUrl(URLEncoder.encode(repoUrl, UTF_8));
        binRepoBranchCommitDO.setBranch(URLEncoder.encode(branch, UTF_8));
        binRepoBranchCommitDO.setCommitId(URLEncoder.encode(commitHash, UTF_8));
        binRepoBranchCommitDO.setBinRepoUrl(URLEncoder.encode("BIN_" + repoUrl, UTF_8));
        binRepoBranchCommitDO.setBinRepoBranch(URLEncoder.encode("BIN_" + branch, UTF_8));
        binRepoBranchCommitDO.setBinRepoCommitId(URLEncoder.encode("BIN_" + commitHash, UTF_8));
        return binRepoBranchCommitDO;
    }

    // Copies all files under srcDir to dstDir.
    private void copyDirectory(final File srcDir, final File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir(); // Create dstDir if required

            String[] children = srcDir.list();
            for (String child : children) {
                copyDirectory(new File(srcDir, child), new File(dstDir, child));
            }

        } else {
            Files.copy(srcDir, dstDir);
        }
    }

    public String getUrlForFindByRepoBranchCommit() throws UnsupportedEncodingException{
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append(getServiceUrl() );
    	sb.append("/");
    	sb.append( SVC_BASE );
    	sb.append("/");
    	sb.append(SVC_FINDBY_REPO_BRANCH_COMMITID );
    	
    	// TODO: remove "//" found anywhere in this string.
    	
    	String url = URLEncoder.encode(sb.toString(), UTF_8);
    	
    	return url;
    }
	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

}
