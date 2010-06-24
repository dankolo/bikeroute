package com.nanosheep.bikeroute;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * A class to display markers on a map and update them after a scrolling
 * event.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class LiveMarkers extends Markers {

	public LiveMarkers(final Drawable defaultMarker) {
		super(defaultMarker);
	}

	/**
	 * Overrides to update markers if a gesture has completed.
	 * @return a boolean indicating whether this overlay dealt with the touch.
	 */

	@Override
	public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			final GeoPoint p = mapView.getMapCenter();
			reCenter(p);
		}
		return false;
	}

	/**
	 * Update markers around given point.
	 */

	public void reCenter(final GeoPoint p) {
		mOverlays = Stands.getMarkers(p, RADIUS);
		populate();
	}

}
