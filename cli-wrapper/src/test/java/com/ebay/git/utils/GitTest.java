package com.ebay.git.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import org.junit.Test;

import com.ebay.zeus.git.commands.ShowRef;

public class GitTest {
	
	@Test
	public void fetch(){
		
		FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
		try {
			Repository repository = repobuiler.findGitDir( new File("s:\\git\\demo\\search_raptor") ).build();
			Git git = Git.wrap(repository);
			
			FetchResult fetchResult = git.fetch().call();
			
			Collection<Ref> refs = fetchResult.getAdvertisedRefs();
			
			for( Ref ref : refs ){
				System.out.println( ref.getName() + ":" + ref.getObjectId().getName() );
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void showref(){
		
		FileRepositoryBuilder repobuiler = new FileRepositoryBuilder();
		Repository repository;
		
		try {
			repository = repobuiler.findGitDir( new File("s:\\git\\demo\\search_raptor") ).build();
			ShowRef showref = new ShowRef(repository);
			
			showref.run();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
