package com.ebay.github.client;

import com.ebay.git.utils.GitUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Test;
import org.kohsuke.github.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GitHubClientTest {
	
	@Test
	public void listRepos(){
		try {
			GitHubClient client = new GitHubClient();
			client.listRepositories("Binary");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    // 2. Get branch/commit hash for the source repo - the actual source code
    @Test
    public void existsInGit() throws IOException {
        final String repo = "binrepo-devex";
       //  final String repo = "search_raptor_binary";
        String githubUrl = "https://github.scm.corp.ebay.com/api/v3/";
        String accessToken = "1cf7d9792235b8592eda18bd7dcc2de37f99b3bc";
        // String accessToken = "5d8e186b08062ca405ab25d489fca9823c2a7136";

        GitHub gitHub = GitHub.connectUsingOAuth(githubUrl, accessToken);

        String login = gitHub.getMyself().getLogin();
        System.out.println(login);

        GHRepository repository = gitHub.getMyself().getRepository(repo);
        if (repository != null) {
            System.out.println(repository.getDescription());
            Map<String,GHBranch> branches = repository.getBranches();
            for (String branch : branches.keySet()) {
                System.out.println(branch);

                GHBranch ghBranch = branches.get(branch);

                System.out.println("Repository URL = " + repository.getUrl() + " Branch = " + ghBranch.getName() + " Commit Hash = " + ghBranch.getSHA1());



            }

            PagedIterable<GHCommit> commits = repository.listCommits();
            for (GHCommit commit : commits) {
                List<GHCommit.File> commitFiles = commit.getFiles();
                for (GHCommit.File commitFile : commitFiles) {
                    System.out.println(commitFile.getFileName() + commitFile.getRawUrl());
                }
                System.out.println(commit.getSHA1() + " NumFiles = " + commit.getFiles().size() + " Committer = " + commit.getCommitter().getName()); // + commit.getLastStatus().getDescription());

            }

            GHBranch test2 = branches.get("test2");
            // test2.get

        }


    }

     /*
    val repo = builder.setGitDir(new File("/www/test-repo"))
  .readEnvironment()
  .findGitDir()
  .build()

val walk: RevWalk = new RevWalk(repo, 100)

val head: ObjectId = repo.resolve(Constants.HEAD)
val headCommit: RevCommit = walk.parseCommit(head)
     */



    // 2. Get branch/commit hash for the source repo - the actual source code
    @Test
    public void existsIn() throws IOException {
        final String repo = "binrepo-devex";
        //  final String repo = "search_raptor_binary";
        String githubUrl = "https://github.scm.corp.ebay.com/api/v3";
        String accessToken = "1cf7d9792235b8592eda18bd7dcc2de37f99b3bc";

        final String path = "D:\\dev\\devex\\binrepo-devex\\.git";

        File gitDir = new File(path);
        org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepository(gitDir);
        String branch = repository.getBranch();
        System.out.println(branch);

        RevWalk revWalk = new RevWalk(repository);
        // Pop the most recent commit off from RevWalk
        // RevCommit commit = revWalk.next(); returns null :-(
        // System.out.println(commit);

        ObjectId resolve = repository.resolve(Constants.HEAD);
        RevCommit commit = revWalk.parseCommit(resolve);
        String commitHash = commit.getName();
        System.out.println(commitHash +  "\t" + commit.getFullMessage());


        RefDatabase refDatabase = repository.getRefDatabase();



    }

    // 2. Get branch/commit hash for the source repo - the actual source code
    @Test
    public void existsIn2() throws IOException {
        final String repo = "binrepo-devex";
        //  final String repo = "search_raptor_binary";
        String githubUrl = "https://github.scm.corp.ebay.com/api/v3";
        String accessToken = "1cf7d9792235b8592eda18bd7dcc2de37f99b3bc";

        final String path = "D:\\dev\\devex\\.binrepo-devex\\.git";

        File gitDir = new File(path);
        org.eclipse.jgit.lib.Repository repository = new org.eclipse.jgit.storage.file.FileRepository(gitDir);
        String branch = repository.getBranch();
        System.out.println("Branch=" + branch);
        final Map<String,Ref> allRefs = repository.getAllRefs();
        for (String s : allRefs.keySet()) {
            System.out.println("Here" + s);
        }

        RevWalk revWalk = new RevWalk(repository);
        ObjectId resolve = repository.resolve(Constants.HEAD);
        RevCommit commitRev = revWalk.parseCommit(resolve);
        String commitHash = commitRev.getName();
        System.out.println(commitHash +  "\t" + commitRev.getFullMessage());

        Git binaryRepo = Git.open(gitDir);

        final ListBranchCommand listBranchCommand = binaryRepo.branchList();
        System.out.println(listBranchCommand.getRepository().getFullBranch());
        // get "status"
        final StatusCommand statusCommand = binaryRepo.status();
        Collection<String> toadd = GitUtils.getFilesToStage(statusCommand);
        for (String s : toadd) {
            System.out.println("To be added:" + s);
        }

        // add files to "staging"
        if( toadd.size() > 0 ){
            AddCommand addCmd = binaryRepo.add();
            for( String file : toadd ){
                addCmd.addFilepattern(file);
            }

            try {
                addCmd.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final StoredConfig config = repository.getConfig();
        String url = config.getString("remote", "origin", "url");
        if (url != null) {
            System.out.println("Origin comes from " + url);
        }

        // commit
        final CommitCommand commit = binaryRepo.commit();
        String msg = "Saving Repo:%s Branch:%s CommitHash:%s Time:%s";
        final String formattedMsg = String.format(msg, repo, branch, commitHash, new Date().toString());
        commit.setMessage(formattedMsg);
        try {
            commit.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // push to origin now
        final PushCommand push = binaryRepo.push();
        final String remote = push.getRemote();
        System.out.println("Remote to push to:'" + remote + "'");
        try {
           push.call();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
}
