package com.ebay.zeus.exceptions;

public class ZeusException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1544781278008748196L;

	public ZeusException( String message ){
		super( message );
	}
	
	public ZeusException( Throwable exception){
		super(exception);
	}
	
	public ZeusException( String message, Throwable exception ){
		super(message, exception);
	}

}
