package com.nanosheep.bikeroute;

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
	private Activity act;
	/** Map view to draw routes into. **/
	private MapView mv;
	/** API feed. */
	private static final String API =
		"http://vega.soi.city.ac.uk/~abjy800/bike/cs.php?";
	/** Route overlay. **/
	private RouteOverlay routeOverlay;
	/** Route planned switch. **/
	private boolean isPlanned;
	/** Route. **/
	private Route route;
	
	public RouteManager(final Activity activity, final MapView mapview) {
		super();
		act = activity;
		mv = mapview;
		setPlanned(false);
	}
	
	/**
	 * Plan a route between the points given and show it on the map. Displays an
	 * alert if the planning failed for some reason.
	 * Executes planning process in a separate thread, displays a progress
	 * dialog while planning.
	 * 
	 * @param start Starting point.
	 * @param dest Destination point.
	 */

	public void showRoute(final GeoPoint start, final GeoPoint dest) {
		clearRoute();
		
		act.showDialog(BikeNav.PLANNING_DIALOG);
		final Thread thread = new Thread() {
			public void run() {
				Message msg = messageHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("result", 1);
				try {
					route = plan(start, dest);
					routeOverlay = new RouteOverlay(route, Color.BLUE);
				} catch (Exception e) {
					b.putInt("result", 0); 
				} finally {
					msg.setData(b);
	                messageHandler.sendMessage(msg);
	                interrupt();
				}
			}
		};
		thread.start();
	}
	
	/**
	 * Handler for route planning thread.
	 */
	
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			act.dismissDialog(BikeNav.PLANNING_DIALOG);
			if (msg.getData().getInt("result") == 0) {
				act.showDialog(BikeNav.PLAN_FAIL_DIALOG);
			} else {
				mv.getOverlays().add(routeOverlay);
				mv.invalidate();
				isPlanned = true;
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
		final StringBuffer sBuf = new StringBuffer(API);
		sBuf.append("start_lat=");
		sBuf.append(Degrees.asDegrees(start.getLatitudeE6()));
		sBuf.append("&start_lng=");
		sBuf.append(Degrees.asDegrees(start.getLongitudeE6()));
		sBuf.append("&dest_lat=");
		sBuf.append(Degrees.asDegrees(dest.getLatitudeE6()));
		sBuf.append("&dest_lng=");
		sBuf.append(Degrees.asDegrees(dest.getLongitudeE6()));

		final CycleStreetsParser parser = new CycleStreetsParser(sBuf
				.toString());
		return parser.parse();
	}
	
	/**
	 * Clear the current route.
	 */

	public void clearRoute() {
		mv.getOverlays().remove(routeOverlay);
		routeOverlay = null;
		isPlanned = false;
	}

	/**
	 * @param route the route to set
	 */
	public void setRoute(Route route) {
		this.route = route;
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
	public void setPlanned(boolean isPlanned) {
		this.isPlanned = isPlanned;
	}

	/**
	 * @return the isPlanned
	 */
	public boolean isPlanned() {
		return isPlanned;
	}

}
