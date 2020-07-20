package com.st.maven.apt;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AptDeployMojoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test(expected = MojoExecutionException.class)
	public void testInvalidFile() throws Exception {
		File ar = createAr();
		AptDeployMojo.readControl(new SystemStreamLog(), ar);
	}

	@Test(expected = MojoExecutionException.class)
	public void testInvalidFile2() throws Exception {
		File ar = createAr(createFile("control.tar.xx"));
		AptDeployMojo.readControl(new SystemStreamLog(), ar);
	}
	
	@Test(expected = MojoExecutionException.class)
	public void testUnknownFile() throws Exception {
		AptDeployMojo.readControl(new SystemStreamLog(), new File(UUID.randomUUID().toString()));
	}

	@Test
	public void testControlFile() throws Exception {
		ControlFile file = AptDeployMojo.readControl(new SystemStreamLog(), new File("./src/test/resources/rtl-sdr_0.6git_armhf.deb"));
		assertEquals("rtl-sdr", file.getPackageName());
	}

	@Test
	public void testControlFileNewFormat() throws Exception {
		ControlFile file = AptDeployMojo.readControl(new SystemStreamLog(), new File("./src/test/resources/rtl-sdr_0.6_armhf.deb"));
		assertEquals("rtl-sdr", file.getPackageName());
	}

	private File createAr() throws Exception {
		return createAr(createFile("test"));
	}

	private File createAr(File entryFile) throws Exception {
		File result = new File(tempFolder.getRoot(), UUID.randomUUID().toString());
		FileOutputStream fos = new FileOutputStream(result);
		ArchiveOutputStream aos = new ArchiveStreamFactory().createArchiveOutputStream("ar", fos);
		ArchiveEntry entry = aos.createArchiveEntry(entryFile, entryFile.getName());
		aos.putArchiveEntry(entry);
		aos.write(1);
		aos.closeArchiveEntry();
		aos.close();
		return result;
	}

	private File createFile(String name) throws Exception {
		File entryFile = new File(tempFolder.getRoot(), name);
		try (FileOutputStream fos = new FileOutputStream(entryFile)) {
			fos.write(1);
		}
		return entryFile;
	}
}
