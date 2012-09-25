package com.ebay.maven.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CliArgsParser {
	
	private Options options = null;
	
	public CliArgsParser(){
		options = buildOptions();
	}
	
	public Options buildOptions(){
		Options options = new Options();
		
		options.addOption("c", "create-update", false, "create or update binary repository");
		options.addOption("s", "setup", false, "setup workspace");
		options.addOption("h", "help", false, "usage");
		
		return options;
	}
	
	public InputParams parse( String[] args ) throws ParseException{
		
		InputParams params = new InputParams();
		if( args == null || args.length == 0 ){
			params.setMode(RunMode.USAGE);
			return params;
		}
		
		GnuParser parser = new GnuParser();
		CommandLine cli = parser.parse(options, args, true);
		
		
		if( cli.hasOption('c') ){
			params.setMode(RunMode.CREATE_UPDATE);
		}
		if( cli.hasOption('s' )){
			params.setMode(RunMode.SETUP);
		}
		if( cli.hasOption('s')){
			params.setMode(RunMode.USAGE);
		}
		
		return params;
	}
	
	public void printUsage(){
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("cliwrapper", options);
	}

}
