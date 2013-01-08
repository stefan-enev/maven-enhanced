package com.ebay.zeus.utils;

import org.apache.commons.lang.time.StopWatch;


public class TimeTracker {
	public TimeTracker(){
		clock = new StopWatch();
	}
	
	private StopWatch clock;
	
	public void start(){
		clock.reset();
		clock.start();
	}
	
	public void stop(){
		clock.stop();
	}
	
	public String getDurationString(){
		return clock.toString();
	}
}
