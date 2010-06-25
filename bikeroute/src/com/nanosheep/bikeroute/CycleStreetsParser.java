package com.nanosheep.bikeroute;

import org.xml.sax.Attributes;
import com.google.android.maps.GeoPoint;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

/**
 * An xml parser for the CycleStreets.net journey planner API.
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
	
	public final Route parse() {
		final Segment segment = new Segment();
		final Route route = new Route();
		final RootElement root = new RootElement(MARKERS);
		final Element marker = root.getChild(MARKER);
		// Listen for start of tag, get attributes and set them
		// on current marker.
		marker.setStartElementListener(new StartElementListener() {
			public void start(final Attributes attributes) {
				GeoPoint p;
				
				final String pointString = attributes.getValue("points");
				final String nameString = attributes.getValue("name");
				String turnString = attributes.getValue("turn");
				final String type = attributes.getValue("type");
				final String geom = attributes.getValue("coordinates");
				final String walk = attributes.getValue("walk");
				final String length = attributes.getValue("distance");
				
				/** Parse segment. **/
				if ("segment".equals(type)) {
					segment.setName(nameString);
									
					final String[] pointsArray = pointString.split(" ", -1);
					
					//Split points string to geopoints list.
					final String[] point = pointsArray[0].split(",", -1);

					p = new GeoPoint(Degrees.asMicroDegrees(Double.parseDouble(point[1])), 
							Degrees.asMicroDegrees(Double.parseDouble(point[0])));
				
					segment.setPoint(p);
					turnString = Character.toUpperCase(turnString.charAt(0)) + turnString.substring(1);
					segment.setTurn(turnString);
					if ("1".equals(walk)) {
						segment.setWalk(true);
					} else {
						segment.setWalk(false);
					}
					segment.setLength(Integer.parseInt(length));
					
				} else {
					/** Parse route details. **/
					route.setName(nameString);
					final String[] pointsArray = geom.split(" ", -1);
					final int len = pointsArray.length;
					for (int i = 0; i < len; i++) {
						final String[] point = pointsArray[i].split(",", -1);
						p = new GeoPoint(Degrees.asMicroDegrees(Double.parseDouble(point[1])), 
								Degrees.asMicroDegrees(Double.parseDouble(point[0])));
						route.addPoint(p);
					}
				}
			}
		});
		marker.setEndElementListener(new EndElementListener() {
			public void end() {
				if (segment.getName() != null) {
					route.addSegment(segment.copy());
				}
			}
		});
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root
					.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return route;
	}
}