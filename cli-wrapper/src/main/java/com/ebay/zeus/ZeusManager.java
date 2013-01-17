package com.ebay.zeus;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.repository.BinaryRepositoryProcessor;
import com.ebay.zeus.repository.BinaryZeusRepository;
import com.ebay.zeus.repository.SourceRepositoryProcessor;
import com.ebay.zeus.repository.SourceZeusRepository;
import com.ebay.zeus.utils.Constants;
import com.ebay.zeus.utils.GitUtils;
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
    private boolean isRemoteBinaryRepoExisted = false;
    
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
			File sourceRepoRoot = sourceRepository.getDirectory().getParentFile();

			isRemoteBinaryRepoExisted = ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository.getRemoteUrl());
			
			File binaryRepoRoot = ZeusUtil.getExistedBinaryRepositoryRoot(sourceRepoRoot);
			
			isLocalBinaryRepoExisted = ZeusUtil.isLocalBinaryRepositoryExisted(sourceRepoRoot);
			if (isRemoteBinaryRepoExisted && isLocalBinaryRepoExisted) {
				this.binaryRepository = new BinaryZeusRepository(new File(binaryRepoRoot, Constants.DOT_GIT));
			}
			
			if (!isRemoteBinaryRepoExisted && isLocalBinaryRepoExisted){
				logger.info("remote binary repository not existed, but local repository exists, delete it.");
				FileUtils.deleteDirectory(binaryRepoRoot);
			}
			
			srcRepoRoot = sourceRepository.getDirectory().getParentFile();
			
			logger.info("initialized basic information.");
		} catch (IOException e) {
			throw new GitException("Fail to initialize source repository or binary repository.", e);
		}
	}
	
	public void createOrUpdateBinaryRepository() throws GitException {
		logger.info("starting to create/update binary repository");
		
		// assume the current directory the "root" of the project
		try {
			
			if (sourceRepository.getBranch().equals(sourceRepository.getFullBranch())){
				//current branch is detached.
				sourceRepository.checkoutBranch(Constants.MASTER_BRANCH);
			}
			
			sourceRepository.pull();
			
			long starttime = 0l;
			long endtime = 0l;

			if (isRemoteBinaryRepoExisted && isLocalBinaryRepoExisted) {
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
					this.binaryRepository = ZeusUtil.cloneBinaryRepository(false, sourceRepository);

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

		// TODO: remove this after implementing above steps. this is
		// temporary
		// get the latest by "git pull" on "source" and "binary".
		gitpull();

		// update binary repo
		processBinaryRepository();
		
		logger.info("Updated existed binary repository");
	}
	
	/**
	 * setup project, it will try to get pre-compiled classes
	 * and copy them into source repository's target folders.
	 * 
	 * @throws GitException
	 * @throws IOException
	 */
	public void setupProject() throws Exception {
		SourceRepositoryProcessor processor = new SourceRepositoryProcessor(sourceRepository, binaryRepository);
		processor.process();
	}
	
	/**
	 * Pull latest from 'source' and 'binary' repository.
	 * @throws GitException 
	 */
	private void gitpull() throws GitException{
		logger.info("Pulling source repository & binary repository...");
		
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

        this.binaryRepository = ZeusUtil.createRemoteBinaryRepository(binaryRepoRoot, sourceRepository.getRemoteUrl());
		addReadmeForNewBinaryRepo(binaryRepoRoot);
		
		processBinaryRepository();
        
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

	/**
	 * Call processor to update binary repository.
	 * 
	 * @throws Exception
	 */
    private void processBinaryRepository() throws Exception {
		BinaryRepositoryProcessor processor = new BinaryRepositoryProcessor(sourceRepository, binaryRepository);
		processor.process();
		gitpull();
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
