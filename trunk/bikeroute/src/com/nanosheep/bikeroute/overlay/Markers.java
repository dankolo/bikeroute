package com.nanosheep.bikeroute.overlay;

/**
 * Overlays a set of markers onto the current map.
 */

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class Markers extends ItemizedOverlay<OverlayItem> {
	/** List of overlay items. **/
	protected List<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	/** Radius to return markers within. **/
	protected static final double RADIUS = 0.5;

	public Markers(final Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	@Override
	protected OverlayItem createItem(final int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	public void addOverlay(final OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}
}
