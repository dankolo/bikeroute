package com.nanosheep.bikeroute.overlay;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.nanosheep.bikeroute.utility.Stands;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

/**
 * A class to display markers on a map and update them from a remote
 * feed.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class LiveMarkers extends Markers {
	/** Update thread. **/
	private Thread update;
	/** Reference to map view to draw markers over. **/
	private final MapView mv;
	/** Markers list for use by thread. **/
	private List<OverlayItem> markers;

	public LiveMarkers(final Drawable defaultMarker, final MapView mapview) {
		super(defaultMarker);
		mv = mapview;
	}

	/**
	 * Update markers around given point.
	 * @param p the Geopoint to gather markers around.
	 */

	public void refresh(final GeoPoint p) {
		update = new Thread() {
			public void run() {
				int msg = 0;
				markers = Stands.getMarkers(p, RADIUS);
				LiveMarkers.this.messageHandler.sendEmptyMessage(msg);
			}
		};
		update.start();
	}
	
	/**
	 * Handler for stands thread.
	 * Remove the existing stands overlay if it exists and
	 * replace it with the new one from the thread.
	 */
	
	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (mv.getOverlays().contains(LiveMarkers.this)) {
				mv.getOverlays().remove(LiveMarkers.this);
			}
			mOverlays.clear();
			mOverlays.addAll(markers);
			LiveMarkers.this.populate();
			setLastFocusedIndex(-1);
			mv.getOverlays().add(LiveMarkers.this);
			mv.invalidate();
		}
	};

}
