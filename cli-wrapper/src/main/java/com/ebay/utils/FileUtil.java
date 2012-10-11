package com.ebay.utils;

import com.ebay.github.client.GitHubClient;
import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.kohsuke.github.GHUser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileUtil {

    public static final String USER_NAME = "user.name";

	public static Collection<File> findDirectoriesThatEndWith( File rootDir, String pattern ){
		
		List<File> directories = new ArrayList<File>();
		
		IOFileFilter filefilter = new FileFilter(); // returns false for all files
		IOFileFilter dirFilter  = new DirFilter(pattern);
		
		Collection<File> files = FileUtils.listFilesAndDirs( rootDir, filefilter, dirFilter);
		
		// filter the files as needed
		for( File dir : files ){
			if( dir.getAbsolutePath().endsWith(pattern) ){
				directories.add(dir);
			}
		}
		
		return directories;
	}
	
    /**
     * 
     * 
     * @param srcDir  the validated source directory, must not be {@code null}
     * @param destDir  the validated destination directory, must not be {@code null}
     * @param filter  the filter to apply, null means copy all directories and files
     * @param preserveFileDate  whether to preserve the file date
     * @param exclusionList  List of files and directories to exclude from the copy, may be null
     * @throws IOException if an error occurs
     * 
     */
    public static void doCopyDirectory( File srcDir, 
    									File destDir, 
    									FilenameFilter filter,
    									boolean preserveFileDate, 
    									List<String> exclusionList) throws IOException {
        // recurse
        File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
        
        // null if abstract pathname does not denote a directory, or if an I/O error occurs
        if (srcFiles == null) {  
            throw new IOException("Failed to list contents of " + srcDir);
        }
        
        if (destDir.exists()) {
            if (destDir.isDirectory() == false) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs() && !destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }
        if (destDir.canWrite() == false) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        for (File srcFile : srcFiles) {
            File dstFile = new File(destDir, srcFile.getName());
            if (exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
                if (srcFile.isDirectory()) {
                    doCopyDirectory(srcFile, dstFile, filter, preserveFileDate, exclusionList);
                } else {
                    FileUtils.copyFile(srcFile, dstFile, preserveFileDate);
                }
            }
        }

        // Do this last, as the above has probably affected directory metadata
        if (preserveFileDate) {
            destDir.setLastModified(srcDir.lastModified());
        }
    }
    
    /**
     * 
     * @param source
     * @param destination
     * @param excludePattern
     * @throws IOException
     */
	public static void copyBinaryFolders(File source, 
									File destination, 
									String excludePattern ) 
											throws IOException {

		// TODO: excludePattern should be an array, so that we can pass a list of patterns

		File[] files = source.listFiles();
		List<File> filteredfiles = new ArrayList<File>();
		
		Collection<File> excludeFiles = FileUtil.findDirectoriesThatEndWith(source, excludePattern);
		
		for( File f: excludeFiles ){
			for( File o : files ){
				if( !o.getCanonicalPath().equals(f.getCanonicalPath()) ){
					filteredfiles.add(o);
				}
			}
		}
		
		List<String> exclusionList = new ArrayList<String>();
		exclusionList.add( excludePattern );

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return true;
			}
		};

		int pathlength = source.getCanonicalPath().length();
		for (File f : filteredfiles) {

			// construct the directory to copied
			if (f.getCanonicalPath().startsWith(source.getCanonicalPath())) {

				// get the path that is additional
				String pathfraction = f.getCanonicalPath().substring(pathlength);

				File d = new File(destination, pathfraction);

				
				if( f.isDirectory()){
					System.out.println("copying directory " + f.getCanonicalPath() + " to " + d.getAbsolutePath());
					FileUtil.doCopyDirectory(f, d, filter, true, exclusionList);
				}else{
					
					//File destFile = new File(d,f.getName());
					System.out.println("copying file " + f.getCanonicalPath() + " to " + d.getAbsolutePath());
					FileUtils.copyFile(f, d, true);
				}
			}
		}
	}
	
	public static class FileFilter implements IOFileFilter{

		public boolean accept(File file) {
			// TODO: enhance based on requirement
			return false;
		}

		public boolean accept(File dir, String name) {
			// TODO: enhance based on requirement
			return false;
		}
	}
	
	public static class DirFilter implements IOFileFilter{
		
		private PositionEnum position = PositionEnum.ENDS_WITH;
		private String pattern;
		
		public DirFilter( String pattern ){
			this( pattern, PositionEnum.ENDS_WITH);
		}
		
		public DirFilter( String pattern, PositionEnum position ){
			this.pattern = pattern;
			this.position = position;
		}

		public boolean accept(File file) {
			boolean result = false;
			//System.out.println( "[1] " + file.getAbsolutePath());

			if( file.isDirectory() ){
				result = true;
			}
			
			return result;
		}

		public boolean accept(File dir, String name) {
			boolean result = false;

			// TODO: enhance as needed
			
			return result;
		}
		
	}
	
	public enum PositionEnum {
		STARTS_WITH,
		ENDS_WITH,
		CONTAINS;
	}

    public static boolean existsInGit(final String repo) throws IOException {
        if (Strings.isNullOrEmpty(repo)) return false;
        String userName = System.getProperty(USER_NAME);
        if (!Strings.isNullOrEmpty(userName)) {
            final GHUser user = new GitHubClient().connect().getUser(userName);
            if (user != null) {
                return user.getRepository(repo) != null;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(existsInGit("binrepo-devex"));
        System.out.println(existsInGit("CreatedUsingGitHub-API-Client"));
    }






}
