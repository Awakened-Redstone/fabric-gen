package com.awakenedredstone.util.version;

@SuppressWarnings({ "deprecation", "serial" }) //Extending the deprecated one for backwards compatibility
public class VersionParsingException extends Exception {
	public VersionParsingException() {
		super();
	}

	public VersionParsingException(Throwable t) {
		super(t);
	}

	public VersionParsingException(String s) {
		super(s);
	}

	public VersionParsingException(String s, Throwable t) {
		super(s, t);
	}
}