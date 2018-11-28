package com.st.maven.apt;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ReleaseTest {

	@Test
	public void load() throws Exception {
		Release release = new Release();
		release.load(ReleaseTest.class.getClassLoader().getResourceAsStream("Release"));
		assertFalse(release.getFiles().isEmpty());
	}
}
