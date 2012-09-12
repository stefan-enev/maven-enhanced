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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

import com.ebay.maven.utils.PomUtils;

/**
 * <code>CliWrapper</code> reads pom file(s) and collects the repositories and dependencies the project needs.
 * The wrapper should be able to process all the CLI parameters that maven takes and pass it on to maven.
 * 
 * @author nsankaran
 *
 */
public class CliWrapper {

	public static void main( String[] args ){
		
		// store the CLI parameters
		
		// read the pom.xml
		// TODO: get the pom.xml path from -f argument
		Model model = PomUtils.readModel("pom.xml");
		
		// collect the repositories in the correct order
		List<Repository> repositories= model.getRepositories();
		
		// collect the dependencies
		List<Dependency> dependencies = model.getDependencies();
		
		// TODO: read the settings.xml to collect the repositories
		
		// construct the JSON request 
		
		// call nexus repository with JSON post
		
		// download the dependencies
		
		// invoke maven with dependencies
		
	}

}
