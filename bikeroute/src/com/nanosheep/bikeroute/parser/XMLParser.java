package com.nanosheep.bikeroute.parser;

import java.net.URL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;

import android.util.Log;

public class XMLParser {
	// names of the XML tags
	protected static final String MARKERS = "markers";
	protected static final String MARKER = "marker";

	protected final URL feedUrl;

	protected XMLParser(final String feedUrl) {
		try {
			this.feedUrl = new URL(feedUrl);
		} catch (MalformedURLException e) {
			Log.e(e.getMessage(), "XML parser - " + feedUrl);
			throw new RuntimeException(e.getMessage());
		}
	}

	protected InputStream getInputStream() {
		try {
			return feedUrl.openConnection().getInputStream();
		} catch (IOException e) {
			Log.e(e.getMessage(), "XML parser - " + feedUrl);
			return null;
		}
	}
}
