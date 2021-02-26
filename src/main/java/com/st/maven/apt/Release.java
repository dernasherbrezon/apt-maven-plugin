package com.st.maven.apt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

class Release {

	private final static Pattern SPACE = Pattern.compile("\\s+");

	private String origin;
	private String label;
	private String codename;
	private String date;
	private Set<String> architectures = new HashSet<>();
	private Set<String> components = new HashSet<>();
	private Set<FileInfo> files = new HashSet<FileInfo>();
	private List<String> unknown = new ArrayList<String>();

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getCodename() {
		return codename;
	}

	public void setCodename(String codename) {
		this.codename = codename;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public Set<String> getArchitectures() {
		return architectures;
	}

	public void setArchitectures(Set<String> architectures) {
		this.architectures = architectures;
	}

	public Set<String> getComponents() {
		return components;
	}

	public void setComponents(Set<String> components) {
		this.components = components;
	}

	public Set<FileInfo> getFiles() {
		return files;
	}

	public void setFiles(Set<FileInfo> files) {
		this.files = files;
	}

	void load(InputStream is) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String curLine = null;
		String curGroup = null;
		Map<String, FileInfo> fileInfoByFilename = new HashMap<String, FileInfo>();
		while ((curLine = r.readLine()) != null) {
			String line = curLine.trim();
			if (line.length() == 0) {
				continue;
			}
			if (curLine.charAt(0) == ' ') {
				if (curGroup != null) {
					String[] parts = SPACE.split(line);
					if (parts.length != 3) {
						throw new IOException("unsupported format: " + line + " expected: <checksum> <file size> <file name>");
					}
					FileInfo info = fileInfoByFilename.get(parts[2]);
					if (info == null) {
						info = new FileInfo();
						info.setFilename(parts[2]);
						info.setSize(parts[1]);
						fileInfoByFilename.put(info.getFilename(), info);
					}
					if (curGroup.equals("MD5Sum")) {
						info.setMd5(parts[0]);
					} else if (curGroup.equals("SHA1")) {
						info.setSha1(parts[0]);
					} else if (curGroup.equals("SHA256")) {
						info.setSha256(parts[0]);
					} else {
						throw new IOException("unsupported checksum: " + curGroup);
					}
				} else {
					unknown.add(line);
				}
				continue;
			}
			String[] parts = splitByColon(line);
			String name = parts[0];
			String value = parts[1].trim();
			if (name.equals("Origin")) {
				origin = value;
			} else if (name.equals("Label")) {
				label = value;
			} else if (name.equals("Codename")) {
				codename = value;
			} else if (name.equals("Date")) {
				date = value;
			} else if (name.equals("Architectures")) {
				architectures = splitBySpace(value);
			} else if (name.equals("Components")) {
				components = splitBySpace(value);
			} else if (name.equals("MD5Sum") || name.equals("SHA1") || name.equals("SHA256")) {
				curGroup = name;
			} else {
				unknown.add(line);
			}
		}
		files = new HashSet<FileInfo>(fileInfoByFilename.size());
		files.addAll(fileInfoByFilename.values());
	}

	private static Set<String> splitBySpace(String line) {
		Set<String> result = new HashSet<>();
		String[] parts = line.split(" ");
		for (String cur : parts) {
			if (cur.trim().length() == 0) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}

	private static String joinBySpace(Set<String> set) {
		List<String> sorted = new ArrayList<>(set);
		Collections.sort(sorted);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < sorted.size(); i++) {
			if (i != 0) {
				result.append(" ");
			}
			result.append(sorted.get(i));
		}
		return result.toString();
	}

	private static String[] splitByColon(String line) {
		int index = line.indexOf(':');
		if (index == -1) {
			return new String[] { line };
		}
		String[] result = new String[2];
		result[0] = line.substring(0, index);
		result[1] = line.substring(index + 1);
		return result;
	}

	void save(OutputStream os) throws IOException {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
		w.append("Origin: ").append(origin).append("\n");
		if (label != null) {
			w.append("Label: ").append(label).append("\n");
		}
		w.append("Codename: ").append(codename).append("\n");
		w.append("Date: ").append(date).append("\n");
		w.append("Architectures: ").append(joinBySpace(architectures)).append("\n");
		w.append("Components: ").append(joinBySpace(components)).append("\n");
		if (!files.isEmpty()) {
			w.append("MD5Sum:\n");
			for (FileInfo cur : files) {
				w.append(" ").append(cur.getMd5()).append(" ").append(cur.getSize()).append(" ").append(cur.getFilename()).append("\n");
			}
			w.append("SHA1:\n");
			for (FileInfo cur : files) {
				w.append(" ").append(cur.getSha1()).append(" ").append(cur.getSize()).append(" ").append(cur.getFilename()).append("\n");
			}
			w.append("SHA256:\n");
			for (FileInfo cur : files) {
				w.append(" ").append(cur.getSha256()).append(" ").append(cur.getSize()).append(" ").append(cur.getFilename()).append("\n");
			}
		}
		w.flush();
	}
}
