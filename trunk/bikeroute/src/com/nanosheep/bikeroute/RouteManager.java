package com.nanosheep.bikeroute;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.nanosheep.bikeroute.overlay.RouteOverlay;
import com.nanosheep.bikeroute.parser.CycleStreetsParser;
import com.nanosheep.bikeroute.parser.GoogleParser;
import com.nanosheep.bikeroute.parser.Parser;
import com.nanosheep.bikeroute.utility.Degrees;

/**
 * Plans routes and displays them as overlays on the provided mapview.
 * 
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */
public class RouteManager {
	/** Owning activity. **/
	private final Activity act;
	/** API feed. */
	private static final String API =
		"http://vega.soi.city.ac.uk/~abjy800/bike/cs.php?";
	/** US API. **/
	private static final String US_API =
		"http://maps.google.com/maps/api/directions/json?";
	/** Route overlay. **/
	private RouteOverlay routeOverlay;
	/** Route planned switch. **/
	private boolean planned;
	/** Route. **/
	private Route route;
	/** Start point. **/
	private GeoPoint start;
	/** Destination point. **/
	private GeoPoint dest;
	/** Country the route is in. **/
	private String country;
	/** Geocoder. **/
	private Geocoder geocoder;
	
	public RouteManager(final Activity activity) {
		super();
		act = activity;
		planned = false;
		geocoder = new Geocoder(act);
	}
	
	/**
	 * Plan a route between the points given and show it on the map. Displays an
	 * alert if the planning failed for some reason.
	 * Executes planning process in a separate thread, displays a progress
	 * dialog while planning.
	 */

	public boolean showRoute() {
		clearRoute();
		try {
			country = geocoder
					.getFromLocation(Degrees.asDegrees(dest.getLatitudeE6()),
							Degrees.asDegrees(dest.getLongitudeE6()), 1)
					.get(0).getCountryCode();
			route = plan(start, dest);
			route.setCountry(country);
			if (RouteManager.this.route.getPoints().isEmpty()) {
				throw new PlanException("Route is empty.");
			}
		} catch (Exception e) {
			Log.e(e.getMessage(), "Planner");
			return false;
		}
		return true;
	}

	/**
	 * Plan a route from the start point to a destination.
	 * 
	 * @param start Start point.
	 * @param dest Destination.
	 * @return a list of segments for the route.
	 */

	private Route plan(final GeoPoint start, final GeoPoint dest) {
		Parser parser;
		if ("GB".equals(country)) {
		final StringBuffer sBuf = new StringBuffer(API);
		sBuf.append("start_lat=");
		sBuf.append(Degrees.asDegrees(start.getLatitudeE6()));
		sBuf.append("&start_lng=");
		sBuf.append(Degrees.asDegrees(start.getLongitudeE6()));
		sBuf.append("&dest_lat=");
		sBuf.append(Degrees.asDegrees(dest.getLatitudeE6()));
		sBuf.append("&dest_lng=");
		sBuf.append(Degrees.asDegrees(dest.getLongitudeE6()));

		parser = new CycleStreetsParser(sBuf
				.toString());
		} else {
			final StringBuffer sBuf = new StringBuffer(US_API);
			sBuf.append("origin=");
			sBuf.append(Degrees.asDegrees(start.getLatitudeE6()));
			sBuf.append(',');
			sBuf.append(Degrees.asDegrees(start.getLongitudeE6()));
			sBuf.append("&destination=");
			sBuf.append(Degrees.asDegrees(dest.getLatitudeE6()));
			sBuf.append(',');
			sBuf.append(Degrees.asDegrees(dest.getLongitudeE6()));
			if ("US".equals(country)) {
				sBuf.append("&sensor=true&mode=bicycling");
			} else {
				sBuf.append("&sensor=true&mode=driving");
			}
		parser = new GoogleParser(sBuf.toString());
		} 
		Route r =  parser.parse();
		return r;
	}
	
	/**
	 * Clear the current route.
	 */

	public void clearRoute() {
		routeOverlay = null;
		planned = false;
	}

	/**
	 * @param route the route to set
	 */
	public void setRoute(final Route route) {
		this.route = route;
		routeOverlay = new RouteOverlay(route, Color.BLUE);
		planned = true;
	}

	/**
	 * @return the route
	 */
	public Route getRoute() {
		return route;
	}

	/**
	 * @param isPlanned the isPlanned to set
	 */
	public void setPlanned(final boolean isPlanned) {
		this.planned = isPlanned;
	}

	/**
	 * @return the isPlanned
	 */
	public boolean isPlanned() {
		return planned;
	}
	
	/**
	 * @return the starting geopoint
	 */
	
	public GeoPoint getStart() {
		return start;
	}
	
	/**
	 * @return the destination geopoint.
	 */
	
	public GeoPoint getDest() {
		return dest;
	}
	
	/**
	 * Set the start point for the route.
	 * @param start start point.
	 */
	
	public void setStart(final GeoPoint start) {
		this.start = start;
	}
	
	/**
	 * Set a start point which is an address.
	 * @param address
	 */
	
	public void setStart(final Address address) {
		GeoPoint p = new GeoPoint(Degrees.asMicroDegrees(address.getLatitude()),
				Degrees.asMicroDegrees(address.getLongitude()));
		setStart(p);
	}
	
	/**
	 * Set a destination point which is an address
	 * @param address
	 */
	
	public void setDest(final Address address) {
		GeoPoint p = new GeoPoint(Degrees.asMicroDegrees(address.getLatitude()),
				Degrees.asMicroDegrees(address.getLongitude()));
		setDest(p);
	}
	
	/**
	 * Set a start point which is a location
	 * @param location
	 */
	
	public void setStart(final Location location) {
		GeoPoint p = new GeoPoint(Degrees.asMicroDegrees(location.getLatitude()),
				Degrees.asMicroDegrees(location.getLongitude()));
		setStart(p);
	}
	
	/**
	 * Set a destination which is a location.
	 * @param location
	 */
	
	public void setDest(final Location location) {
		GeoPoint p = new GeoPoint(Degrees.asMicroDegrees(location.getLatitude()),
				Degrees.asMicroDegrees(location.getLongitude()));
		setDest(p);
	}
	
	/**
	 * Set the destination point for the route.
	 * @param end point.
	 */
	
	public void setDest(final GeoPoint end) {
		this.dest = end;
	}
	
	public void setDest(final String name) throws IOException {
		Address address = geocoder.getFromLocationName(
				name, 1).get(0);
		setDest(address);
	}
	
	public void setStart(final String name) throws IOException {
		Address address = geocoder.getFromLocationName(
				name, 1).get(0);
		setStart(address);
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	
	/**
	 * Exception type for route planning exceptions.
	 * 
	 * @author jono@nanosheep.net
	 * @version Jun 27, 2010
	 */
	private class PlanException extends Exception {

		/**
		 * Empty planexception.
		 */
		public PlanException() {
		}

		/**
		 * PlanException with specified message.
		 * @param detailMessage
		 */
		public PlanException(String detailMessage) {
			super(detailMessage);
		}

	}

}
