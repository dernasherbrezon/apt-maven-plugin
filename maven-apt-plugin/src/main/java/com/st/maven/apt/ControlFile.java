package com.st.maven.apt;

import java.util.HashMap;
import java.util.Map;

class ControlFile {

	private String packageName;
	private String version;
	private Architecture arch;
	private String contents;

	private final Map<String, String> payload = new HashMap<String, String>();

	public Map<String, String> getPayload() {
		return payload;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Architecture getArch() {
		return arch;
	}

	public void setArch(Architecture arch) {
		this.arch = arch;
	}

	public void load(String str) {
		contents = str;
		if (contents.charAt(contents.length() - 1) != '\n') {
			contents += "\n";
		}
		String[] lines = str.split("\\r?\\n");
		for (String cur : lines) {
			String[] parts = cur.split(":");
			if (parts.length != 2) {
				continue;
			}
			String value = parts[1].trim();
			if (parts[0].equalsIgnoreCase("Package")) {
				setPackageName(value);
				continue;
			}
			if (parts[0].equalsIgnoreCase("Version")) {
				setVersion(value);
				continue;
			}
			if (parts[0].equalsIgnoreCase("Architecture")) {
				setArch(Architecture.valueOf(value));
				continue;
			}
		}
	}

	public void append(String str) {
		contents += str + "\n";
	}
}
