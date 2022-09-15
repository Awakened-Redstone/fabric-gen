package com.awakenedredstone.util.version;

public class StringVersion implements Version {
	private final String version;

	public StringVersion(String version) {
		this.version = version;
	}

	@Override
	public String getFriendlyString() {
		return version;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringVersion) {
			return version.equals(((StringVersion) obj).version);
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(Version o) {
		return getFriendlyString().compareTo(o.getFriendlyString());
	}

	@Override
	public String toString() {
		return version;
	}
}