package com.ebay.zeus.utils;

import java.io.File;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.zeus.exceptions.ProcessException;
import com.ebay.zeus.executor.ProcessExecutor;

public class MavenUtil {
	public static final Logger logger = LoggerFactory.getLogger(MavenUtil.class);
	
    /**
     * get maven execute according to OS.
     * 
     * @return command String
     */
    public static String getMvnCommand() {

            String command = "mvn";

            String mvnDebug = System.getProperty("TACT_DEBUG");

            if (mvnDebug != null && mvnDebug.trim().length() > 0) {
                    command = "mvnDebug";
            }
            String os = System.getProperty("os.name");

            if (os.toUpperCase().indexOf("WINDOWS") >= 0) {
                    command += ".bat";
            } else {
                    // command += ".sh";
            }

            String m2home = System.getenv("M2_HOME");
            if (m2home != null && !m2home.trim().equals("")) {
                    File maven_home = new File(m2home);
                    command = maven_home.getAbsolutePath() + File.separator + "bin" + File.separator + command;
            }
            
            return command;
    }
    
    public static boolean isMavenAvalible(){
    	String m2home = System.getenv("M2_HOME");
        if (m2home != null && !m2home.trim().equals("")) {
                File maven_home = new File(m2home);
                if (maven_home.exists()){
                	return true;
                }
        }
        
        String path = System.getenv("path");
        String[] paths = path.split(";");
        
        String mavenPath = null;
        for (String subPath:paths){
        	if (subPath.contains("maven")){
        		mavenPath = subPath;
        		break;
            }
        }
        
        if (mavenPath!=null){
        	if (new File(mavenPath).exists()){
        		return true;
        	}
        }
        
        
        return false;
    }

    
    /**
     * 
     * execute maven command
     * 
     * 
     * @param command
     *            command string like " package -Dmaven.repo.local=***"
     * @param workdir
     *            pom file directory
     * @param logger
     *            output logger
     * @return true success, false failed
     * @throws ServiceException
     * @throws ProcessException 
     */
	public static boolean executeMvnCommand(String command, File workdir,
			PrintStream logger) throws ProcessException {
		
		StringBuilder sb = new StringBuilder();

		sb.append(getMvnCommand());
		sb.append(" ");
		sb.append(command);

		System.out.println("executing maven command :" + sb.toString());

		ProcessExecutor mvnExecutor = new ProcessExecutor(sb.toString(),
				workdir, logger);

		// pass all the environment from parent shell

		mvnExecutor.getEnvironment().putAll(System.getenv());
		// mvnExecutor.getEnvironment().put("MAVEN_OPTS", m2_opts);

		boolean result = false;

		try {
			result = mvnExecutor.executeMaven();
		} catch (Exception e) {
			throw new ProcessException(
					"execute maven command failed, command :" + sb.toString(),
					e);
		}

		return result;
	}
}
