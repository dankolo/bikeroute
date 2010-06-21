package com.nanosheep.bikeroute;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * A class to overlay a route onto a map given a list of geopoints.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class RouteOverlay extends Overlay {
	/** GeoPoints representing this route. **/
	private final List<GeoPoint> route;
	/** Colour to paint route. **/
	private int colour;
	/** Alpha setting for rute overlay. **/
	private static final int ALPHA = 120;
	/** Stroke width. **/
	private static final float STROKE = 4.5f;

	/**
	 * Public constructor.
	 * 
	 * @param list
	 *            List of geopoints representing the route.
	 * @param defaultColour
	 *            default colour to draw route in.
	 */

	public RouteOverlay(final List<GeoPoint> list, final int defaultColour) {
		super();
		route = list;
		colour = defaultColour;
	}

	@Override
	public final void draw(final Canvas c, final MapView mv,
			final boolean shadow) {
		final Point pS = new Point();
		final Point pE = new Point();
		final Projection prj = mv.getProjection();
		final Paint paint = new Paint();

		paint.setColor(colour);
		paint.setAlpha(ALPHA);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(STROKE);

		for (int i = 1; i < route.size(); i++) {
			prj.toPixels(route.get(i - 1), pS);
			prj.toPixels(route.get(i), pE);

			c.drawLine(pS.x, pS.y, pE.x, pE.y, paint);
		}

		super.draw(c, mv, shadow);
		mv.invalidate();
	}

	/**
	 * Set the colour to draw this route's overlay with.
	 * 
	 * @param c
	 *            Int representing colour.
	 */
	public final void setColour(final int c) {
		colour = c;
	}

	/**
	 * Remove points below the given index (inclusive).
	 * 
	 */

	public final void removeBelow(final int index) {
		route.subList(0, index + 1).clear();
	}

	/**
	 * Remove points below the one given (inclusive)
	 * 
	 * @param p
	 *            Point to remove below
	 */

	public final void removeBelow(final GeoPoint p) {
		removeBelow(route.indexOf(p));
	}

	/**
	 * Clear the route overlay.
	 */
	public final void clear() {
		route.clear();
	}

}
