package com.nanosheep.bikeroute;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

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
	/** Route planner. **/
	private RouteManager planner;
	
	/* Constants. */
	/** Dialog ids. **/
	/** Planning. **/
	public static final int PLANNING_DIALOG = 0;
	/** Planning failed. **/
	public static final int PLAN_FAIL_DIALOG = 1;
	/** Unpark. **/
	public static final int UNPARK_DIALOG = 2;
	/** Navigate dialog. **/
	public static final int NAVIGATE = 3;
	/** Awaiting GPS fix dialog. **/
	public static final int AWAITING_FIX = 4;
	/** Route parcel id. **/
	public static final String ROUTE = "com.nanosheep.bikeroute.Route";
	/** Initial zoom level. */
	private static final int ZOOM = 15;
	/* Request ids */
	/** Navigation request. **/
	private static final int FIND_REQ = 0;
	/** Jump request. **/
	private static final int JUMP_REQ = 2;

	/** Parking manager. */
	private Parking prk;

	/** Bike alert manager. **/
	private BikeAlert bikeAlert;
	
	/** Dialog display. **/
	private Dialog dialog;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Initialize map, view & controller
		setContentView(R.layout.main);
		mapView = (MapView) findViewById(R.id.bikeview);
		mapView.displayZoomControls(true);
		mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_UNDER);
		
		mc = mapView.getController();
		mc.setZoom(ZOOM);

		// Initialize stands overlay
		Drawable drawable = this.getResources().getDrawable(
				R.drawable.parking);
		stands = new LiveMarkers(drawable, mapView, this);

		// Initialise parking manager
		prk = new Parking(this);
		// Initialize bike alert manager
		bikeAlert = new BikeAlert(this);
		// Initialize location service
		locOverlay = new UserLocation(this, mapView, mc);
		locOverlay.enableMyLocation();
		mapView.getOverlays().add(locOverlay);
		
		planner = new RouteManager(this, mapView);
		
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if (data != null) {
			if (data[0] != null) {
				planner.setRoute((Route) data[0]);
			}
			if (data[1] != null) {
				planner.setStart((GeoPoint) data[1]);
			}
			if (data[2] != null) {
				planner.setDest((GeoPoint) data[2]);
				planner.showRoute();
			}
			if (data[3] != null) {
				dialog = (Dialog) data[3];
			}
		}
	}
	
	/**
	 * Creates dialogs for loading, on errors, alerts.
	 * Available dialogs:
	 * Planning progress, planning error, unpark.
	 * @return the approriate Dialog object
	 */
	
	public Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder;
		ProgressDialog pDialog;
		switch(id) {
		case PLANNING_DIALOG:
			pDialog = new ProgressDialog(this);
			pDialog.setCancelable(false);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					BikeNav.this.removeDialog(PLANNING_DIALOG);
				}
			});
			dialog = pDialog;
			break;
		case PLAN_FAIL_DIALOG:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.planfail_msg)).setCancelable(
					false).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
						}
					});
			dialog = builder.create();
			break;
		case UNPARK_DIALOG:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Reached bike. Unpark?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									prk.unPark();
									planner.clearRoute();
									bikeAlert.unsetAlert();
									dialog.dismiss();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case AWAITING_FIX:
			pDialog = new ProgressDialog(this);
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.fix_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					BikeNav.this.removeDialog(AWAITING_FIX);
				}
			});
			dialog = pDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Create the options menu.
	 * @return true if menu created.
	 */

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Prepare the menu. Set parking related menus to reflect parked or unparked
	 * state, set directions menu visible only if a route has been planned.
	 * @return a boolean indicating super's state.
	 */

	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem park = menu.findItem(R.id.park);
		final MenuItem back = menu.findItem(R.id.back);
		final MenuItem unPark = menu.findItem(R.id.unpark);
		final MenuItem steps = menu.findItem(R.id.directions);
		if (prk.isParked()) {
			park.setVisible(false);
			unPark.setVisible(true);
			back.setVisible(true);
		} else {
			back.setVisible(false);
			park.setVisible(true);
			unPark.setVisible(false);
		}
		if (planner.isPlanned()) {
			steps.setVisible(true);
		} else {
			steps.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Handle option selection.
	 * @return true if option selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.center:
			showDialog(AWAITING_FIX);
			BikeNav.this.locOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (BikeNav.this.dialog.isShowing()) {
						BikeNav.this.dismissDialog(AWAITING_FIX);
						BikeNav.this.mc.animateTo(BikeNav.this.locOverlay.getMyLocation());
					}
				}
			});
			return true;
		case R.id.back:
			bikeAlert.setBikeAlert(prk.getLocation());
			showDialog(AWAITING_FIX);
			locOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (BikeNav.this.dialog.isShowing()) {
						BikeNav.this.dismissDialog(AWAITING_FIX);
						BikeNav.this.planner.setStart(BikeNav.this.locOverlay.getMyLocation());
						BikeNav.this.planner.setDest(BikeNav.this.prk.getLocation());
						BikeNav.this.planner.showRoute();
					}
				}
			});
			return true;
		case R.id.navigate:
			Intent intentNav = new Intent(BikeNav.this, FindPlace.class);
			if (BikeNav.this.locOverlay.getMyLocation() != null) {
				intentNav.putExtra("lat",
						BikeNav.this.locOverlay.getMyLocation().getLatitudeE6());
				intentNav.putExtra("lng",
						BikeNav.this.locOverlay.getMyLocation().getLongitudeE6());
			}
			startActivityForResult(intentNav, FIND_REQ);
			return true;
		case R.id.unpark:
			prk.unPark();
			planner.clearRoute();
			bikeAlert.unsetAlert();
			return true;
		case R.id.showstands:
			if (item.isChecked()) {
				item.setChecked(false);
				mapView.getOverlays().remove(stands);
			} 
			Toast.makeText(this, "Getting stands from OpenStreetMap..",
					Toast.LENGTH_LONG).show();
			item.setChecked(true);
			stands.refresh(mapView.getMapCenter());
			mapView.getOverlays().add(stands);
			mapView.invalidate();
			return true;
		case R.id.park:
			prk.park(locOverlay.getMyLocation());
			return true;
		case R.id.directions:
			Intent intentDir = new Intent(this, DirectionsView.class);
			intentDir.putExtra(ROUTE, planner.getRoute());
			startActivityForResult(intentDir, JUMP_REQ);
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
	 * Handles responses from started activities. In this case, deal with navigation
	 * requests and jumping to a point in the list of directions for a route.
	 */
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		//Glue for place finding screens.
		if (resultCode == RESULT_OK && requestCode == FIND_REQ) {
			final List<Integer> latLng = data
				.getIntegerArrayListExtra("latLng");
			final GeoPoint startPoint = new GeoPoint(latLng.get(0), latLng.get(1));
			final GeoPoint endPoint = new GeoPoint(latLng.get(2), latLng.get(3));
			planner.setStart(startPoint);
			planner.setDest(endPoint);
			planner.setCountry(data.getStringExtra("country"));
			planner.showRoute();
		//Jump to point in directions on the map.	
		} else if (requestCode == JUMP_REQ && resultCode != RESULT_CANCELED) {
			mc.setCenter(planner.getRoute().getSegments().get(resultCode).startPoint());
			mc.setZoom(16);
		}
	}
	
	/**
	 * Retain any route data if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[3];
		objs[0] = planner.getRoute();
		objs[1] = planner.getStart();
		objs[2] = planner.getDest();
		objs[3] = dialog;
	    return objs;
	}

}
