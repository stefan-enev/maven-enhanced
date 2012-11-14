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
package com.ebay.maven.cli;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

import com.ebay.maven.binaryrepository.BinaryRepository;
import com.ebay.maven.binaryrepository.GitException;
import com.ebay.maven.binaryrepository.MapServiceException;
import com.ebay.maven.utils.PomUtils;

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
 * This will enable the developer to get start coding in seconds. 
 * 
 * 
 * @author nambi sankaran
 */
public class CliWrapper {

	public static void main( String[] args ) throws ParseException{
		
		long begin = Calendar.getInstance().getTimeInMillis();
		
		CliWrapper wrapper = new CliWrapper();
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
			createOrUpdateBinaryRepository( input);
		}
		if( input.getMode().equals(RunMode.SETUP) ){
			setupProject(input);
		}
	}
	

	
	public void createOrUpdateBinaryRepository( InputParams input ){
		
		// assume the current directory the "root" of the project
		File root = new File( System.getProperty("user.dir"));
		try {
           
			// System.out.println(root.getName());
			
            // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
			BinaryRepository repository = new BinaryRepository(root);
			if( input.getMapSvcUrl() != null ){
				repository.setBaseServiceUrl(input.getMapSvcUrl() );
			}
			
			if (repository.isBinaryRepositoryAvailable()) {
                repository.updateBinaryRepository();

			} else {
				if (repository.isRemoteBinaryRepositoryAvailable()) {
					// IN this case, setup must be run before.
					System.out.println("Remote binary repository is already available, but not cloned. Please run -s option");
				} else {
					repository.createBinaryRepository();
				}                
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MapServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setupProject(final InputParams input ) {
		// TODO: download dependencies
		
		// read the source project
		File root = new File( System.getProperty("user.dir"));
        // TODO: RGIROTI Remove next line at some point - refactor this to a test case somewhere
        // root = new File("D:\\dev\\devex\\binrepo-devex");
		try {
			BinaryRepository repository = new BinaryRepository(root);
            repository.setBaseServiceUrl(input.getMapSvcUrl());
			
			if( repository.isBinaryRepositoryAvailable() ){
				
			}else if( repository.isRemoteBinaryRepositoryAvailable() ) {
				repository.cloneBinaryRepository();
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
