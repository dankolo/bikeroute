package com.nanosheep.bikeroute;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;

/**
 * A class to display markers on a map and update them after a scrolling
 * event.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class LiveMarkers extends Markers {
	private Thread update;

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
		update = new Thread() {
			public void run() {
				int msg = 0;
				mOverlays = Stands.getMarkers(p, RADIUS);
				LiveMarkers.this.messageHandler.sendEmptyMessage(msg);
			}
		};
		update.start();
	}
	
	/**
	 * Handler for stands thread.
	 */
	
	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			LiveMarkers.this.populate();
			
		}
	};

}
