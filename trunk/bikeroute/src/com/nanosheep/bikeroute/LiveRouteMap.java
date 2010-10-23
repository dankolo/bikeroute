/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.Iterator;

import org.andnav.osm.util.GeoPoint;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.service.RouteListener;
import com.nanosheep.bikeroute.service.RoutePlannerTask;
import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;
import com.nanosheep.bikeroute.R;


import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Extends RouteMap providing live/satnav features - turn guidance advancing with location,
 * route replanning.
 * 
 * @author jono@nanosheep.net
 * @version Oct 4, 2010
 */
public class LiveRouteMap extends SpeechRouteMap implements LocationListener, RouteListener {
	/** Intent for replanning searches. **/
	protected Intent searchIntent;
	/** Planning dialog tracker. **/
	protected boolean mShownDialog;
	
	/** Search task for replanning. **/
	private RoutePlannerTask search;
	
	/** Live navigation toggle. **/
	private boolean liveNavigation;
	
	/** Last segment visited. **/
	
	private Segment lastSegment;
	private boolean spoken;
	
	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if (data != null) {
			isSearching = (Boolean) data[2];
			search = (RoutePlannerTask) data[3];
			if(search != null) {
				search.setListener(this);
			}
			spoken = (Boolean) data[4];
		}
		
		liveNavigation = mSettings.getBoolean("gps", false);
		
		if (liveNavigation) {
			if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			}	else {
				showDialog(R.id.gps);
			}
		}
	}
	
	/**
	 * Retain any state if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[5];
		objs[0] = directionsVisible;
		objs[1] = mShownDialog;
		objs[2] = isSearching;
		objs[3] = search;
		objs[4] = spoken;
	    return objs;
	}
	
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem replan = menu.findItem(R.id.replan);
		replan.setVisible(true);
		if (app.getRoute() == null) {
			replan.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public void showStep() {
		super.showStep();
		if(mSettings.getBoolean("gps", false)) {
			mLocationOverlay.followLocation(true);
		} else {
			mLocationOverlay.followLocation(false);
		}
	}
	
	@Override
	public void hideStep() {
		super.hideStep();
		mLocationOverlay.followLocation(false);
	}
	
	/**
	 * Fire a replanning request (current location -> last point of route.)
	 * to the routing service, display a dialog while in progress.
	 */
	
	private void replan() {
		if (tts) {
			directionsTts.speak("Reeplanning.", TextToSpeech.QUEUE_FLUSH, null);
		}
		showDialog(R.id.plan);
		
		mLocationOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
						Location self = mLocationOverlay.getLastFix();
						
						if (self == null) {
							self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						}
						if (self == null) {
							self = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						}
						if (self != null) {
							searchIntent = new Intent();
							searchIntent.putExtra(RoutePlannerTask.PLAN_TYPE, RoutePlannerTask.REPLAN_PLAN);
							searchIntent.putExtra(RoutePlannerTask.START_LOCATION, self);
							searchIntent.putExtra(RoutePlannerTask.END_POINT,
									app.getRoute().getPoints().get(app.getRoute().getPoints().size() - 1));
							search = new RoutePlannerTask(LiveRouteMap.this, searchIntent);
							search.execute();
						} else {
							dismissDialog(R.id.plan);
							showDialog(R.id.plan_fail);
						}
				}
		});
	}
	

	@Override
	public Dialog onCreateDialog(final int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch(id) {
		case R.id.gps:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.gps_msg);
			builder.setCancelable(false);
			builder.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int id) {
						showGpsOptions();
					}
				});
			builder.setTitle(R.string.gps_msg_title);
			dialog = builder.create();
			break;
		case R.id.plan:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(final DialogInterface arg0) {
					removeDialog(R.id.plan);
					if (!LiveRouteMap.this.isSearching) {
						mShownDialog = false;
					}
				}
				});
			pDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(final DialogInterface arg0) {
					if (search != null) {
						search.cancel(true);
					}
				}
			
			});
			dialog = pDialog;
			break;
		case R.id.plan_fail:
			if (tts) {
				directionsTts.speak(getString(R.string.plan_failed_speech), TextToSpeech.QUEUE_FLUSH, null);
			}
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.planfail_msg);
			builder.setCancelable(
				true).setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int id) {
					}
				});
			dialog = builder.create();
			break;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	/**
	 * Handle option selection.
	 * @return true if option selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.replan:
			replan();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocationManager.removeUpdates(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		liveNavigation = mSettings.getBoolean("gps", false);
		if (tts && directionsVisible && !isSearching) {
			speak(app.getSegment());
		}
	}
	
	/**
	 * Listen for location changes, check for the nearest point to the new location
	 * find the segment for it and advance to that step of the directions. If that point
	 * is more than 50m away, fire a replan request,
	 */
	
	/* (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location) {
		//Ignore if directions not shown or replanning
		if (directionsVisible && !isSearching && liveNavigation) {
		try {
			//Find the nearest point and unless it is far, assume we're there
			GeoPoint self = new GeoPoint(location);
			GeoPoint near = app.getRoute().nearest(self);
			
			Iterator<GeoPoint> it = app.getRoute().getPoints().listIterator(
					app.getRoute().getPoints().indexOf(near) + 1);
			GeoPoint next = it.hasNext() ? it.next() : near;
			double range = range(self, near, next);
			
			if ((range > 50) && (self.distanceTo(near) > 50)) {
				isSearching = true;
				replan();
			} else {
				app.setSegment(app.getRoute().getSegment(near));
				if (!app.getSegment().equals(lastSegment)) {
					lastSegment = app.getSegment();
					spoken = false;
				}
				//Speak directions if the next point is a new segment
				//and have not spoken already
				if (!spoken && !app.getSegment().equals(app.getRoute().getSegment(next)) && tts) {
						speak(app.getRoute().getSegment(next));
						spoken = true;
				}
				showStep();
				traverse(near);
			}
			
		} catch (KeySizeException e) {
			Log.e("KD", e.toString());
		}
		}
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.nanosheep.bikeroute.service.RouteListener#searchComplete(java.lang.Integer, com.nanosheep.bikeroute.utility.Route)
	 */
	@Override
	public void searchComplete(Integer msg, Route route) {
		dismissDialog(R.id.plan);
		
		isSearching = false;
		if (msg != null) {
			
			if (msg == R.id.result_ok) {
				app.setRoute(route);
				app.setSegment(app.getRoute().getSegments().get(0));
				mOsmv.getController().setCenter(app.getSegment().startPoint());
				traverse(app.getSegment().startPoint());
				if (directionsVisible) {
					showStep();
				}
			} else {
				showDialog(msg);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.nanosheep.bikeroute.service.RouteListener#searchCancelled()
	 */
	@Override
	public void searchCancelled() {
		isSearching = false;
		search = null;
	}

	/* (non-Javadoc)
	 * @see com.nanosheep.bikeroute.service.RouteListener#getContext()
	 */
	@Override
	public Context getContext() {
		return this;
	}
	
	/**
	 * Get the cross track error of p0 from the path p1 -> p2
	 * @param p0 point to get distance to.
	 * @param p1 start point of line.
	 * @param p2 end point of line.
	 * @return the distance from p0 to the path in meters as a double.
	 */
	
	private double range(final GeoPoint p0, final GeoPoint p1, final GeoPoint p2) {
		double dist = Math.asin(Math.sin(p1.distanceTo(p0)/BikeRouteConsts.EARTH_RADIUS) * 
				Math.sin(p1.bearingTo(p0) - p1.bearingTo(p2))) * 
				BikeRouteConsts.EARTH_RADIUS;
		
		return Math.abs(dist);
	}
	
	/** Show GPS options if GPS provider is disabled.
	 * 
	 */
	
	private void showGpsOptions() { 
        startActivity(new Intent(  
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)); 
	}
}
