package com.nanosheep.bikeroute.utility;

import java.util.ArrayList;
import java.util.List;

import android.location.Address;

import com.google.android.maps.OverlayItem;
import com.google.android.maps.GeoPoint;
import com.nanosheep.bikeroute.overlay.Marker;
import com.nanosheep.bikeroute.parser.OSMParser;

/**
 * Utility class for querying cycle stands api based on gis data.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public final class Stands {
	/** API url. OpenStreetMap xapi interface. **/
	private static final String OSM_API =
		"http://www.informationfreeway.org/api/0.6/node[amenity=bicycle_parking]";
	/** Pi/180 for converting degrees - radians. **/
	private static final double PI_180 = Math.PI / 180;
	/** Radius of the earth for degrees - miles calculations. **/
	private static final double EARTH_RADIUS = 3960.0;

	private Stands() {
	}
	
	/**
	 * Find the nearest cycle stand to the point given, or
	 * return null if there's not one in a mile.
	 * @param point GeoPoint to search near
	 * @return a GeoPoint representing the cycle stand position or null.
	 */
	
	public static GeoPoint getNearest(final GeoPoint point) {
		GeoPoint closest = null;
		double xD;
		double yD;
		double best = 9999999;
		double dist;
		for (OverlayItem o : getMarkers(point, 1)) {
			xD = o.getPoint().getLatitudeE6() - point.getLatitudeE6();
			yD = o.getPoint().getLongitudeE6() - point.getLongitudeE6();
			dist = Math.sqrt(xD*xD + yD*yD);
			if (best > dist) {
				best = dist;
				closest = o.getPoint();
			}	
		}
		return closest;
	}
	
	public static GeoPoint getNearest(final Address address) {
		GeoPoint p = new GeoPoint(Degrees.asMicroDegrees(address.getLatitude()),
				Degrees.asMicroDegrees(address.getLongitude()));
		return getNearest(p);
	}

	/**
	 * Get markers from the api.
	 * 
	 * @param p Center point
	 * @param distance radius to collect markers within.
	 * @return an arraylist of OverlayItems corresponding to markers in range.
	 */

	public static List<OverlayItem> getMarkers(final GeoPoint p,
			final double distance) {
		final String query = OSM_API + getOSMBounds(getBounds(p, distance));
		final List<OverlayItem> markers = new ArrayList<OverlayItem>();
		final OSMParser parser = new OSMParser(query);

		// Parse XML to overlayitems (cycle stands)
		for (Marker m : parser.parse()) {
			markers.add(new OverlayItem(m.getLocation(), Integer.toString(m
					.getCapacity()), ""));
		}

		return markers;
	}

	/**
	 * Generate an array of points representing a MBR for the circle described
	 * by the radius given.
	 * 
	 * @param p point to use as center
	 * @param distance radius to bound within.
	 * @return an array of 4 geopoints representing an mbr drawn clockwise from the ne corner.
	 */
	private static List<GeoPoint> getBounds(final GeoPoint p, final double distance) {
		final List<GeoPoint> points = new ArrayList<GeoPoint>(4);
		final int degrees = Degrees.asMicroDegrees(((distance / EARTH_RADIUS) * (1/PI_180)));
		final double latRadius = EARTH_RADIUS * Math.cos(degrees * PI_180);
		final int degreesLng = Degrees.asMicroDegrees( (distance / latRadius) * (1/PI_180));

		final int maxLng = degreesLng + p.getLongitudeE6();
		final int maxLat = degrees + p.getLatitudeE6();

		final int minLng = p.getLongitudeE6() - degreesLng;
		final int minLat = p.getLatitudeE6() - degrees;

		points.add(new GeoPoint(maxLat, maxLng));
		points.add(new GeoPoint(maxLat, minLng));
		points.add(new GeoPoint(minLat, minLng));
		points.add(new GeoPoint(minLat, maxLng));

		return points;
	}
	
	/**
	 * Get an OSM bounding box string of an array of GeoPoints representing
	 * a bounding box drawn clockwise from the northeast.
	 * @param points List of geopoints
	 * @return a string in OSM xapi bounding box form.
	 */
	
	private static String getOSMBounds(final List<GeoPoint> points) {
		final StringBuffer sBuf = new StringBuffer("[bbox=");
		sBuf.append(Degrees.asDegrees(points.get(2).getLongitudeE6()));
		sBuf.append(',');
		sBuf.append(Degrees.asDegrees(points.get(2).getLatitudeE6()));
		sBuf.append(',');
		sBuf.append(Degrees.asDegrees(points.get(0).getLongitudeE6()));
		sBuf.append(',');
		sBuf.append(Degrees.asDegrees(points.get(0).getLatitudeE6()));
		sBuf.append(']');

		return sBuf.toString();
	}

}
