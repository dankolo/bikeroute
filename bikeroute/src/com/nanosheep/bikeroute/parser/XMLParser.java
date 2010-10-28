package com.nanosheep.bikeroute.parser;

import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.zip.GZIPInputStream;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

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
			//URLConnection feed = feedUrl.openConnection();
			//feed.setRequestProperty("Accept-Encoding", "gzip");
			HttpUriRequest request = new HttpGet(feedUrl.toString());
			request.addHeader("Accept-Encoding", "gzip");
	        final HttpResponse response = new DefaultHttpClient().execute(request);
	        Header ce = response.getFirstHeader("Content-Encoding");
	        String contentEncoding = null;
	        if (ce != null) {
	        	contentEncoding = ce.getValue();
	        }
	         InputStream instream = response.getEntity().getContent();
			//InputStream instream = feed.getInputStream();
			//String contentEncoding = feed.getContentEncoding();
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
