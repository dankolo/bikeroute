/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
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
public class RouteManager extends BroadcastReceiver {
	private Activity act;
	private MapView mv;
	/** API feed. */
	private static final String API = "http://vega.soi.city.ac.uk/~abjy800/bike/cs.php?";
	/** Route overlay. **/
	private RouteOverlay route;
	/** Point -> PI map. **/
	private Map<ArrayList<Integer>, PendingIntent> pendingIntents;
	/** Name for bike alert intents. **/
	private static final String INTENT_ID = "com.nanosheep.bikeroute.GPS_ALERT";
	/** Intent for alerts. **/
	private Intent intent;
	
	public RouteManager (Activity activity, MapView mapview) {
		act = activity;
		mv = mapview;
		pendingIntents = new HashMap<ArrayList<Integer>, PendingIntent>();
		intent = new Intent(INTENT_ID);
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
		Thread thread = new Thread() {
			public void run() {
				List<GeoPoint> points = new ArrayList<GeoPoint>();
				Message msg = messageHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt("result", 1);
				try {
					points = plan(start, dest);
					setAlerts(points);
					route = new RouteOverlay(points, Color.BLUE);
				} catch (Exception e) {
					b.putInt("result", 0); 
				} finally {
					msg.setData(b);
	                messageHandler.sendMessage(msg);
				}
			}
		};
		thread.start();
	}
	
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {			
			act.dismissDialog(BikeNav.PLANNING_DIALOG);
			if(msg.getData().getInt("result") == 0) {
				act.showDialog(BikeNav.PLAN_FAIL_DIALOG);
			} else {
				mv.getOverlays().add(route);
				mv.invalidate();
			}
		}
	};

	/**
	 * Plan a route from here to a destination.
	 * 
	 * @param start Start point.
	 * @param dest Destination.
	 * @return a list of GeoPoints for the route.
	 */

	private List<GeoPoint> plan(final GeoPoint start, final GeoPoint dest) {
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

	public void clearRoute() {
		mv.getOverlays().remove(route);
		route = null;
	}

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		final boolean enter = intent.getBooleanExtra(
				LocationManager.KEY_PROXIMITY_ENTERING, false);
		if (enter) {
			unsetAlert(intent);
		}
		
	}
	
	public void setAlerts(List<GeoPoint> points) {
		final LocationManager lm = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);

		PendingIntent pi;
		ArrayList<Integer> latLng;
		for(int i = 0; i < points.size(); i++) {
			latLng = new ArrayList<Integer>(2);
			latLng.add(points.get(i).getLatitudeE6());
			latLng.add(points.get(i).getLongitudeE6());
			intent.putIntegerArrayListExtra("latLng", latLng);
			pi = PendingIntent.getBroadcast(act, i, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
			lm.addProximityAlert(Degrees.asDegrees(latLng.get(0)),
				Degrees.asDegrees(latLng.get(1)), 5f, -1, pi);
		}
		final IntentFilter filter = new IntentFilter(INTENT_ID);
		act.registerReceiver(this, filter);
	}
	
	public void unsetAlert(Intent intent) {
		final LocationManager lm = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
		List<Integer> latLng = intent.getIntegerArrayListExtra("latLng");
		PendingIntent pi = pendingIntents.get(latLng);
		lm.removeProximityAlert(pi);
	}

}
