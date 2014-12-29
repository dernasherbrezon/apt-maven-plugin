package com.st.maven.apt;

public enum Architecture {

	amd64, i386, any, all;

	public boolean supports(Architecture other) {
		if (this == other || other == any || other == all) {
			return true;
		}
		return false;
	}

}
