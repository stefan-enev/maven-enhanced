package com.ebay.zeus.repository;

import java.io.File;
import java.io.IOException;

public class SourceZeusRepository extends ZeusRepository {

	public SourceZeusRepository(File gitDir) throws IOException {
		super(gitDir);
	}

	//TODO
	public void processNewCommits(){
		
	}
}
