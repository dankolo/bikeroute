package com.nanosheep.bikeroute.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.nanosheep.bikeroute.view.overlay.Marker;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

/**
 * XML parser for cycle stand queries.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class StandsParser extends XMLParser {

	protected static final String LAT = "lat";
	protected static final String LNG = "lng";
	protected static final String CAPACITY = "capacity";

	public StandsParser(final String feedUrl) {
		super(feedUrl);
	}

	public List<Marker> parse() {
		final Marker currentMarker = new Marker();
		final RootElement root = new RootElement(MARKERS);
		final List<Marker> marks = new ArrayList<Marker>();
		final Element marker = root.getChild(MARKER);
		// Listen for start of tag, get attributes and set them
		// on current marker.
		marker.setStartElementListener(new StartElementListener() {
			public void start(final Attributes attributes) {
				currentMarker.setLat(attributes.getValue(LAT));
				currentMarker.setLng(attributes.getValue(LNG));
				currentMarker.setCapacity(attributes.getValue(CAPACITY));
			}

		});
		marker.setEndElementListener(new EndElementListener() {
			public void end() {
				marks.add(currentMarker.copy());
			}
		});
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root
					.getContentHandler());
		} catch (IOException e) {
			Log.e(e.getMessage(), "Stands parser - " + feedUrl);
			} catch (SAXException e) {
			Log.e(e.getMessage(), "Stands parser - " + feedUrl);
		}
		return marks;
	}
}