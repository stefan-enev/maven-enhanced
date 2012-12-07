package com.ebay.zeus.exceptions;

public class ProcessException  extends Exception {

	private static final long serialVersionUID = 6148566014980465421L;

	public ProcessException(String string, Exception e) {
            super(string,e);
    }

    public ProcessException(String string) {
            super(string);
    }

    public ProcessException( Exception e ){
            super(e);
    }
}
