package com.nanosheep.bikeroute;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
	private MapView mv;
	private List<OverlayItem> markers;
	private Activity act;

	public LiveMarkers(final Drawable defaultMarker, final MapView mapview, final Activity activity) {
		super(defaultMarker);
		mv = mapview;
		act = activity;
	}

	/**
	 * Update markers around given point.
	 */

	public void refresh(final GeoPoint p) {
		act.showDialog(BikeNav.LOADSTANDS);
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
	 */
	
	private final Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			mOverlays.clear();
			mOverlays.addAll(markers);
			LiveMarkers.this.populate();
			setLastFocusedIndex(-1);
			mv.invalidate();
			act.dismissDialog(BikeNav.LOADSTANDS);
		}
	};

}
