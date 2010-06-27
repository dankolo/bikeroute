package com.nanosheep.bikeroute;

import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;

public class XMLParser {
	// names of the XML tags
	protected static final String MARKERS = "markers";
	protected static final String MARKER = "marker";

	protected final URL feedUrl;

	protected XMLParser(final String feedUrl) {
		try {
			this.feedUrl = new URL(feedUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	protected InputStream getInputStream() {
		try {
			URLConnection connection = feedUrl.openConnection();
			connection.setReadTimeout(10000);
			return connection.getInputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
