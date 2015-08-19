package com.st.maven.apt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

class Release {

	private final static Pattern SPACE = Pattern.compile(" ");

	private String origin;
	private String label;
	private String codename;
	private String date;
	private String architectures;
	private String components;
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

	public String getArchitectures() {
		return architectures;
	}

	public void setArchitectures(String architectures) {
		this.architectures = architectures;
	}

	public String getComponents() {
		return components;
	}

	public void setComponents(String components) {
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
			if (line.charAt(0) == ' ') {
				if (curGroup != null) {
					String[] parts = SPACE.split(line.substring(1));
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
			if (parts[0].equals("Origin")) {
				origin = parts[1];
			} else if (parts[0].equals("Label")) {
				label = parts[1];
			} else if (parts[0].equals("Codename")) {
				codename = parts[1];
			} else if (parts[0].equals("Date")) {
				date = parts[1];
			} else if (parts[0].equals("Architectures")) {
				architectures = parts[1];
			} else if (parts[0].equals("Components")) {
				components = parts[1];
			} else if (parts[0].equals("MD5Sum") || parts[0].equals("SHA1") || parts[0].equals("SHA256")) {
				curGroup = parts[0];
			} else {
				unknown.add(line);
			}
		}
		files = new HashSet<FileInfo>(fileInfoByFilename.size());
		files.addAll(fileInfoByFilename.values());
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
		w.append("Label: ").append(label).append("\n");
		w.append("Codename: ").append(codename).append("\n");
		w.append("Date: ").append(date).append("\n");
		w.append("Architectures: ").append(architectures).append("\n");
		w.append("Components: ").append(components).append("\n");
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