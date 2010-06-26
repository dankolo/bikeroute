package com.nanosheep.bikeroute;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.OverlayItem;
import com.google.android.maps.GeoPoint;

/**
 * Utility class for querying cycle stands api based on gis data.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public final class Stands {

	private static final String QUERY_URL = "http://vega.soi.city.ac.uk/~abjy800/bike/xml.php?poly=";
	private static final String OSM_API =
		"http://www.informationfreeway.org/api/0.6/node[amenity=bicycle_parking]";

	private Stands() {
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
		// URL encode the WKT representation of the MBR for the circle
		// described by the center & distance given.
		//final String poly = getWKTPoly(getBounds(p, distance));
		//final String query = QUERY_URL + URLEncoder.encode(poly);
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
	 * @return an array of 4 geopoints.
	 */
	private static List<GeoPoint> getBounds(final GeoPoint p, final double distance) {
		final List<GeoPoint> points = new ArrayList<GeoPoint>(4);
		final double pi180 = Math.PI / 180;
		final double earthRadius = 3960.0;
		final int degrees = Degrees.asMicroDegrees(((distance / earthRadius) * (1/pi180)));
		final double latRadius = earthRadius * Math.cos(degrees * pi180);
		final int degreesLng = Degrees.asMicroDegrees( (distance / latRadius) * (1/pi180));

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
	 * Get a WKT polygon representation of an array of geopoints.
	 * 
	 * @param points
	 * @return a WKT string of the form POLYGON(..
	 */

	@Deprecated
	private static String getWKTPoly(final List<GeoPoint> points) {
		final StringBuffer sBuf = new StringBuffer("POLYGON((");
        for (GeoPoint p : points) {
                sBuf.append(p.getLatitudeE6() / Degrees.CNV);
                sBuf.append(' ');
                sBuf.append(p.getLongitudeE6() / Degrees.CNV);
                sBuf.append(", ");
        }
        final GeoPoint pnt = points.get(0);
        sBuf.append(pnt.getLatitudeE6() / Degrees.CNV);
        sBuf.append(' ');
        sBuf.append(pnt.getLongitudeE6() / Degrees.CNV);
        sBuf.append("))");

        return sBuf.toString();
	}
	
	/**
	 * Get an OSM bounding box string of an array of GeoPoints.
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
