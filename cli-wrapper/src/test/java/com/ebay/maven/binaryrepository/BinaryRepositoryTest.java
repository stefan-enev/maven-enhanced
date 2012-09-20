package com.ebay.maven.binaryrepository;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class BinaryRepositoryTest {
	
	@Test
	public void getRepositoryName(){
		
		BinaryRepository repo;
		try {
			repo = new BinaryRepository( new File( System.getProperty("user.dir")) );
			System.out.println( repo.getRepositoryName() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Test
	public void isBinaryRepositoryAvailable(){
		
		BinaryRepository repo;
		try {
			repo = new BinaryRepository( new File( System.getProperty("user.dir")) );
			System.out.println( repo.isBinaryRepositoryAvailable() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}
	
	@Test
	public void createBinaryRepository(){
		BinaryRepository repo;
		try {
			repo = new BinaryRepository( new File( System.getProperty("user.dir")) );
			repo.createBinaryRepository();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Test
	public void copyBinaryRepository(){
		BinaryRepository repo;
		try {
			repo = new BinaryRepository( new File( System.getProperty("user.dir")) );
			File destination = new File( new File( System.getProperty("user.dir")) , "destination");
			repo.copyBinaryFolders("target" , destination );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}

}
