/**
 * 
 */
package com.nanosheep.bikeroute.view.overlay;

import org.andnav.osm.views.overlay.OpenStreetMapViewPathOverlay;

import android.content.Context;

/**
 * @author jono@nanosheep.net
 * @version Sep 29, 2010
 */
public class RouteOverlay extends OpenStreetMapViewPathOverlay {

	/**
	 * @param color
	 * @param ctx
	 */
	public RouteOverlay(int color, Context ctx) {
		super(color, ctx);
		mPaint.setStrokeWidth(5.0f);
		mPaint.setAlpha(175);
	}

}
