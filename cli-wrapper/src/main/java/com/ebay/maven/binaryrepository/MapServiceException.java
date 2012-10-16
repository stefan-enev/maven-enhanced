package com.ebay.maven.binaryrepository;

public class MapServiceException extends Exception{

	private static final long serialVersionUID = 5527311814141305691L;

	public MapServiceException( String message ){
		super( message );
	}
	
	public MapServiceException( Throwable exception){
		super(exception);
	}
	
	public MapServiceException( String message, Throwable exception ){
		super(message, exception);
	}
}
