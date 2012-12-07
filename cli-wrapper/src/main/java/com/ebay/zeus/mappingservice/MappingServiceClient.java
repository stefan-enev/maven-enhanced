package com.ebay.zeus.mappingservice;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.ebay.zeus.beans.BinRepoBranchCommitDO;
import com.ebay.zeus.exceptions.MapServiceException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class MappingServiceClient {
    private static Client client;

    static {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(JacksonJsonProvider.class);
        client = Client.create(config);
    }

    public static final String SVC_BASE_URL = "http://stratus-fd12.stratus.dev.ebay.com:8080";
    public static final String SVC_BASE = "services/repo";
    public static final String SVC_FINDBY_REPO_BRANCH_COMMITID = "search/byrepourlbranchandcommitid/?";
    public static final String UTF_8 = "UTF-8";
    
    private String baseServiceUrl = null;
    
	public MappingServiceClient(){
		this.baseServiceUrl = SVC_BASE_URL;
	}
	
	public BinRepoBranchCommitDO newInstance(final String repoUrl,
			final String branch, final String commitHash,
			final String binRepoUrl, final String binRepoBranch,
			final String binRepoCommitHash) throws UnsupportedEncodingException {
		BinRepoBranchCommitDO binRepoBranchCommitDO = new BinRepoBranchCommitDO();
		binRepoBranchCommitDO.setRepoUrl(URLEncoder.encode(repoUrl, UTF_8));
		binRepoBranchCommitDO.setBranch(URLEncoder.encode(branch, UTF_8));
		binRepoBranchCommitDO.setCommitId(URLEncoder.encode(commitHash, UTF_8));
		binRepoBranchCommitDO.setBinRepoUrl(URLEncoder
				.encode(binRepoUrl, UTF_8));
		binRepoBranchCommitDO.setBinRepoBranch(URLEncoder.encode(binRepoBranch,
				UTF_8));
		binRepoBranchCommitDO.setBinRepoCommitId(URLEncoder.encode(
				binRepoCommitHash, UTF_8));
		return binRepoBranchCommitDO;
	}

	public String getUrlForFindByRepoBranchCommit()
			throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		sb.append(getBaseServiceUrl());
		sb.append("/");
		sb.append(SVC_BASE);
		sb.append("/");
		sb.append(SVC_FINDBY_REPO_BRANCH_COMMITID);
		// TODO: Convert any "//" to "/" Look at main()
		return sb.toString();
	}

	public String getUrlForPost() throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		sb.append(getBaseServiceUrl());
		sb.append("/");
		sb.append(SVC_BASE);
		return sb.toString();
	}

	public String getBaseServiceUrl() {
		return baseServiceUrl;
	}

	public void setBaseServiceUrl(String serviceUrl) {
		this.baseServiceUrl = serviceUrl;
	}
	
	public void post(final String repoUrl,
			final String branch, final String commitHash,
			final String binRepoUrl, final String binRepoBranch,
			final String binRepoCommitHash) throws Exception{
		BinRepoBranchCommitDO binRepoBranchCommitDO = newInstance(repoUrl, branch, commitHash, binRepoUrl, binRepoBranch, binRepoCommitHash);
		
		final WebResource resource = client.resource(getUrlForPost());
        
        BinRepoBranchCommitDO postedDO = null;
        try {
            postedDO = resource.accept(MediaType.APPLICATION_XML).post(BinRepoBranchCommitDO.class, binRepoBranchCommitDO);
            System.out.println("Posted Object = " + postedDO.toString());
        } catch (UniformInterfaceException e) {
            int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
            System.out.println("status code: " + statusCode);
            throw new MapServiceException("Unable to register the commit details", e);
        }
	}
	
	public boolean isEntryExisted(String repoUrl, String branch, String commitHash) throws Exception{
		final String url = getUrlForFindByRepoBranchCommit() + "repourl=" + URLEncoder.encode(repoUrl, UTF_8) + "&branch=" +
        URLEncoder.encode(branch, UTF_8) + "&commitid=" + URLEncoder.encode(commitHash, UTF_8);
		System.out.println("svc url : " + url);
		
		WebResource webResource = client.resource(url);
		
		BinRepoBranchCommitDO binRepoBranchCommitDO1 = null;
		
		try {
		    binRepoBranchCommitDO1 = webResource.accept(MediaType.APPLICATION_JSON).get(BinRepoBranchCommitDO.class);
		    if (binRepoBranchCommitDO1 != null){
		    	return true;
		    }
		} catch (UniformInterfaceException e) {
		    int statusCode = e.getResponse().getClientResponseStatus().getStatusCode();
		    System.out.println("Service Status Code : " + statusCode);
		    return (statusCode == 204 || statusCode == 404);     // HTTP 204 is NO CONTENT which is ok for us
		} catch (Exception e) { // Catch-all to deal with network problems etc.
		    //TODO: log it.		    
		}
		
		return false;
	}
}
