package com.nanosheep.bikeroute.utility;

import com.nanosheep.bikeroute.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import org.andnav.osm.util.GeoPoint;

/**
 * Display an alert on return to the location the bike was parked at.
 * 
 * @author jono@nanosheep.net
 * 
 */

public final class BikeAlert extends BroadcastReceiver {
	/** Intent filter. **/
	private final IntentFilter filter;
	/** Pending Intent. **/
	private final PendingIntent pi;
	/** Owning activity. **/
	private final Activity act;

	public BikeAlert(final Activity activity) {
		super();
		act = activity;
		filter = new IntentFilter(act.getString(R.string.bike_alert_intent));
		pi = PendingIntent.getBroadcast(activity, 0, new Intent(act.getString(R.string.bike_alert_intent)),
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	/**
	 * Display a dialog offering to unpark the bike.
	 */

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final boolean enter = intent.getBooleanExtra(
				LocationManager.KEY_PROXIMITY_ENTERING, false);
		if (enter) {
			act.showDialog(R.id.unpark);
		}
	}

	/**
	 * Set a proximity alert at the given point for tracking bike position.
	 * 
	 * @param bikeLoc point to alert at.
	 */

	public void setBikeAlert(final GeoPoint bikeLoc) {
		final LocationManager lm = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
		lm.addProximityAlert(Convert.asDegrees(bikeLoc.getLatitudeE6()),
				Convert.asDegrees(bikeLoc.getLongitudeE6()), 5f, -1, pi);
		act.registerReceiver(this, filter);
	}

	/**
	 * Remove the alert.
	 */

	public void unsetAlert() {
		final LocationManager lm = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
		lm.removeProximityAlert(pi);
	}
}