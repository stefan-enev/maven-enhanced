package com.ebay.zeus.exceptions;

public class GitException extends Exception {

	private static final long serialVersionUID = -6877578368311715694L;
	
	public GitException( String message ){
		super( message );
	}
	
	public GitException( Throwable exception){
		super(exception);
	}
	
	public GitException( String message, Throwable exception ){
		super(message, exception);
	}

}
