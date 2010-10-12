package com.nanosheep.bikeroute.view.overlay;

import java.util.ArrayList;
import java.util.List;

import org.andnav.osm.DefaultResourceProxyImpl;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewItemizedOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewItemizedOverlay.OnItemTapListener;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlayItem;

import com.nanosheep.bikeroute.R;
import com.nanosheep.bikeroute.utility.Stands;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

/**
 * A class to display markers on a map and update them from a remote
 * feed.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class LiveMarkers implements OnItemTapListener<OpenStreetMapViewOverlayItem> {
	/** Update thread. **/
	private Thread update;
	/** Reference to map view to draw markers over. **/
	private final OpenStreetMapView mv;
	/** Markers list for use by thread. **/
	private List<OpenStreetMapViewOverlayItem> markers;
	private final Context context;
	/** Radius to return markers within. **/
	protected static final double RADIUS = 0.5;
	/** List of overlay items. **/
	private final List<OpenStreetMapViewOverlayItem> mOverlays;
	/** Itemized Overlay. **/
	private OpenStreetMapViewItemizedOverlay<OpenStreetMapViewOverlayItem> iOverlay;

	public LiveMarkers(final OpenStreetMapView mapview, final Context ctxt) {
		mv = mapview;
		context = ctxt;
		mOverlays = new ArrayList<OpenStreetMapViewOverlayItem>();
		iOverlay = new OpenStreetMapViewItemizedOverlay<OpenStreetMapViewOverlayItem>(ctxt, mOverlays,
				this, new DefaultResourceProxyImpl(ctxt));
	}

	/**
	 * Update markers around given point.
	 * @param p the Geopoint to gather markers around.
	 */

	public void refresh(final GeoPoint p) {
		update = new Thread() {
			private static final int MSG = 0;
			public void run() {
				markers = Stands.getMarkers(p, RADIUS, context);
				LiveMarkers.this.messageHandler.sendEmptyMessage(MSG);
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
			if (mv.getOverlays().contains(iOverlay)) {
				mv.getOverlays().remove(iOverlay);
			}
			mOverlays.clear();
			mOverlays.addAll(markers);
			iOverlay = new OpenStreetMapViewItemizedOverlay<OpenStreetMapViewOverlayItem>(
					context, mOverlays, null, new DefaultResourceProxyImpl(context));
			mv.getOverlays().add(iOverlay);
			mv.postInvalidate();
		}
	};

	/* (non-Javadoc)
	 * @see org.andnav.osm.views.overlay.OpenStreetMapViewItemizedOverlay.OnItemTapListener#onItemTap(int, java.lang.Object)
	 */
	@Override
	public boolean onItemTap(int aIndex, OpenStreetMapViewOverlayItem aItem) {
		return false;
	}

}
