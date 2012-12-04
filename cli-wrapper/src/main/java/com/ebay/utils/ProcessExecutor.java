package com.ebay.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 
 * This class is copied from com.ebay.ebox.core.utils.ProcessExecutor
 */
public class ProcessExecutor {
	
	private String command;
	private ProcessBuilder builder;
	private PrintStream outStream;
	
	public ProcessExecutor( String cmd, String directory, PrintStream logger ){
		this(cmd, new File(directory), logger);
	}
	
	public ProcessExecutor( String cmd, String directory, PrintStream logger, boolean redirectErrorStream){
		this(cmd, new File(directory), logger, redirectErrorStream);
	}
	
	public  ProcessExecutor( String cmd, File directory, PrintStream logger){
		/*
		 * Redirecting error stream to output stream causes maven builds with soa codegen to exit with code 1.
		 * Handle Error and Input streams separately
		 */
		this(cmd, directory, logger, false);  
	}
	
	public ProcessExecutor(String cmd, File directory, PrintStream logger, boolean redirectErrorStream) {
		if(isWindows()){
			command = "cmd  /C  " +  cmd;
		}else{
			command = cmd;
		}
		
		String strippedCmd = CommonUtils.replaceMultipleSpacesWithSingleSpace( command );
		
		String[] commandArray = strippedCmd.split(" ");
        List<String> cmdarray = Arrays.asList(commandArray);
        builder = new ProcessBuilder(cmdarray);
        builder.redirectErrorStream(redirectErrorStream);
        builder.directory(directory);
        outStream = logger;
	}
	
	public Map<String,String> getEnvironment(){
		return builder.environment();
	}
	
	public ProcessBuilder getProcessBuilder(){
		return builder;
	}
	
	public boolean execute() throws InterruptedException, IOException, CommonException{
		
		boolean result = false;
	
		outStream.println("executing : "+ command);
        Process proc = builder.start();

        // drain the stdout and stderr from the process
        StreamReaderThread outReaderThread = new StreamReaderThread(proc.getInputStream(),outStream);
        outReaderThread.start();
        
        StreamReaderThread errReaderThread = null;
        
		if (!builder.redirectErrorStream()){
        	errReaderThread = new StreamReaderThread(proc.getErrorStream(),outStream);
        	errReaderThread.start();
        }       

        proc.waitFor();

        outReaderThread.join();
       
		if (errReaderThread != null){
        	errReaderThread.join();
        }
        
        outStream.println("Exit code: " + proc.exitValue());
        if(  proc.exitValue() != 0 ){
            // error executing the script
            outStream.println("ERROR : Command " + getCommand() + " failed");
            throw new CommonException("ERROR : Command " + getCommand() + " failed");
        }else{
        	result = true;
        }
        
        return result;
	}
	
	public boolean executeMaven(File mavenHome ) throws InterruptedException, IOException,CommonException{

		Map<String,String> env =getEnvironment();
        env.put("M2_HOME", mavenHome.getAbsolutePath() );
        prependPath(env, mavenHome.getAbsolutePath() + File.separator + "bin" );
        String mavenOpts = env.get("MAVEN_OPTS");
        if( mavenOpts != null && !mavenOpts.trim().equals("")){
            env.put("MAVEN_OPTS", "-DDUMMY");            
        }
        
        return execute();
	}
	
	public boolean executeMaven() throws InterruptedException, IOException,CommonException{

		Map<String,String> env =getEnvironment();
        String mavenOpts = env.get("MAVEN_OPTS");
        if( mavenOpts != null && !mavenOpts.trim().equals("")){
            env.put("MAVEN_OPTS", "-DDUMMY");            
        }
        
        return execute();
	}
	
	public static boolean isWindows(){
		boolean result = false;
		String os = System.getProperty("os.name");
	   
		if( os.toUpperCase().indexOf( "WINDOWS" )>= 0 ){
				result = true;
		}
		
		return result;
	}
	
	public String getCommand(){
		return command;
	}
	
    public class StreamReaderThread extends Thread {
        InputStream in;
        PrintStream out;

        StreamReaderThread(InputStream input, PrintStream output) {
            in = input;
            out = output;
        }

        public void run() {

            try {

                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                	
                	char[] chararray = line.toCharArray();
                	char[] newarray = CommonUtils.removeCarriageReturns(chararray);
                	
                	String str = new String(newarray);
                	out.println( str );
                	
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }
    
    public static String convertPathToOS( String path ){
    	
    	if( isWindows()){
    		return addBackSlashForWindowsPath(path);
    	}else{
    		return path;
    	}
    }
    
    public static String addBackSlashForWindowsPath(String path){
    	
    	String patternStr = "\\\\";
    	String replaceStr = "\\\\\\\\";
    	
    	Pattern pattern = Pattern.compile( patternStr );
    	
    	Matcher matcher = pattern.matcher(path);
    	String result = matcher.replaceAll(replaceStr);
    	
    	return result;
    }
    
    private void prependPath( Map<String,String>  env, String path){

        String systempath = CommonUtils.getPath(env);
        systempath = path + File.pathSeparator + systempath;

        if( CommonUtils.isLinux()){
            env.put("PATH", systempath);
        }else{
             env.put("Path", systempath);   
        }
    }
    
    public static void main(String[] args){
    	
    	String line = "hello\n";
    	
    	// get the last char
    	char c =line.charAt( ( line.length()-1));
    	if( c == '\n'){
    		System.out.print(line);
    	}else{
    		System.out.println(line);
    	}
    	
    	System.out.println("----");
    }
}