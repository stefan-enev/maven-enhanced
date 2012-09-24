package com.ebay.github.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class RestClient {
	
	private Client client=null;
	private static String githubUrl = "https://github.scm.corp.ebay.com";
	
	public RestClient(){
		client = Client.create();
	}
	
	public void createRepo( String binaryRepo ){
		
		WebResource webResource = client.resource( githubUrl);
	}

}
