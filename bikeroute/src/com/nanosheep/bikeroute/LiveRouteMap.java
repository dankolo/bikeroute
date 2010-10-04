/**
 * 
 */
package com.nanosheep.bikeroute;

import org.andnav.osm.util.GeoPoint;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MenuItem;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.service.RoutePlannerService;
import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.Route;
import com.nanosheep.bikeroute.utility.Segment;
import com.nanosheep.bikeroute.view.overlay.RouteOverlay;

/**
 * Extends RouteMap providing live/satnav features - turn guidance advancing with location,
 * route replanning.
 * 
 * @author jono@nanosheep.net
 * @version Oct 4, 2010
 */
public class LiveRouteMap extends RouteMap {
	/** Proximity alert receiver for directions. **/
	private ProximityReceiver proxAlerter;
	/** Intent for replanning searches. **/
	protected Intent searchIntent;
	/** Replanning result receiver. **/
	protected BroadcastReceiver routeReceiver;
	/** Planning dialog tracker. **/
	protected boolean mShownDialog;
	
	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if (data != null) {
			isSearching = (Boolean) data[2];
			if (isSearching) {
				registerReceiver(new ReplanReceiver(), new IntentFilter(RoutePlannerService.INTENT_ID));
			}
			mShownDialog = (Boolean) data[1];
		}
		proxAlerter = new ProximityReceiver();
	}
	
	/**
	 * Retain any state if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[3];
		objs[0] = directionsVisible;
		objs[1] = mShownDialog;
		objs[2] = isSearching;
	    return objs;
	}
	
	@Override
	public void showStep() {
		proxAlerter.setStepAlert(route.getSegments().get(segId + 1));
		super.showStep();
		mLocationOverlay.enableMyLocation();
	}
	
	/**
	 * Fire a replanning request (current location -> last point of route.)
	 * to the routing service, display a dialog while in progress.
	 */
	
	private void replan() {
		showDialog(BikeRouteConsts.PLAN);
		mLocationOverlay.followLocation(true);
		
		mLocationOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
						Location self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						if (self == null) {
							self = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						}
						if (self != null) {
							searchIntent = new Intent(LiveRouteMap.this, RoutePlannerService.class);
							searchIntent.putExtra(RoutePlannerService.PLAN_TYPE, RoutePlannerService.REPLAN_PLAN);
							searchIntent.putExtra(RoutePlannerService.START_LOCATION, mLocationOverlay.getLastFix());
							searchIntent.putExtra(RoutePlannerService.END_POINT,
									route.getPoints().get(route.getPoints().size() - 1));
							isSearching = true;
							startService(searchIntent);
							routeReceiver = new ReplanReceiver();
							registerReceiver(routeReceiver, new IntentFilter(RoutePlannerService.INTENT_ID));
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
			if (tts) {
				directionsTts.speak("Reeplanning.", TextToSpeech.QUEUE_FLUSH, null);
			}
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
					if (isSearching) {
						stopService(searchIntent);
						unregisterReceiver(routeReceiver);
						isSearching = false;
					}
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

	private class ReplanReceiver extends BroadcastReceiver {

		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context arg0, Intent intent) {
				if (intent.getIntExtra("msg", BikeRouteConsts.PLAN_FAIL_DIALOG) == BikeRouteConsts.RESULT_OK) {
					final BikeRouteApp app = (BikeRouteApp) getApplication();
					route = (Route) intent.getParcelableExtra("route");
					app.setRoute(route);
					mOsmv.getOverlays().remove(routeOverlay);
					
					routeOverlay = new RouteOverlay(Color.BLUE, LiveRouteMap.this);
					for(GeoPoint pt : route.getPoints()) {
						routeOverlay.addPoint(pt);
					}
					mOsmv.getOverlays().add(routeOverlay);
					segId = 0;
					currSegment = route.getSegments().get(segId);
					mc.setCenter(currSegment.startPoint());
					dismissDialog(BikeRouteConsts.PLAN);
					if (directionsVisible) {
						showStep();
					}
				} else {
					dismissDialog(BikeRouteConsts.PLAN);
					showDialog(intent.getIntExtra("msg", 0));
				}
		}
		
	}
	
	
	private class ProximityReceiver extends BroadcastReceiver {
		private static final String INTENT_ID = "com.nanosheep.bikeroute.STEP";
		/** Intent filter. **/
		private final IntentFilter filter;
		/** Pending Intent. **/
		private final PendingIntent pi;

		public ProximityReceiver () {
			super();
			filter = new IntentFilter(INTENT_ID);
			pi = PendingIntent.getBroadcast(LiveRouteMap.this, 0, new Intent(INTENT_ID),
					PendingIntent.FLAG_CANCEL_CURRENT);
		}
		
		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			unsetAlert();
			if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
				nextStep();
				setStepAlert(currSegment);
			} else {
				replan();
			}
			return;
		}
		
		/**
		 * Set a proximity alert at the given point for updating directions step.
		 * 
		 * @param segment point to alert at.
		 */

		public void setStepAlert(final Segment segment) {
			final LocationManager lm = (LocationManager) LiveRouteMap.this.getSystemService(Context.LOCATION_SERVICE);
			lm.addProximityAlert(Convert.asDegrees(segment.startPoint().getLatitudeE6()),
					Convert.asDegrees(segment.startPoint().getLongitudeE6()), 20f, -1, pi);
			LiveRouteMap.this.registerReceiver(this, filter);
		}

		/**
		 * Remove the alert.
		 */

		public void unsetAlert() {
			final LocationManager lm = (LocationManager) LiveRouteMap.this.getSystemService(Context.LOCATION_SERVICE);
			lm.removeProximityAlert(pi);
		}
		
	}
}
