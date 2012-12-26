package com.ebay.zeus.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectEntry {
	private File root;
	private File targetFolder;
	
	private List<File> sourceFolders = new ArrayList<File>();
	
	public ProjectEntry(File projectRoot){
		this.root = projectRoot;
		this.targetFolder = new File(root, "target");
		
		readSourceFolders();
	}

	private void readSourceFolders() {
		File dotClasspath = new File(root, ".classpath");
		if (dotClasspath.exists()){
			try {
				readSourceFoldersFromDotClasspath(dotClasspath);
				return;
			} catch (Exception e) {
				// log it, and ignore it.
				if (sourceFolders != null){
					sourceFolders.clear();
				}
				
			}
		}

		useDefaultMavenSourcefolders();
	}
	
	private void readSourceFoldersFromDotClasspath(File dotClasspath) throws Exception {
		SAXParserFactory spf =SAXParserFactory.newInstance();
	    spf.setNamespaceAware(true);
	    XMLReader parser = spf.newSAXParser().getXMLReader();
	    parser.setContentHandler(new DefaultHandler() {
	        public void startElement(String uri,String localName,String qname,Attributes atts) {
	            if( !localName.equals("classpathentry") )
	                return;

	            String kind = atts.getValue("kind");
				if (kind != null && kind.equals("src")) {
					String path = atts.getValue("path");
					addFolder(path);
				}
	        }
	    });
	    parser.parse(dotClasspath.toURL().toString());
	}

	private void useDefaultMavenSourcefolders() {
		addFolder("src/main/java");
		addFolder("src/main/resources");
		addFolder("src/test/java");
		addFolder("src/test/resources");
	}
	
	private void addFolder(String folderPath){
		File mainJava = new File(root, folderPath);
		if (mainJava.exists() && mainJava.isDirectory() && mainJava.canRead()){
			sourceFolders.add(mainJava);
		}
	}
	
	public File getTargetFolder(){
		return this.targetFolder;
	}
	
	public File getProjectRoot(){
		return this.root;
	}
	
	public List<File> getSourceFolders(){
		return this.sourceFolders;
	}
	
	public boolean belongToSourceFolder(File file){
		for (File srcFolder:this.sourceFolders){
			if (file.getAbsolutePath().startsWith(srcFolder.getAbsolutePath())){
				return true;
			}
		}
		
		return false;
	}
	
	public String getPackagePath(File file){
		for (File srcFolder:this.sourceFolders){
			String filePath = file.getAbsolutePath();
			String srcFolderPath = srcFolder.getAbsolutePath();
			
			if (filePath.startsWith(srcFolderPath)){
				String packagePath = filePath.substring(srcFolderPath.length(), filePath.length()-file.getName().length());
				
				if (srcFolderPath.contains("src/test/")){
					packagePath = filePath.substring(srcFolderPath.length(), filePath.length()-file.getName().length());
					return "test-classes" + packagePath;
				}else{
					return "classes" + packagePath;
				}
				
			}
		}
		
		return null;
	}
	
}
