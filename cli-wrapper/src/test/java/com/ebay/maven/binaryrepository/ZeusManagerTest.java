package com.ebay.maven.binaryrepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ZeusManagerTest {
	
	@Test
	public void getRepositoryName(){
		
		ZeusManager repo;
		try {
			repo = new ZeusManager( new File( System.getProperty("user.dir")) );
			System.out.println( repo.getRepositoryName() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	@Test
	public void isBinaryRepositoryAvailable(){
		
		ZeusManager repo;
		try {
			repo = new ZeusManager( new File( System.getProperty("user.dir")) );
			System.out.println( repo.isBinaryRepositoryAvailable() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}
	
	@Test
	public void createBinaryRepository(){
		ZeusManager repo;
		try {
			repo = new ZeusManager( new File( System.getProperty("user.dir")) );
			repo.createBinaryRepository();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (GitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MapServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void copyBinaryRepository(){
		ZeusManager repo;
		try {
			repo = new ZeusManager( new File( System.getProperty("user.dir")) );
			File destination = new File( new File( System.getProperty("user.dir")) , "destination");
			List<String> excludes = new ArrayList<String>();
			excludes.add("localobr");
			repo.copyBinaryFolders("target" , excludes, destination );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}

}
