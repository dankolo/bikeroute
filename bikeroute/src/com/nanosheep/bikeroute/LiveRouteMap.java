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
import com.nanosheep.bikeroute.utility.Route;

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
	private boolean spoken;
	private Object lastSegment;
	private RoutePlannerTask search;
	
	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if (data != null) {
			isSearching = (Boolean) data[2];
			search = (RoutePlannerTask) data[3];
			mShownDialog = (Boolean) data[1];
		}
		
		spoken = true;
		lastSegment = app.getSegment();
	}
	
	/**
	 * Retain any state if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[4];
		objs[0] = directionsVisible;
		objs[1] = mShownDialog;
		objs[2] = isSearching;
		objs[3] = search;
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
		if (!spoken) {
			spoken = true;
			speak(app.getSegment());
		}
		super.showStep();
	}
	
	@Override
	public void hideStep() {
		super.hideStep();
		spoken = false;
	}
	
	/**
	 * Fire a replanning request (current location -> last point of route.)
	 * to the routing service, display a dialog while in progress.
	 */
	
	private void replan() {
		if (tts) {
			directionsTts.speak("Reeplanning.", TextToSpeech.QUEUE_FLUSH, null);
		}
		showDialog(BikeRouteConsts.PLAN);
		
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
							dismissDialog(BikeRouteConsts.PLAN);
							showDialog(BikeRouteConsts.PLAN_FAIL_DIALOG);
						}
				}
		});
	}
	

	public Dialog onCreateDialog(final int id) {
		Dialog dialog;
		switch(id) {
		case BikeRouteConsts.PLAN:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(final DialogInterface arg0) {
					removeDialog(BikeRouteConsts.PLAN);
					if (!LiveRouteMap.this.isSearching) {
						mShownDialog = false;
					}
				}
				});
			pDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(final DialogInterface arg0) {
					search.cancel(true);
				}
			
			});
			dialog = pDialog;
			break;
		case BikeRouteConsts.PLAN_FAIL_DIALOG:
			if (tts) {
				directionsTts.speak("Planning failed.", TextToSpeech.QUEUE_FLUSH, null);
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.planfail_msg)).setCancelable(
				true).setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
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
	 * Overridden to deal with rotations which require tracking
	 * displayed dialog to ensure it is not duplicated.
	 */
	
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == BikeRouteConsts.PLAN) {
			mShownDialog = true;
		}
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
		if (directionsVisible && !isSearching) {
		try {
			//Find the nearest point and unless it is far, assume we're there
			GeoPoint self = new GeoPoint(location.getLatitude(), location.getLongitude());
			GeoPoint near = app.getRoute().nearest(self);
			
			if (self.distanceTo(near) < 50) {
				app.setSegment(app.getRoute().getSegment(near));
				if (!lastSegment.equals(app.getSegment())) {
					spoken = false;
					lastSegment = app.getSegment();
				}
				showStep();
				traverse(near);
			} else {
				Iterator<GeoPoint> it = app.getRoute().getPoints().listIterator(
						app.getRoute().getPoints().indexOf(near));
				GeoPoint next = it.hasNext() ? it.next() : near;
				
				if (range(self, near, next) >= 50){
					isSearching = true;
					replan();
				}
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
		if (msg != null) {
			if (mShownDialog) {
				dismissDialog(BikeRouteConsts.PLAN);
			}
			isSearching = false;
			if (msg == BikeRouteConsts.RESULT_OK) {
				app.setRoute(route);
				app.setSegment(app.getRoute().getSegments().get(0));
				mOsmv.getController().setCenter(app.getSegment().startPoint());
				traverse(app.getSegment().startPoint());
				spoken = true;
				if (directionsVisible) {
					spoken = false;
					lastSegment = app.getSegment();
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
		
		return dist;
	}
}
