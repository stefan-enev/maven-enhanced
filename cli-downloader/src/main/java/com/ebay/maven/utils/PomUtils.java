package com.ebay.maven.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomUtils {
	
	public static Model readModel( String pom ){
		
		Model model = null;
		
		try {
			
			File pomFile = new File(pom);
			BufferedReader in = new BufferedReader( new FileReader(pomFile));
			MavenXpp3Reader reader = new MavenXpp3Reader();
			model = reader.read(in);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return model;
	}

}
