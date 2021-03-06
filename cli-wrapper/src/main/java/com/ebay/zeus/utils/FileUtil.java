package com.ebay.zeus.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.google.common.io.Files;

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
	
    public static void copyBinaryFolders(String pattern, List<String> exclusionList, File source, File destination) throws IOException {
//        File root = source.getParentFile();
        Collection<File> files = FileUtil.findDirectoriesThatEndWith(source, pattern);

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				if (file.isDirectory()){
					return true;
				}
				
				if (name.endsWith(".class")){
					return true;
				}
				return false;
			}
		};

        int pathlength = source.getCanonicalPath().length();
        for (File f : files) {
            // construct the directory to copied
			if (f.getCanonicalPath().startsWith(source.getCanonicalPath())) {
                // get the path that is additional
				String pathfraction  = f.getCanonicalPath().substring(pathlength);
                File d = new File(destination, pathfraction );
                if (d.isDirectory()){
                	FileUtils.deleteDirectory(d);
                }
                System.out.println( "copying " + f.getCanonicalPath() + " to " + d.getAbsolutePath() );
				FileUtil.doCopyDirectory(f, d, filter, true, exclusionList);
			}
		}
	}
    
	public static class FileFilter implements IOFileFilter{
		public boolean accept(File file) {
			return false;
		}

		public boolean accept(File dir, String name) {
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


    // Filters out all files/directories which have target in the canonical path name and excludes any files/dirs that belong
    // under localobr
    // TODO: RGIROTI Talk to Nambi about filtering out more patterns from here - for instance anything under target/bundles or matching target/.*.jar
    private static java.io.FileFilter targetDirFilter = new java.io.FileFilter() {
        public boolean accept(File dir) {
            boolean temp = false;
            try {
                temp = dir.getCanonicalPath().contains("target") && !dir.getCanonicalPath().contains("localobr");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return temp;
        }
    };

    /**
     * Copies all binaries - read class files - from srcDir (should be top-level project directory) to dstDir (binary
     * equivalent of top-level project directory named .project usually)
     *
     * @param srcDir        the source directory
     * @param dstDir        the destination directory
     * @throws IOException  if any errors were encountered in copying
     */
    public static void copyBinaries(final File srcDir, final File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir(); // Create dstDir if required

            final File[] files = srcDir.listFiles();
            for (File file : files) {
                if (file.isDirectory())
                    copyDirectory(file, new File(dstDir, file.getName()), targetDirFilter);
            }
        }

    }

    /**
     * Copies everything from srcDir to dstDir using the passed in filter. If filter is null, does not do any
     * filtering in copying of files
     *
     * @param srcDir        the source directory
     * @param dstDir        the destination directory
     * @param fileFilter    the file filter to use here
     *
     * @throws IOException  if any errors were encountered in copying
     */
    public static void copyDirectory(final File srcDir, final File dstDir, final java.io.FileFilter fileFilter) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) dstDir.mkdir();

            File[] files = null;
            if (fileFilter != null) {
                files = srcDir.listFiles(fileFilter);

            } else {
                files = srcDir.listFiles();
            }

            for (File file : files) {
                copyDirectory(file, new File(dstDir, file.getName()), targetDirFilter);
            }

        } else {
            if (srcDir.lastModified() > dstDir.lastModified()) {
                System.out.println("Copying source:'" + srcDir.getAbsolutePath() + "' to destination:'" + dstDir.getAbsolutePath() + "'");
                Files.copy(srcDir, dstDir);
            }
        }
    }

    public static List<String> findExcludes(File rootDir, String pattern) throws IOException{
		List<String> excludes = new ArrayList<String>();
		Collection<File> excludeFiles = findDirectoriesThatEndWith(rootDir, pattern);
		for( File file: excludeFiles ){
			excludes.add( file.getCanonicalPath() );
		}
		
		return excludes;
	}
    
    /**
     * copy source repo's output files to binary repo.
     * 
     * @param srcRepoRoot
     * @param srcChangedFiles : source repo's changed source files.
     * @param binaryRepoRoot
     */
	public static void copyOutputFiles(File srcRepoRoot, List<File> srcChangedFiles,
			File binaryRepoRoot) {
		
		if (srcChangedFiles.size() == 0){
			return;
		}
		
		Set<File> candidateFiles = getOutputFiles(srcRepoRoot, srcChangedFiles);
		String srcRepoRootPath = srcRepoRoot.getAbsolutePath();
		
		for (File srcFile:candidateFiles){
			File destFile = null;
			String srcFilePath = srcFile.getAbsolutePath();
			int idx = srcFilePath.indexOf(srcRepoRootPath);
			if (idx != -1){
				destFile = new File(binaryRepoRoot, srcFilePath.substring(idx+srcRepoRootPath.length()));
			}
			
			try {
				FileUtils.copyFile(srcFile, destFile);
			} catch (IOException e) {
				// TODO log it, shouldn't break the process.
			}
		}
		
	}

	/**
	 * get output files according to changed source files 
	 * 
	 * @param srcRepoRoot
	 * @param srcChangedFiles
	 * @return
	 */
	private static Set<File> getOutputFiles(File srcRepoRoot,	List<File> srcChangedFiles) {
		if (srcChangedFiles.size()==0){
			return Collections.emptySet();
		}
		
		List<ProjectEntry> entries = getProjectEntries(srcRepoRoot);
		
		if (entries.size() == 0){
			return Collections.emptySet();
		}
		
		Set<File> candidateFiles = new ListOrderedSet();
		
		for (File srcFile:srcChangedFiles){
			File targetFile = getTargetFile(srcFile, entries);
			if (targetFile != null && targetFile.exists()){
				candidateFiles.add(targetFile);
			}
		}
		
		return candidateFiles;
	}
	
	/**
	 * get all project entries, contains project's meta-info.
	 * like source folder, project root path etc.
	 * 
	 * @param repoRoot
	 * @return
	 */
	public static List<ProjectEntry> getProjectEntries(File repoRoot){
		Collection<File> pomFiles = listPomFiles(repoRoot);
		List<ProjectEntry> entries = new ArrayList<ProjectEntry>();
		for (File pom:pomFiles){
			ProjectEntry entry = new ProjectEntry(pom.getParentFile());
			entries.add(entry);
		}
		
		return entries;
	}
	
	/**
     * get their output files in "target" folder.
     * 
     * @param srcFile : source file.
     * @param projectEntries : list of ProjectEntry.
     * @return
     */
    public static File getTargetFile(File srcFile, List<ProjectEntry> projectEntries) {
		ProjectEntry entry = getProjectEntry(srcFile, projectEntries);
		
		if (entry == null){
			return null;
		}
		
		return getTargetFile(srcFile, entry);
	}
    
    public static File getTargetFile(File srcFile, ProjectEntry entry){
    	String fileName = srcFile.getName();
		if (fileName.endsWith(".java")){
			fileName = fileName.substring(0, fileName.length()-5)+".class";
		}
		
		String filePath = entry.getPackagePath(srcFile)+fileName;
		return new File(entry.getTargetFolder(), filePath);
    }
    
    public static ProjectEntry getProjectEntry(File srcFile,
			List<ProjectEntry> projectEntries) {
		for (ProjectEntry entry:projectEntries){
			if (entry.belongToSourceFolder(srcFile)){
				return entry;
			}
		}
		
		return null;
	}

	/**
     * list all files that filename equals specified filename.
     * 
     * @param dir
     * @param fileName
     * @return
     */
    public static Collection<File> listFiles(File dir, String fileName){
    	NameFileFilter filter = new NameFileFilter(fileName);
    	return FileUtils.listFiles(dir, filter, TrueFileFilter.INSTANCE);
    }
    
    /**
     * list all pom.xml for specified parent directory.
     * 
     * @param dir
     * @return
     */
    public static Collection<File> listPomFiles(File dir){
    	return listFiles(dir, "pom.xml");
    }
}
