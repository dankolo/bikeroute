package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.android.maps.GeoPoint;

import android.sax.Element;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

/**
 * An xml parser for the CycleStreets.net journey planner API.
 * Presently only parses to a path.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class CycleStreetsParser extends XMLParser {

	public CycleStreetsParser(final String feedUrl) {
		super(feedUrl);
	}

	/**
	 * Parse the query URL to a list of GeoPoints.
	 * @return a list of geopoints corresponding to the routeplan.
	 */
	
	public List<GeoPoint> parse() {
		final RootElement root = new RootElement(MARKERS);
		final List<GeoPoint> points = new ArrayList<GeoPoint>();
		final Element marker = root.getChild(MARKER);
		// Listen for start of tag, get attributes and set them
		// on current marker.
		marker.setStartElementListener(new StartElementListener() {
			public void start(final Attributes attributes) {
				final String pointString = attributes.getValue("points");
				if (pointString != null) {
					final String[] pointsArray = pointString.split(" ", -1);
					for (String s : pointsArray) {
						final String[] point = s.split(",", -1);
						final double lat = new Double(point[1]);
						final double lng = new Double(point[0]);
						points.add(new GeoPoint((int) (lat * Degrees.CNV),
								(int) (lng * Degrees.CNV)));
					}
				}
			}
		});
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root
					.getContentHandler());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		return points;
	}
}