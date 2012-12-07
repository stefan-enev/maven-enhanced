/*
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. 
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package com.ebay.zeus;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

import com.ebay.zeus.cli.CliArgsParser;
import com.ebay.zeus.cli.InputParams;
import com.ebay.zeus.cli.RunMode;
import com.ebay.zeus.exceptions.GitException;
import com.ebay.zeus.repositorys.SourceZeusRepository;
import com.ebay.zeus.utils.PomUtils;
import com.ebay.zeus.utils.ZeusUtil;

/**
 * <code>CliWrapper</code> prepares the workspace before maven kicks in.
 * <ol>
 * 	<li>Downloads all dependencies and pre-fill the local repository before maven starts.</li>
 * 	<li>If the project has a <I>Compiled Source Repository</I>, it gets the class files into target/classes folder.
 * </ol>
 *
 * <H3>Downloading All Dependencies</H3>
 * reads pom file(s) and collects the repositories and dependencies the project needs.
 * The wrapper should be able to process all the CLI parameters that maven takes and pass it on to maven.
 * 
 * <H3>Using compiled repository</H3>
 * large projects may have 1000s of java classes, it may take a long time to build in a laptop.
 * So, lightening will download the pre-compiled classes for the project and fill 'target' folder.
 * This will enable the developer to start coding in seconds. 
 * 
 * 
 * @author nambi sankaran
 */
public class ZeusCli {

	public static void main( String[] args ) throws ParseException{
		
		long begin = Calendar.getInstance().getTimeInMillis();
		
		ZeusCli wrapper = new ZeusCli();
		InputParams input = wrapper.processCliArguments(args);
		
		wrapper.process(input);
		
		long end = Calendar.getInstance().getTimeInMillis();
		long diff = end - begin;
		System.out.println("Time taken " + diff + " ms");
	}
	
	public InputParams processCliArguments( String[] args ) throws ParseException{
		
		InputParams input = null;
		
		CliArgsParser parser = new CliArgsParser();
		input = parser.parse(args);
			
		if( input.getMode().equals(RunMode.USAGE ) ){
			parser.printUsage();
		}
		
		return input;
	}
	
	public void process( InputParams input ){
		
		if( input.getMode().equals(RunMode.CREATE_UPDATE) ){
			try {
				createOrUpdateBinaryRepository( input);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if( input.getMode().equals(RunMode.SETUP) ){
			try {
				setupProject(input);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void createOrUpdateBinaryRepository( InputParams input ) throws IOException{
		
		// assume the current directory the "root" of the project
		File root = new File( System.getProperty("user.dir"));
		SourceZeusRepository sourceRepository = new SourceZeusRepository(root);
		
		try {
			
			ZeusManager zmanager = new ZeusManager(root);
			if( input.getMapSvcUrl() != null ){
				zmanager.setBaseServiceUrl(input.getMapSvcUrl() );
			}
			
			long starttime=0l;
			long endtime = 0l;
			
			while( true ){
			
				if (ZeusUtil.isLocalBinaryRepositoryExisted(sourceRepository.getDirectory())) {
					
						// if previous run started in less then 1 minute before, wait for a minute
						long begintime = Calendar.getInstance().getTimeInMillis();
						if( ( begintime - starttime ) < (60*1000) ){
							Thread.sleep(60*1000);
						}
						
						// calculate start time
						starttime = Calendar.getInstance().getTimeInMillis();
						
						// TODO: run 'show-ref' and keep the current status of src & bin repos in memory before doing a 'fetch'
						
						// TODO: ideally we need 'git fetch' and record what is fetched, which is then processed
	
						// TODO: calculate how many new branches/commits have been created since the last fetch on source repo
						//zmanager.findNewCommits();
						
						// TODO: figure out the new commits and process each one of them
						//zmanager.processNewCommits();
	
						// TODO: remove this after implementing above steps. this is temporary
						// get the latest by "git pull" on "source" and "binary". 
						zmanager.gitpull();
						
						// TODO: perform maven build. Remove this after implementing 'processNewCommits'
						// even if it fails, continue the loop
						zmanager.build("compile");
						
						// update binary repo
		                zmanager.updateBinaryRepository();
		                
		                endtime = Calendar.getInstance().getTimeInMillis();
		                
		                System.out.println("Updated in " + ((endtime-starttime)/1000) + " seconds");
					
	
				} else {
					
					if (ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository.getRemoteUrl())) {
						
						System.out.println("cloning binary repository....");
						// clone binary repository
						zmanager.cloneBinaryRepository(false);
						
						System.out.println("binary repository cloned");
					
					} else {
						
						// calculate start time
						starttime = Calendar.getInstance().getTimeInMillis();
						zmanager.createBinaryRepository();
						
						endtime = Calendar.getInstance().getTimeInMillis();
		                System.out.println("Created in " + ((endtime-starttime)/1000) + " seconds");
					}                
				}
			
			}
		} catch (Exception e){
			//TODO: log it.
		}
	}
	
	public void setupProject(final InputParams input ) throws IOException {
		// TODO: download dependencies
		
		// read the source project
		File root = new File( System.getProperty("user.dir"));
        // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
        // root = new File("D:\\dev\\devex\\binrepo-devex");
		
		SourceZeusRepository sourceRepository = new SourceZeusRepository(root);
		
		try {
			ZeusManager repository = new ZeusManager(root);
            repository.setBaseServiceUrl(input.getMapSvcUrl());
			
			if( ZeusUtil.isBinaryRepositoryExisted(sourceRepository) ){
				repository.checkoutAndCopyFromBinaryRepository();
				System.out.println("setup is complete");
			}else if( ZeusUtil.isRemoteBinaryRepositoryExisted(sourceRepository.getRemoteUrl()) ) {
				repository.cloneBinaryRepository(true);
				System.out.println("setup is complete");
			}else{
				// TODO: anything we can do?
				System.out.println("Binary repository not available. exiting...");
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Get the binary classes and populate project "target" folders
	}

	public void downloadDependencies(){
		
		// read the pom.xml
		// TODO: get the pom.xml path from -f argument
		Model model = PomUtils.readModel("pom.xml");
		
		// collect the repositories in the correct order
		List<Repository> repositories= model.getRepositories();
		
		// collect the dependencies
		List<Dependency> dependencies = model.getDependencies();
		
		System.out.println(repositories.toString() + dependencies.toString() );
		// TODO: read the settings.xml to collect the repositories
		
		// construct the JSON request 
		
		// call nexus repository with JSON post
		
		// download the dependencies
		
		// invoke maven with dependencies
	}
}
