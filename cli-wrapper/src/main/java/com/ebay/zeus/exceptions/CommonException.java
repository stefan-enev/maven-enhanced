package com.ebay.zeus.exceptions;

public class CommonException extends Exception {

	private static final long serialVersionUID = -6877578368311715694L;
	
	public CommonException( String message ){
		super( message );
	}
	
	public CommonException( Throwable exception){
		super(exception);
	}
	
	public CommonException( String message, Throwable exception ){
		super(message, exception);
	}

}
