package com.nanosheep.bikeroute.parser;

import org.xml.sax.Attributes;
import org.andnav.osm.util.GeoPoint;

import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

/**
 * An xml parser for the CycleStreets.net journey planner API.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class CycleStreetsParser extends XMLParser implements Parser {
	/** Distance covered. **/
	private double distance;

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
		route.setCopyright("Route planning by CycleStreets.net");
		final RootElement root = new RootElement(MARKERS);
		final Element marker = root.getChild(MARKER);
		// Listen for start of tag, get attributes and set them
		// on current marker.
		marker.setStartElementListener(new StartElementListener() {
			public void start(final Attributes attributes) {
				segment.clearPoints();
				GeoPoint p;
				
				final String pointString = attributes.getValue("points");
				final String nameString = attributes.getValue("name");
				String turnString = attributes.getValue("turn");
				final String type = attributes.getValue("type");
				final String walk = attributes.getValue("walk");
				final String length = attributes.getValue("distance");
				final String totalDistance = attributes.getValue("length");
				final String elev = attributes.getValue("elevations");
				final String distances = attributes.getValue("distances");
				final String id = attributes.getValue("itinerary");
				
				/** Parse segment. **/
				if ("segment".equals(type)) {
					 StringBuffer sBuf = new StringBuffer();
					  if (!"unknown".equals(turnString)) {
						 
						  sBuf.append(Character.toUpperCase(
								  turnString.charAt(0)) + turnString.substring(1));
						  sBuf.append(" at ");
					  }
					  sBuf.append(nameString);
					  sBuf.append(' ');
					  if ("1".equals(walk)) {
						  sBuf.append("(dismount)");
					  }
					  segment.setInstruction(sBuf.toString());
									
					final String[] pointsArray = pointString.split(" ", -1);
					final String[] elevations = elev.split(",", -1);
					final String[] dists = distances.split(",", -1);
					
					//Add elevations to the elevation/distance series
					for (int i = 0; i < dists.length; i++) {
						int len = Integer.parseInt(dists[i]);
						int elevation = Integer.parseInt(elevations[i]);
						distance += len;
						route.addElevation(elevation, distance);
					}
					
					final int len = pointsArray.length;
					for (int i = 0; i < len; i++) {
						final String[] point = pointsArray[i].split(",", -1);
						p = new GeoPoint(Convert.asMicroDegrees(Double.parseDouble(point[1])), 
								Convert.asMicroDegrees(Double.parseDouble(point[0])));
						route.addPoint(p);
						segment.addPoint(p);
					}
					segment.setDistance(distance/1000);
					segment.setLength(Integer.parseInt(length));
					
					
				} else {
					/** Parse route details. **/
					route.setName(nameString);
					route.setLength(Integer.parseInt(totalDistance));
					route.setItineraryId(Integer.parseInt(id));
				}
			}
		});
		marker.setEndElementListener(new EndElementListener() {
			public void end() {
				if (segment.getInstruction() != null) {
					route.addSegment(segment.copy());
				}
			}
		});
		try {
			Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root
					.getContentHandler());
		} catch (Exception e) {
			Log.e(e.getMessage(), "CycleStreets parser - " + feedUrl);
			return null;
		}
		//route.buildTree();
		return route;
	}
}