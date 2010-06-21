package com.nanosheep.bikeroute;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

/**
 * A bicycle GPS system. Plans and displays routes to locations, optionally also
 * shows nearby bike stands and remembers where you parked your bike.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class BikeNav extends MapActivity {

	/** The map view. */
	private MapView mapView;
	/** The controller for the view. */
	private MapController mc;
	/** Stand markers overlay. */
	private LiveMarkers stands;
	/** User location overlay. **/
	private UserLocation locOverlay;
	/** Route overlay. **/
	private RouteOverlay route;
	/** Initial zoom level. */
	private static final int ZOOM = 15;
	/** API feed. */
	private static final String API = "http://vega.soi.city.ac.uk/~abjy800/bike/cs.php?";

	/** Parking manager. */
	private Parking prk;

	/** Bike alert manager. **/
	private BikeAlert bikeAlert;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize map, view & controller
		setContentView(R.layout.main);
		mapView = (MapView) findViewById(R.id.bikeview);
		mapView.displayZoomControls(true);
		mc = mapView.getController();
		mc.setZoom(ZOOM);

		// Initialize stands overlay
		Drawable drawable = this.getResources().getDrawable(
				R.drawable.androidmarker);
		stands = new LiveMarkers(drawable);

		// Initialise parking manager
		prk = new Parking(this);
		// Initialize bike alert manager
		bikeAlert = new BikeAlert(this);
		// Initialize location service
		locOverlay = new UserLocation(this, mapView, mc);
		locOverlay.enableMyLocation();
		mapView.getOverlays().add(locOverlay);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Create the options menu.
	 */

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Prepare the menu. Set parking related menus to reflect parked or unparked
	 * state.
	 */

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem park = menu.findItem(R.id.park);
		final MenuItem back = menu.findItem(R.id.back);
		final MenuItem unPark = menu.findItem(R.id.unpark);
		if (prk.isParked()) {
			park.setVisible(false);
			unPark.setVisible(true);
			back.setVisible(true);
		} else {
			back.setVisible(false);
			park.setVisible(true);
			unPark.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Handle option selection.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.center:
			mc.animateTo(locOverlay.getMyLocation());
			return true;
		case R.id.back:
			bikeAlert.setBikeAlert(prk.getLocation());
			showRoute(locOverlay.getMyLocation(), prk.getLocation());
			return true;
		case R.id.navigate:
			Intent intent = new Intent(this, FindPlace.class);
			startActivityForResult(intent, 0);
			return true;
		case R.id.unpark:
			prk.unPark();
			clearRoute();
			bikeAlert.unsetAlert();
			return true;
		case R.id.showstands:
			if (item.isChecked()) {
				item.setChecked(false);
				mapView.getOverlays().remove(stands);
			} else {
				item.setChecked(true);
				stands.reCenter(mapView.getMapCenter());
				mapView.getOverlays().add(stands);
			}
			mapView.invalidate();
			return true;
		case R.id.park:
			prk.park(locOverlay.getMyLocation());
			return true;
		default:
			return false;

		}
	}

	@Override
	public void onResume() {
		locOverlay.enableMyLocation();
		super.onResume();
	}

	@Override
	public void onPause() {
		locOverlay.disableMyLocation();
		super.onPause();
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

	private void showRoute(final GeoPoint start, final GeoPoint dest) {
		clearRoute();
		List<GeoPoint> points = new ArrayList<GeoPoint>();
		final ProgressDialog alert = ProgressDialog.show(this, "",
				getText(R.string.plan_msg), true, true,
				new DialogInterface.OnCancelListener() {
					public void onCancel(final DialogInterface dialog) {
						return;
					}
				});
		try {
			points = plan(start, dest);
			alert.dismiss();
			route = new RouteOverlay(points, Color.BLUE);
			mapView.getOverlays().add(route);
			mapView.invalidate();
		} catch (Exception e) {
			alert.dismiss();
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.planfail_msg)).setCancelable(
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

	private void clearRoute() {
		mapView.getOverlays().remove(route);
		route = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			final List<Integer> latLng = data
					.getIntegerArrayListExtra("latLng");
			final GeoPoint dest = new GeoPoint(latLng.get(0), latLng.get(1));

			showRoute(locOverlay.getMyLocation(), dest);
		}
	}

	/**
	 * Display an alert on return to the location the bike was parked at,
	 * 
	 * @author jono@nanosheep.net
	 * 
	 */

	private final class BikeAlert extends BroadcastReceiver {
		/** Name for bike alert intents. **/
		private static final String INTENT_ID = "com.nanosheep.bikeroute.BIKE_ALERT";
		/** Intent. **/
		private final Intent intent;
		/** Pending Intent. **/
		private final PendingIntent pi;

		public BikeAlert(final Activity activity) {
			super();
			intent = new Intent(INTENT_ID);
			pi = PendingIntent.getBroadcast(activity, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);
		}

		/**
		 * Display a dialog offering to unpark the bike.
		 */

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean enter = intent.getBooleanExtra(
					LocationManager.KEY_PROXIMITY_ENTERING, false);
			if (enter) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						BikeNav.this);
				builder.setMessage("Reached bike. Unpark?")
						.setCancelable(false).setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										prk.unPark();
										route.clear();
										unsetAlert();
										dialog.dismiss();
									}
								}).setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										dialog.cancel();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}

		/**
		 * Set a proximity alert at the given point for tracking bike position.
		 * 
		 * @param bikeLoc
		 *            point to alert at.
		 */

		public void setBikeAlert(final GeoPoint bikeLoc) {
			final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lm.addProximityAlert(bikeLoc.getLatitudeE6() / Degrees.CNV, bikeLoc
					.getLongitudeE6()
					/ Degrees.CNV, 5f, -1, pi);
			final IntentFilter filter = new IntentFilter(INTENT_ID);
			registerReceiver(this, filter);
		}

		/**
		 * Remove the alert.
		 */

		public void unsetAlert() {
			final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lm.removeProximityAlert(pi);
		}
	}

}
