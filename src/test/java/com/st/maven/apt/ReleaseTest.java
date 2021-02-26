package com.st.maven.apt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseTest {

	@Test
	public void load() throws Exception {
		Release release = new Release();
		release.load(ReleaseTest.class.getClassLoader().getResourceAsStream("Release"));
		assertFalse(release.getFiles().isEmpty());
		assertTrue(release.getComponents().contains("main"));
		assertTrue(release.getArchitectures().contains("amd64"));
		assertTrue(release.getArchitectures().contains("armhf"));
		assertTrue(release.getArchitectures().contains("i386"));
	}
}
