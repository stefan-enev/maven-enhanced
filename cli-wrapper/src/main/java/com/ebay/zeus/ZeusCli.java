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
import java.util.Calendar;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.cli.CliArgsParser;
import com.ebay.zeus.cli.InputParams;
import com.ebay.zeus.cli.RunMode;
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
 * @author nambi sankaran, yunfwang@ebay.com
 */
public class ZeusCli {

	public static final Logger logger = LoggerFactory.getLogger(ZeusCli.class);
	
	public static void main( String[] args ) throws ParseException{
		
		long begin = Calendar.getInstance().getTimeInMillis();
		
		ZeusCli cli = new ZeusCli();
		InputParams input = cli.processCliArguments(args);
		
		cli.process(input);
		
		long end = Calendar.getInstance().getTimeInMillis();
		long diff = end - begin;
		
		logger.info("Time taken " + diff + " ms");
	}
	
	public InputParams processCliArguments( String[] args ) throws ParseException{
		
		InputParams input = null;
		
		CliArgsParser parser = new CliArgsParser();
		input = parser.parse(args);
			
		if( input.getMode().equals(RunMode.USAGE ) ){
			parser.printUsage();
		}
		
		logger.info("input params: mode - "+input.getMode()+ "; map service url - "+ input.getMapSvcUrl());
		
		return input;
	}
	
	public void process( InputParams input ){
		File root = new File(System.getProperty("user.dir"));
		
		String inputRepoRoot = input.getSourceRepoRoot();
		if (inputRepoRoot!=null && ZeusUtil.isLocalRepoExisted(new File(inputRepoRoot))){
			root = new File(inputRepoRoot);
		}
		
		logger.info("Running Zeus in source repository root:" + root.getAbsolutePath());
		
		ZeusManager zmanager = null;
		try {
			zmanager = new ZeusManager(root);

			if (input.getMode().equals(RunMode.CREATE_UPDATE)) {
				zmanager.createOrUpdateBinaryRepository();
			}

			if (input.getMode().equals(RunMode.SETUP)) {
				zmanager.setupProject();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}
}
