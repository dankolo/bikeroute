package com.nanosheep.bikeroute;

import android.content.Context;
import android.location.Location;

import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

/**
 * MyLocationOverlay that takes a mapcontroller and uses it to follow the user's
 * location.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class UserLocation extends MyLocationOverlay {
	/** Map controller to use when animating. **/
	private MapController mc;

	public UserLocation(final Context context, final MapView mapView) {
		super(context, mapView);
	}

	public UserLocation(final Context context, final MapView mapView,
			final MapController mapControl) {
		super(context, mapView);
		mc = mapControl;
	}

	/**
	 * Center the map on the current user location.
	 */
	
	public void center() {
		mc.animateTo(getMyLocation());
	}

}
