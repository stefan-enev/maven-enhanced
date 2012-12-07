package com.ebay.maven.utils;

import java.net.URL;

import org.apache.maven.model.Model;
import org.junit.Assert;
import org.junit.Test;

import com.ebay.zeus.utils.PomUtils;

public class PomUtilsTest {
	
	@Test
	public void readModel(){
		
		URL pomUrl = PomUtilsTest.class.getClassLoader().getResource("test1/pom.xml");
		Model model = PomUtils.readModel(pomUrl.getFile());
		
		Assert.assertNotNull(model);
	}

}
