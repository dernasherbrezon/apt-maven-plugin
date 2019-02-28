package com.st.maven.apt;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class PackagesTest {

	@Test
	public void load() throws Exception {
		Packages packages = new Packages();
		packages.load(PackagesTest.class.getClassLoader().getResourceAsStream("Packages"));
		assertNotNull(packages.getContents().get("r2cloud-ui"));
	}
	
}
