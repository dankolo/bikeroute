/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * Plans routes and displays them as overlays on the provided mapview.
 * 
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */
public class Planner {
	private Context act;
	private MapView mv;
	/** API feed. */
	private static final String API = "http://vega.soi.city.ac.uk/~abjy800/bike/cs.php?";
	/** Route overlay. **/
	private RouteOverlay route;
	
	public Planner (Context context, MapView mapview) {
		act = context;
		mv = mapview;
	}
	
	/**
	 * Plan a route between the points given and show it on the map. Displays an
	 * alert if the planning failed for some reason.
	 * 
	 * @param start
	 *            Starting point.
	 * @param dest
	 *            Destination point.
	 */

	public void showRoute(final GeoPoint start, final GeoPoint dest) {
		clearRoute();
		List<GeoPoint> points = new ArrayList<GeoPoint>();
		final ProgressDialog alert = ProgressDialog.show(act, "",
				act.getText(R.string.plan_msg), true, true,
				new DialogInterface.OnCancelListener() {
					public void onCancel(final DialogInterface dialog) {
						return;
					}
				});
		try {
			points = plan(start, dest);
			alert.dismiss();
			route = new RouteOverlay(points, Color.BLUE);
			mv.getOverlays().add(route);
			mv.invalidate();
		} catch (Exception e) {
			alert.dismiss();
			final AlertDialog.Builder builder = new AlertDialog.Builder(act);
			builder.setMessage(act.getText(R.string.planfail_msg)).setCancelable(
					false).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
						}
					});
			builder.create();
		}
	}

	/**
	 * Plan a route from here to a destination.
	 * 
	 * @param start
	 *            Start point.
	 * @param dest
	 *            Destination.
	 * @return a list of GeoPoints for the route.
	 */

	private List<GeoPoint> plan(final GeoPoint start, final GeoPoint dest) {
		final StringBuffer sBuf = new StringBuffer(API);
		sBuf.append("start_lat=");
		sBuf.append(start.getLatitudeE6() / Degrees.CNV);
		sBuf.append("&start_lng=");
		sBuf.append(start.getLongitudeE6() / Degrees.CNV);
		sBuf.append("&dest_lat=");
		sBuf.append(dest.getLatitudeE6() / Degrees.CNV);
		sBuf.append("&dest_lng=");
		sBuf.append(dest.getLongitudeE6() / Degrees.CNV);

		final CycleStreetsParser parser = new CycleStreetsParser(sBuf
				.toString());
		return parser.parse();
	}

	public void clearRoute() {
		mv.getOverlays().remove(route);
		route = null;
	}

}
