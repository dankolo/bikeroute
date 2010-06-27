package com.nanosheep.bikeroute;

import java.util.Locale;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * Plans routes and displays them as overlays on the provided mapview.
 * 
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */
public class RouteManager {
	/** Owning activity. **/
	private final Activity act;
	/** Map view to draw routes into. **/
	private final MapView mv;
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
	
	public RouteManager(final Activity activity, final MapView mapview) {
		super();
		act = activity;
		mv = mapview;
		planned = false;
	}
	
	/**
	 * Plan a route between the points given and show it on the map. Displays an
	 * alert if the planning failed for some reason.
	 * Executes planning process in a separate thread, displays a progress
	 * dialog while planning.
	 */

	public void showRoute() {
		clearRoute();
		
		act.showDialog(BikeNav.PLANNING_DIALOG);
		final Thread thread = new Thread() {
			public void run() {
				Message msg = RouteManager.this.messageHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("result", 1);
				try {
					RouteManager.this.route = plan(start, dest);
					RouteManager.this.route.setCountry(country);
					RouteManager.this.routeOverlay = new RouteOverlay(route, Color.BLUE);
				} catch (Exception e) {
					b.putInt("result", 0); 
				} finally {
					msg.setData(b);
					RouteManager.this.messageHandler.sendMessage(msg);
	                interrupt();
				}
			}
		};
		thread.start();
	}
	
	/**
	 * Handler for route planning thread.
	 */
	
	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			act.dismissDialog(BikeNav.PLANNING_DIALOG);
			if (msg.getData().getInt("result") == 0) {
				act.showDialog(BikeNav.PLAN_FAIL_DIALOG);
			} else {
				mv.getOverlays().add(routeOverlay);
				mv.invalidate();
				mv.getController().animateTo(route.getPoints().get(0));
				planned = true;
			}
		}
	};

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
		mv.getOverlays().remove(routeOverlay);
		routeOverlay = null;
		planned = false;
	}

	/**
	 * @param route the route to set
	 */
	public void setRoute(final Route route) {
		this.route = route;
		routeOverlay = new RouteOverlay(route, Color.BLUE);
		mv.getOverlays().add(routeOverlay);
		mv.invalidate();
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
	 * Set the destination point for the route.
	 * @param end point.
	 */
	
	public void setDest(final GeoPoint end) {
		this.dest = end;
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

}
