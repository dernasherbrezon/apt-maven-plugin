package com.st.maven.apt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

class Packages {

	private String architecture;
	private final Map<String, ControlFile> contents = new HashMap<String, ControlFile>();

	void load(InputStream is) throws IOException {
		String curLine = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuilder currentControl = new StringBuilder();
		while ((curLine = r.readLine()) != null) {
			if (curLine.trim().length() == 0) {
				String currentControlStr = currentControl.toString();
				currentControl = new StringBuilder();
				if (currentControlStr.trim().length() != 0) {
					ControlFile curFile = new ControlFile();
					curFile.load(currentControlStr);
					contents.put(curFile.getPackageName(), curFile);
				}
				continue;
			}
			currentControl.append(curLine).append("\n");
		}
	}

	void save(OutputStream os) throws IOException {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
		for (ControlFile cur : contents.values()) {
			w.append(cur.getContents()).append("\n");
		}
		w.flush();
	}

	void add(ControlFile file) {
		contents.put(file.getPackageName(), file);
	}
	
	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}
	
	public String getArchitecture() {
		return architecture;
	}
	

}
