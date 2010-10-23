package com.nanosheep.bikeroute.parser;

import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.zip.GZIPInputStream;
import java.io.InputStream;

import android.util.Log;

public class XMLParser {
	// names of the XML tags
	protected static final String MARKERS = "markers";
	protected static final String MARKER = "marker";

	protected URL feedUrl;

	protected XMLParser(final String feedUrl) {
		try {
			this.feedUrl = new URL(feedUrl);
		} catch (MalformedURLException e) {
			Log.e(e.getMessage(), "XML parser - " + feedUrl);
		}
	}

	protected InputStream getInputStream() {
		try {
			URLConnection feed = feedUrl.openConnection();
			feed.setRequestProperty("Accept-Encoding", "gzip");
			InputStream instream = feed.getInputStream();
			String contentEncoding = feed.getContentEncoding();
			if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
			    instream = new GZIPInputStream(instream);
			}
			return instream;
		} catch (IOException e) {
			Log.e(e.getMessage(), "XML parser - " + feedUrl);
			return null;
		}
	}
}
