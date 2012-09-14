package com.ebay.maven.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CliArgsParser {
	
	private Options options = null;
	
	public CliArgsParser(){
		options = buildOptions();
	}
	
	public Options buildOptions(){
		Options options = new Options();
		
		// add t option
		options.addOption("c", false, "create binary repository");
		
		return options;
	}
	
	public InputParams parse( String[] args ) throws ParseException{
		GnuParser parser = new GnuParser();
		CommandLine cli = parser.parse(options, args, true);
		
		InputParams params = new InputParams();
		
		if( cli.hasOption('c') ){
			params.setMode(RunMode.BINARY_REPO);
			params.getActions().add( Actions.BINARYREPO_CREATE );
		}
		
		return params;
	}

}
