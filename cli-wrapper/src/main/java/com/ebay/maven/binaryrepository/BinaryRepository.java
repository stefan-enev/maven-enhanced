package com.ebay.maven.binaryrepository;

import com.ebay.beans.BinRepoBranchCommitDO;
import com.ebay.git.utils.GitUtils;
import com.ebay.github.client.GitHubClient;
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
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.util.FileUtils;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import javax.ws.rs.core.MediaType;
import java.io.*;
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
    private String baseServiceUrl = null;
    private GitHubClient ghClient = null;
    
    private static Client client = Client.create();

    public static final String SVC_BASE = "services/repo";
    public static final String SVC_FINDBY_REPO_BRANCH_COMMITID = "search/byrepourlbranchandcommitid/?";
    public static final String UTF_8 = "UTF-8";

	public BinaryRepository(File root) throws IOException {
        if (root.canRead() && root.isDirectory()){
            this.root = root;
            // get the repository name.
			FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
			this.sourceRepository = repobuiler.findGitDir(root).build();
			
			ghClient = new GitHubClient();
        } else{
			// TODO: throw exception
		}
    }

	public String getRepositoryName() {
		String remoteUrl = sourceRepository.getConfig().getString("remote", "origin", "url");
		String repository = GitUtils.getRepositoryName(remoteUrl);
		return repository;
	}

	public String getSourceRemoteUrl() {
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
		if (binaryRepoFolder.exists() && binaryRepoFolder.isDirectory() && binaryRepoFolder.canRead()) {
            // check whether ".SourceRepo.git/.git" exists
			File binGit = new File(binaryRepoFolder, ".git");
			if( binGit.exists() && binGit.isDirectory() && binGit.canRead() ){
				result = true;
			}
		}
		// return result && isRepoPresentInGit();
        boolean remoteRepoCheck = false;
        try {
            remoteRepoCheck = isRemoteBinaryRepositoryAvailable();
        } catch (GitException e) {
            e.printStackTrace();
        }
        return result && remoteRepoCheck;
	}

    public boolean isRemoteBinaryRepositoryAvailable() throws GitException {
        boolean result = false;

		String srcRepoUrl = getSourceRemoteUrl();
		String org = GitUtils.getOrgName(srcRepoUrl);
		String repoName = GitUtils.getRepositoryName(srcRepoUrl);
		
		String binaryRepoName = calculateBinaryRepositoryName(org, repoName);
		
		// TODO: use github apis to check whether the repository is available
		try {
			GHOrganization ghOrg = ghClient.getGithub().getOrganization("Binary");
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

	public void createBinaryRepository() throws IOException, GitException {
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

        createReadMeFile(binaryRepoFolder);

        // get "status"
		StatusCommand statusC = binaryRepo.status();
		Collection<String> toadd = GitUtils.getFilesToStage(statusC);

		// add "readme" file to staging
		if (toadd.size() > 0) {
			AddCommand add = binaryRepo.add();
			for( String file : toadd ){
				add.addFilepattern(file);
			}
            try {
                add.call();
                CommitCommand commit = binaryRepo.commit();
				commit.setMessage("initial commit");
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
		
		// Calculate the remote url for binary repository
		String remoteUrl = calculateBinaryRepositoryUrl();
		
		// TODO: check whether the remote exists, if not create it, else fail
		GitHub github = new GitHubClient().getGithub();
		GHOrganization githubOrg = github.getOrganization("Binary");
		GHRepository repository = githubOrg.getRepository( GitUtils.getRepositoryName(remoteUrl) );
		
		if (repository == null ) {
			GHRepository repo = githubOrg.createRepository(GitUtils.getRepositoryName(remoteUrl), "Binary repository", "https://github.scm.corp.ebay.com", "Owners", true);
		} else {
			// fail, it shouldn't come here
		}
		
		// add "remote" repository
		StoredConfig config = binaryRepo.getRepository().getConfig();
		config.setString("remote", "origin", "url", remoteUrl);
		config.save();
		
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

		// push
		PushCommand pushCmd = binaryRepo.push();
		try {
			pushCmd.call();
		} catch (InvalidRemoteException e) {
			throw new GitException("unable to push", e);
		} catch (TransportException e) {
			throw new GitException("unable to push", e);
		} catch (GitAPIException e) {
			throw new GitException("unable to push", e);
		}

        final String repoUrl = getSourceRemoteUrl();
        // branchName was computed above
        final org.eclipse.jgit.lib.Repository repo = new org.eclipse.jgit.storage.file.FileRepository(f);
        final RevWalk revWalk = new RevWalk(repo);
        final ObjectId resolve = repo.resolve(Constants.HEAD);
        final RevCommit commitRev = revWalk.parseCommit(resolve);
        final String commitHash = commitRev.getName();

        Git git = Git.open(binaryRepoFolder);
        final RevCommit binRepoResolveCommitRev;
        try {
            binRepoResolveCommitRev = git.log().call().iterator().next();
        } catch (NoHeadException e) {
            throw new GitException("No head found for repo", e);
        } catch (GitAPIException e) {
            throw new GitException("No head found for repo", e);
        }
        final String binRepoResolveCommitHash = binRepoResolveCommitRev.getName();
        final String binRepoBranchName = git.getRepository().getBranch();

        System.out.println("Update Bin Repo Service with the new changes - POST new object to service");
        final BinRepoBranchCommitDO binRepoBranchCommitDO = newInstance(repoUrl, branchname, commitHash, remoteUrl, binRepoBranchName, binRepoResolveCommitHash);
        final WebResource resource = client.resource(getUrlForPost());
        BinRepoBranchCommitDO postedDO = null;

        try {
            postedDO = resource.accept(MediaType.APPLICATION_JSON).post(BinRepoBranchCommitDO.class, binRepoBranchCommitDO);
        } catch (UniformInterfaceException e) {
            int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
            System.out.println("status code: " + statusCode);
        }
        System.out.println(postedDO != null ? postedDO.toString() : "postedDO was null");

    }

    private void createReadMeFile(final File binaryRepoFolder) throws IOException {
        // add a "README.md" file and commit
        final File readmeFile = new File(binaryRepoFolder, "README.md");
        final List<String> readmeContent = new ArrayList<String>();
        readmeContent.add("Binary Repository For " + getSourceRemoteUrl() );
        readmeContent.add("=======================================================");
        readmeContent.add("");
        readmeContent.add("Stores the class files for the above source repository");
        org.apache.commons.io.FileUtils.writeLines(readmeFile, readmeContent, "\n");
    }

    public void copyBinaryFolders(String pattern, List<String> exclusionList, File destination) throws IOException {
        File root = sourceRepository.getDirectory().getParentFile();
        Collection<File> files = FileUtil.findDirectoriesThatEndWith(root, pattern);

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return true;
			}
		};

        int pathlength = root.getCanonicalPath().length();
        for (File f : files) {
            // construct the directory to copied
			if (f.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                // get the path that is additional
				String pathfraction  = f.getCanonicalPath().substring(pathlength);
                File d = new File(destination, pathfraction );
                System.out.println( "copying " + f.getCanonicalPath() + " to " + d.getAbsolutePath() );
				FileUtil.doCopyDirectory(f, d, filter, true, exclusionList);
			}
		}
	}

	public void cloneBinaryRepository() throws GitException {

		// find the name of the "source repository"
		String srcRepoUrl = getSourceRemoteUrl();
		String org = GitUtils.getOrgName(srcRepoUrl);
		String repoName = GitUtils.getRepositoryName(srcRepoUrl);
		String binaryRepoName = calculateBinaryRepositoryName(org, repoName);
		

		// find where ".git" folder is found
		File f = sourceRepository.getDirectory();
		File sourceDir = f.getParentFile();

		String sourceRepoFolderName = f.getParentFile().getName();

		// construct the binary repository URL
		String giturl = "git@github.scm.corp.ebay.com:Binary/" + binaryRepoName + ".git";

		// calculate binary repository folder
		File parent = f.getParentFile().getParentFile();
		File binaryRepoFolder = new File( parent , ( "." + sourceRepoFolderName) );

		// read the branch from "source" repository
		String branchName = "master";
		try {
			branchName = sourceRepository.getBranch();
		} catch (IOException e) {
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
			checkoutCmd.setName("origin/" + branchName);
			// TODO: checkout is not happening properly for a branch. fix it.
			Ref branch = checkoutCmd.call();

			CheckoutResult result = checkoutCmd.getResult();
			//System.out.println( result.getStatus());
			
			// TODO: find out whether Binary is up-to-date with the sources
			//       call the MapSvc to find it out.
            final org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepository(f);
            final RevWalk revWalk = new RevWalk(repository);
            final ObjectId resolve = repository.resolve(Constants.HEAD);
            final RevCommit commit = revWalk.parseCommit(resolve);
            final String commitHash = commit.getName();
            final String url = getUrlForFindByRepoBranchCommit() + "repourl=" + URLEncoder.encode(getSourceRemoteUrl(), UTF_8) +
                    "&branch=" + URLEncoder.encode(branchName, UTF_8) + "&commitid=" + URLEncoder.encode(commitHash, UTF_8);

            final WebResource webResource = client.resource(url);
            boolean noContent = false;
            BinRepoBranchCommitDO binRepoBranchCommitDO = null;
            try {
                binRepoBranchCommitDO = webResource.accept(MediaType.APPLICATION_JSON_TYPE).get(BinRepoBranchCommitDO.class);
            } catch (UniformInterfaceException e) {
                int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
                noContent = (statusCode == 204);
            } catch (Exception e) { // catch-all in case there are network problems
                e.printStackTrace();
            }
            // No matching entry found in mapping service
            // TODO: RGIROTI Talk to Nambi and find out what we want to do in this case
            if (noContent) {

            } else {
                // if it matches copy the .class files from binaryrepository to source-repository
                if (binRepoBranchCommitDO != null &&
                        binRepoBranchCommitDO.getRepoUrl().equalsIgnoreCase(getSourceRemoteUrl()) &&
                        binRepoBranchCommitDO.getBranch().equalsIgnoreCase(branchName) &&
                        binRepoBranchCommitDO.getCommitId().equalsIgnoreCase(commitHash)) {
                    FileUtil.copyBinaryFolders(binaryRepoFolder, sourceDir, ".git");
                }
            }
        } catch (InvalidRemoteException e) {
			throw new GitException("unable to clone " + giturl, e);
		} catch (TransportException e) {
			throw new GitException("unable to clone " + giturl, e);
		} catch (GitAPIException e) {
			throw new GitException("unable to clone " + giturl, e);
		} catch (IOException e) {
			throw new GitException("unable to clone " + giturl, e);
		}
    }

    public void updateBinaryRepository() throws IOException, GitException {
        // 1. Check if repository exists remotely git@github.scm.corp.ebay.com/Binary/Repo_Binary.git
        // find the name of the "source repository"
        final String repoUrl = getSourceRemoteUrl();

        // find where ".git" folder is found  // SourceRepository = D:\dev\devex\binrepo-devex // BinaryRepository = D:\dev\devex\.binrepo-devex
        final File srcRepoDir = sourceRepository.getDirectory();
        final File sourceDir = srcRepoDir.getParentFile();

        final String sourceRepoFolder = srcRepoDir.getParentFile().getCanonicalPath();

        final File parent = srcRepoDir.getParentFile().getParentFile();
        final File binaryRepoDir = new File(parent, "." + srcRepoDir.getParentFile().getName());
        System.out.println("SourceRepository = " + sourceRepoFolder + "\nBinaryRepository = " + binaryRepoDir.getCanonicalPath());

        // 2. Get branch/commit hash for the source repo - the actual source code
        final org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepository(srcRepoDir);
        final String branch = repository.getBranch();
        System.out.println("SourceRepository = " + sourceRepository + " RepoUrl = " + repoUrl + " Branch = " + branch);

        final RevWalk revWalk = new RevWalk(repository);
        final ObjectId resolve = repository.resolve(Constants.HEAD);
        final RevCommit commit = revWalk.parseCommit(resolve);
        String commitHash = commit.getName();
        System.out.println("CommitHash:" + commitHash + "\tMessage:" + commit.getFullMessage());

        // 3. Call the BinRepo service and check if a corresponding BinRepo entry exists
        final String url = getUrlForFindByRepoBranchCommit() + "repourl=" + URLEncoder.encode(repoUrl, UTF_8) + "&branch=" +
                URLEncoder.encode(branch, UTF_8) + "&commitid=" + URLEncoder.encode(commitHash, UTF_8);
        System.out.println("svc url : " + url);
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
            System.out.println("Source Directory:'" + sourceDir.getCanonicalPath() + "' Destination Directory:'" + binaryRepoDir.getCanonicalPath() + "'");
            /*File src = new File(sourceDir, "target");
            File dest = new File(binaryRepoDir, "target");
            copyDirectory(src, dest);*/
            copyDirectory2(sourceDir, binaryRepoDir);
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

        // add files to "staging" - if there is nothing to stage none of the other operations make any sense at all
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

            // 6. Commit the changes to local and call push after that (use JGit API for this)
            // 6a. COmmit message should use format "Saving url:branch:commit:UTC time"
            // commit
            final CommitCommand commitCommand = binaryRepo.commit();
            String msg = "Saving Repo:%s Branch:%s CommitHash:%s Time:%s";
            final String formattedMsg = String.format(msg, repoUrl, branch, commitHash, new Date().toString());
            commitCommand.setMessage(formattedMsg);
            String commitHashBinRepo = null;
            try {
                final RevCommit call = commitCommand.call();
                commitHashBinRepo = call.getName();      // Got this
            } catch (Exception e) {
                e.printStackTrace();
            }

            // push to origin now
            final PushCommand push = binaryRepo.push();
            final String remote = push.getRemote();
            final String remoteBranch = push.getRepository().getBranch();
            System.out.println("Remote to push to:'" + remote + "'");
            try {
                final Iterable<PushResult> call = push.call();
            } catch (Exception e) {
                e.printStackTrace();
            }


            // Calculate the remote url for binary repository
            String binRepoUrl = calculateBinaryRepositoryUrl();  // Got this

            // 7. Call the BinRepo service and create a new entity for this change - repoUrl, branch, and commit
            System.out.println("Update Bin Repo Service with the new changes - POST new object to service");
            // final BinRepoBranchCommitDO binRepoBranchCommitDO = newInstance(repoUrl, branch, commitHash);
            final BinRepoBranchCommitDO binRepoBranchCommitDO = newInstance(repoUrl, branch, commitHash, binRepoUrl, remoteBranch, commitHashBinRepo);
            webResource = client.resource(getUrlForPost());

            BinRepoBranchCommitDO postedDO = null;
            try {
                postedDO = webResource.accept(MediaType.APPLICATION_JSON).post(BinRepoBranchCommitDO.class, binRepoBranchCommitDO);
            } catch (UniformInterfaceException e) {
                int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
                System.out.println("status code: " + statusCode);
            }
            System.out.println(postedDO != null ? postedDO.toString() : "Put was null");
        }
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

    private BinRepoBranchCommitDO newInstance(final String repoUrl, final String branch, final String commitHash,
                                              final String binRepoUrl, final String binRepoBranch, final String binRepoCommitHash) throws UnsupportedEncodingException {
        BinRepoBranchCommitDO binRepoBranchCommitDO = new BinRepoBranchCommitDO();
        binRepoBranchCommitDO.setRepoUrl(URLEncoder.encode(repoUrl, UTF_8));
        binRepoBranchCommitDO.setBranch(URLEncoder.encode(branch, UTF_8));
        binRepoBranchCommitDO.setCommitId(URLEncoder.encode(commitHash, UTF_8));
        binRepoBranchCommitDO.setBinRepoUrl(URLEncoder.encode(binRepoUrl, UTF_8));
        binRepoBranchCommitDO.setBinRepoBranch(URLEncoder.encode(binRepoBranch, UTF_8));
        binRepoBranchCommitDO.setBinRepoCommitId(URLEncoder.encode(binRepoCommitHash, UTF_8));
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

    private FileFilter pathFilter = new FileFilter() {
        public boolean accept(File dir) {
            boolean temp = false;
            try {
                temp = dir.getCanonicalPath().contains("target");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return temp;
        }
    };

    // Copies all files under srcDir to dstDir.
    private void copyDirectory2(final File srcDir, final File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir(); // Create dstDir if required

            final File[] files = srcDir.listFiles();
            for (File file : files) {
                copyDirectory3(file, new File(dstDir, file.getName()));
            }
        }
    }

    private void copyDirectory3(final File srcDir, final File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir(); // Create dstDir if required

            final File[] files = srcDir.listFiles(pathFilter);
            for (File file : files) {
                copyDirectory3(file, new File(dstDir, file.getName()));
            }


        } else {
            Files.copy(srcDir, dstDir);
        }
    }

    public String getUrlForFindByRepoBranchCommit() throws UnsupportedEncodingException{
    	StringBuilder sb = new StringBuilder();
        sb.append(getBaseServiceUrl() );
    	sb.append("/");
    	sb.append( SVC_BASE );
    	sb.append("/");
    	sb.append(SVC_FINDBY_REPO_BRANCH_COMMITID );
        // TODO: Convert any "//" to "/" Look at main()
    	return sb.toString();
    }
    
    public String getUrlForPost() throws UnsupportedEncodingException{
    	StringBuilder sb = new StringBuilder();
        sb.append(getBaseServiceUrl() );
    	sb.append("/");
    	sb.append( SVC_BASE );
        return sb.toString();
    }
    
    public String calculateBinaryRepositoryUrl(){
    	String remoteUrl=null;
    	String srcUrl = getSourceRemoteUrl();
    	if( srcUrl.contains("scm.corp.ebay.com")){
    		String org = GitUtils.getOrgName(srcUrl);
    		String repoName = GitUtils.getRepositoryName(srcUrl);
    		remoteUrl = "git@github.scm.corp.ebay.com:Binary/" +  calculateBinaryRepositoryName(org, repoName) + ".git";
    	}else{
    		
    	}
    	return remoteUrl;
    }
    
    public String calculateBinaryRepositoryName(String org , String repoName ){
    	return org + "_" + repoName + "_binary";
    }
    
	public String getBaseServiceUrl() {
		return baseServiceUrl;
	}

	public void setBaseServiceUrl(String serviceUrl) {
		this.baseServiceUrl = serviceUrl;
	}


    public static void main(String[] args) {
        String input = "hello://world//of//Java//Python_is_good_too";
        // TODO: FIX this regex to escape the double slashes
        System.out.println(input.replaceAll("[^://]/{2}", "/"));
    }

}
