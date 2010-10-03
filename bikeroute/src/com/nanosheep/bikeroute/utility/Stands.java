package com.nanosheep.bikeroute.utility;

import java.util.ArrayList;
import java.util.List;

import android.location.Address;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlayItem;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.parser.OSMParser;
import com.nanosheep.bikeroute.view.overlay.Marker;

/**
 * Utility class for querying cycle stands api based on gis data.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public final class Stands {

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
		for (OpenStreetMapViewOverlayItem o : getMarkers(point, 1)) {
			xD = o.mGeoPoint.getLatitudeE6() - point.getLatitudeE6();
			yD = o.mGeoPoint.getLongitudeE6() - point.getLongitudeE6();
			dist = Math.sqrt(xD*xD + yD*yD);
			if (best > dist) {
				best = dist;
				closest = o.mGeoPoint;
			}	
		}
		return closest;
	}
	
	public static GeoPoint getNearest(final Address address) {
		return getNearest(new GeoPoint(Convert.asMicroDegrees(address.getLatitude()),
				Convert.asMicroDegrees(address.getLongitude())));
	}

	/**
	 * Get markers from the api.
	 * 
	 * @param p Center point
	 * @param distance radius to collect markers within.
	 * @return an arraylist of OverlayItems corresponding to markers in range.
	 */

	public static List<OpenStreetMapViewOverlayItem> getMarkers(final GeoPoint p,
			final double distance) {
		final String query = BikeRouteConsts.OSM_API + getOSMBounds(getBounds(p, distance));
		final List<OpenStreetMapViewOverlayItem> markers = new ArrayList<OpenStreetMapViewOverlayItem>();
		final OSMParser parser = new OSMParser(query);

		// Parse XML to overlayitems (cycle stands)
		for (Marker m : parser.parse()) {
			markers.add(new OpenStreetMapViewOverlayItem(Integer.toString(m
					.getCapacity()), "", m.getLocation()));
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
		final int degrees = Convert.asMicroDegrees(((distance / BikeRouteConsts.EARTH_RADIUS) 
				* (1/BikeRouteConsts.PI_180)));
		final double latRadius = BikeRouteConsts.EARTH_RADIUS * Math.cos(degrees * BikeRouteConsts.PI_180);
		final int degreesLng = Convert.asMicroDegrees( (distance / latRadius) * (1/BikeRouteConsts.PI_180));

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
		sBuf.append(Convert.asDegrees(points.get(2).getLongitudeE6()));
		sBuf.append(',');
		sBuf.append(Convert.asDegrees(points.get(2).getLatitudeE6()));
		sBuf.append(',');
		sBuf.append(Convert.asDegrees(points.get(0).getLongitudeE6()));
		sBuf.append(',');
		sBuf.append(Convert.asDegrees(points.get(0).getLatitudeE6()));
		sBuf.append(']');

		return sBuf.toString();
	}

}
