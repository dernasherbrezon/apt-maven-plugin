package com.st.maven.apt;

public enum Architecture {

	amd64(false), i386(false), any(true), all(true), armhf(false);
	
	private final boolean wildcard;
	
	private Architecture(boolean wildcard) {
		this.wildcard = wildcard;
	}
	
	public boolean isWildcard() {
		return wildcard;
	}

}
