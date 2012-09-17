package com.ebay.maven.binaryrepository;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class BinaryRepositoryTest {
	
	@Test
	public void readConfig(){
		BinaryRepository repo;
		try {
			repo = new BinaryRepository( new File( System.getProperty("user.dir")) );
			System.out.println( repo.getRepositoryName() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
