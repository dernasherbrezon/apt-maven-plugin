package com.st.maven.apt;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public class ControlFileTest {

	@Test
	public void testNoSpaceAfterDescription() throws Exception {
		ControlFile controlFile = new ControlFile();
		controlFile.load(loadFile("control.txt"));
		controlFile.append("Filename: /dist/file.gz");
		assertEquals(loadFile("expectedControl.txt"), controlFile.getContents());
	}

	private static String loadFile(String name) throws IOException, UnsupportedEncodingException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		IOUtils.copy(ControlFileTest.class.getClassLoader().getResourceAsStream(name), outputStream);
		String contentString = outputStream.toString("UTF-8");
		outputStream.close();
		return contentString;
	}

}
