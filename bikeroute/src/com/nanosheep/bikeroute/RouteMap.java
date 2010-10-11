package com.nanosheep.bikeroute;

import java.util.ListIterator;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.MyLocationOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewPathOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.utility.BikeAlert;
import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.Parking;
import com.nanosheep.bikeroute.utility.Segment;
import com.nanosheep.bikeroute.utility.TurnByTurnGestureListener;
import com.nanosheep.bikeroute.view.overlay.LiveMarkers;
import com.nanosheep.bikeroute.view.overlay.RouteOverlay;

/**
 * A class for displaying a route map with overlays for directions and
 * nearby bicycle stands.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class RouteMap extends OpenStreetMapActivity {

	/** Stand markers overlay. */
	private LiveMarkers stands;
	/** Route overlay. **/
	protected OpenStreetMapViewPathOverlay routeOverlay;
	/** Travelled route overlay. **/
	protected OpenStreetMapViewPathOverlay travelledRouteOverlay;
	/** Location manager. **/
	protected LocationManager mLocationManager;
	
	/* Constants. */
	/** Initial zoom level. */
	private static final int ZOOM = 15;
	protected boolean isSearching = false;

	/** Parking manager. */
	private Parking prk;

	/** Bike alert manager. **/
	private BikeAlert bikeAlert;
	
	/** Dialog display. **/
	private Dialog dialog;
	
	/** Application reference. **/
	protected BikeRouteApp app;
	
	/** Onscreen directions shown. **/
	protected boolean directionsVisible;
	
	/** Gesture detection for the onscreen directions. **/
    private GestureDetector gestureDetector;
	private OnTouchListener gestureListener;
	
	/** Units for directions. **/
	protected String unit;
	

	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		
		/* Get location manager. */
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		
		/* Units preferences. */
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		unit = settings.getString("unitsPref", "km");
		
		//Set OSD invisible
		directionsVisible = false;
		
		// Initialize map, view & controller
		setContentView(R.layout.main);
		this.mOsmv = (OpenStreetMapView) findViewById(R.id.mapview);
        this.mOsmv.setResourceProxy(mResourceProxy);
        mOsmv.setRenderer(OpenStreetMapRendererFactory.CYCLEMAP);
        this.mLocationOverlay = new MyLocationOverlay(this.getBaseContext(), this.mOsmv);
        this.mLocationOverlay.enableMyLocation();
        this.mOsmv.setBuiltInZoomControls(true);
        this.mOsmv.setMultiTouchControls(true);
        this.mOsmv.getOverlays().add(this.mLocationOverlay);
        this.mOsmv.getOverlays().add(new OSDOverlay(this));
        

        mOsmv.getController().setZoom(mPrefs.getInt(BikeRouteConsts.PREFS_ZOOM_LEVEL, 1));
        mOsmv.scrollTo(mPrefs.getInt(BikeRouteConsts.PREFS_SCROLL_X, 0), mPrefs.getInt(BikeRouteConsts.PREFS_SCROLL_Y, 0));
		mOsmv.setBuiltInZoomControls(true);
		
		mOsmv.getController().setZoom(ZOOM);
		
		//Directions overlay
		final View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.setVisibility(View.INVISIBLE);
		
		//Get application reference
		app = (BikeRouteApp)getApplication();
				
		if (app.getRoute() != null) {
			routeOverlay = new RouteOverlay(Color.BLUE,this);
			travelledRouteOverlay = new RouteOverlay(Color.GREEN,this);
			for(GeoPoint pt : app.getRoute().getPoints()) {
				routeOverlay.addPoint(pt);
			}
			mOsmv.getOverlays().add(routeOverlay);
			mOsmv.getOverlays().add(travelledRouteOverlay);
			traverse(app.getSegment().startPoint());
			if (getIntent().getBooleanExtra("jump", false)) {
				showStep();
			}
			mOsmv.getController().setCenter(app.getSegment().startPoint());
		}


		// Initialize stands overlay
		stands = new LiveMarkers(mOsmv, this);

		// Initialize parking manager
		prk = new Parking(this);
		// Initialize bike alert manager
		bikeAlert = new BikeAlert(this);
				
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if ((data != null) && ((Boolean) data[0])) {
			mOsmv.getController().setZoom(16);
			showStep();
		}
	}
	
	/**
	 * Creates dialogs for loading, on errors, alerts.
	 * Available dialogs:
	 * Planning progress, planning error, unpark.
	 * @return the appropriate Dialog object
	 */
	
	public Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder;
		ProgressDialog pDialog;
		switch(id) {
		case BikeRouteConsts.UNPARK_DIALOG:
			builder = new AlertDialog.Builder(this);
			builder.setMessage("Reached bike. Unpark?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									prk.unPark();
									RouteMap.this.mOsmv.getOverlays().remove(routeOverlay);
									RouteMap.this.hideStep();
									RouteMap.this.app.setRoute(null);
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
		case BikeRouteConsts.AWAITING_FIX:
			pDialog = new ProgressDialog(this);
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.fix_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					RouteMap.this.removeDialog(BikeRouteConsts.AWAITING_FIX);
				}
			});
			dialog = pDialog;
			break;
		case BikeRouteConsts.ABOUT:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.about_message)).setCancelable(
					true).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	/**
	 * Create the options menu.
	 * @return true if menu created.
	 */

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}

	/**
	 * Prepare the menu. Set parking related menus to reflect parked or unparked
	 * state, set directions menu & turnbyturn visible only if a route has been planned.
	 * @return a boolean indicating super's state.
	 */

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem park = menu.findItem(R.id.park);
		final MenuItem unPark = menu.findItem(R.id.unpark);
		final MenuItem steps = menu.findItem(R.id.directions);
		final MenuItem turnByTurn = menu.findItem(R.id.turnbyturn);
		final MenuItem map = menu.findItem(R.id.map);
		final MenuItem elev = menu.findItem(R.id.elevation);
		if (prk.isParked()) {
			park.setVisible(false);
			unPark.setVisible(true);
		} else {
			park.setVisible(true);
			unPark.setVisible(false);
		}
		if (app.getRoute() != null) {
			steps.setVisible(true);
			elev.setVisible(true);
			if (directionsVisible) {
				turnByTurn.setVisible(false);
				map.setVisible(true);
			} else {
				turnByTurn.setVisible(true);
				map.setVisible(false);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Handle option selection.
	 * @return true if option selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.prefs:
			intent = new Intent(this, Preferences.class);
			startActivity(intent);
			break;
		case R.id.unpark:
			prk.unPark();
			break;
		case R.id.center:
			showDialog(BikeRouteConsts.AWAITING_FIX);
			RouteMap.this.mLocationOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (RouteMap.this.dialog.isShowing()) {
							RouteMap.this.dismissDialog(BikeRouteConsts.AWAITING_FIX);
							Location self = mLocationOverlay.getLastFix();
							
							if (self == null) {
								self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							}
							if (self == null) {
								self = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
							}
							if (self != null) {
								RouteMap.this.mOsmv.getController().animateTo(
									new GeoPoint(self.getLatitude(), self.getLongitude()));
							}
					}
				}
			});
			break;
		case R.id.showstands:
			Toast.makeText(this, "Getting stands from OpenStreetMap..",
					Toast.LENGTH_LONG).show();
			stands.refresh(mOsmv.getMapCenter());
			return true;
		case R.id.park:
			showDialog(BikeRouteConsts.AWAITING_FIX);
			RouteMap.this.mLocationOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (RouteMap.this.dialog.isShowing()) {
							RouteMap.this.dismissDialog(BikeRouteConsts.AWAITING_FIX);
							Location self = mLocationOverlay.getLastFix();
							
							if (self == null) {
								self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							}
							if (self == null) {
								self = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
							}
							if (self != null) {
								prk.park(
									new GeoPoint(self.getLatitude(), self.getLongitude()));
							}
					}
				}
			});
			break;
		case R.id.directions:
			intent = new Intent(this, DirectionsView.class);
			startActivity(intent);
			break;
		case R.id.turnbyturn:
			showStep();
			break;
		case R.id.map:
			hideStep();
			break;
		case R.id.navigate:
			intent = new Intent(this, Navigate.class);
			startActivity(intent);
			
			break;
		case R.id.elevation:
			XYMultipleSeriesDataset elevation = app.getRoute().getElevations();
			final XYMultipleSeriesRenderer renderer = app.getRoute().getChartRenderer();
			if (!"km".equals(unit)) {
				elevation = Convert.asImperial(elevation);
				renderer.setYTitle("ft");
			    renderer.setXTitle("m");
			}
			renderer.setYAxisMax(elevation.getSeriesAt(0).getMaxY() + 200);
			intent = ChartFactory.getLineChartIntent(this, elevation, renderer);
			startActivity(intent);
		case R.id.about:
			showDialog(BikeRouteConsts.ABOUT);
			break;
		default:
			return false;

		}
		return true;
	}
	
	/**
	 * Retain any route data if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[1];
		objs[0] = directionsVisible;
	    return objs;
	}
	
	/**
	 * Traverse the route up to the point given, overlay a different coloured route
	 * up to there.
	 * @param point
	 */
	
	protected void traverse(final GeoPoint point) {
		travelledRouteOverlay.clearPath();
		routeOverlay.clearPath();
		int index = app.getRoute().getPoints().indexOf(point);
		for (int i = 1; i < app.getRoute().getPoints().size(); i++) {
			if (i <= index) {
				travelledRouteOverlay.addPoint(app.getRoute().getPoints().get(i));
			}	else {
				routeOverlay.addPoint(app.getRoute().getPoints().get(i));
			}
		}
	}
	
	/**
	 * Go to the next step of the directions and show it.
	 */
	
	public void nextStep() {
		final int index = app.getRoute().getSegments().indexOf(app.getSegment());
		final ListIterator<Segment> it = app.getRoute().getSegments().listIterator(index + 1);
		if (it.hasNext()) {
			app.setSegment(it.next());
		}
		showStep();
		traverse(app.getSegment().startPoint());
	}
	
	/**
	 * Go to the previous step of the directions and show it.
	 */
	
	public void lastStep() {
		final int index = app.getRoute().getSegments().indexOf(app.getSegment());
		final ListIterator<Segment> it = app.getRoute().getSegments().listIterator(index);
		if (it.hasPrevious()) {
			app.setSegment(it.previous());
		}
		showStep();
		traverse(app.getSegment().startPoint());
 	}
	
	/**
	 * Hide the onscreen directions.
	 */
	
	public void hideStep() {
		final View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.setVisibility(View.INVISIBLE);
		mOsmv.setClickable(true);
		directionsVisible = false;
		this.mOsmv.setBuiltInZoomControls(true);
        this.mOsmv.setMultiTouchControls(true);
	}
	
	/**
	 * Show the currently selected step of directions onscreen, focus
	 * the map at the start of that section.
	 */
	
	public void showStep() {
		directionsVisible = true;
		this.mOsmv.setBuiltInZoomControls(false);
        this.mOsmv.setMultiTouchControls(false);
        mOsmv.getController().setZoom(16);
        mOsmv.getController().setCenter(app.getSegment().startPoint());
		
		final View overlay = (View) findViewById(R.id.directions_overlay);
		this.mOsmv.setClickable(false);
		
		//Setup buttons
		final Button back = (Button) overlay.findViewById(R.id.back_button);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				lastStep();
			}
			
		});
		
		final Button next = (Button) overlay.findViewById(R.id.next_button);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				nextStep();
			}
			
		});
		
		overlay.setOnTouchListener(gestureListener);
		
		final TextView turn = (TextView) overlay.findViewById(R.id.turn);
		final TextView distance = (TextView) overlay.findViewById(R.id.distance);
		final TextView num = (TextView) overlay.findViewById(R.id.step_no);
		turn.setText(app.getSegment().getInstruction());
		
		final String distanceString = "km".equals(unit) ? Convert.asMeterString(app.getSegment().getLength()) + " ("
				+ Convert.asKilometerString(app.getSegment().getDistance()) + ")" : 
					Convert.asFeetString(app.getSegment().getLength()) + " ("
					+ Convert.asMilesString(app.getSegment().getDistance()) + ")";
		
		distance.setText(distanceString);
		num.setText(app.getRoute().getSegments().indexOf(app.getSegment()) 
				+ 1 + "/" + app.getRoute().getSegments().size());
		overlay.setVisibility(View.VISIBLE);
	}

	/** Overlay to handle swipe events when showing directions.
	 * 
	 */
	
	private class OSDOverlay extends OpenStreetMapViewOverlay {

		/**
		 * @param ctx
		 */
		public OSDOverlay(final Context ctx) {
			super(ctx);
			//Detect swipes (left & right, taps.)
	        gestureDetector = new GestureDetector(RouteMap.this, new TurnByTurnGestureListener(RouteMap.this));
	        gestureListener = new View.OnTouchListener() {
	            public boolean onTouch(final View v, final MotionEvent event) {
	                if (gestureDetector.onTouchEvent(event)) {
	                    return true;
	                }
	                return false;
	            }
	        };
		}
		
		/**
		 * If the onscreen display is enabled, capture motion events to control it.
		 */
		
		@Override
		public boolean onTouchEvent(final MotionEvent event, final OpenStreetMapView mv) {
			if (RouteMap.this.directionsVisible) {
				RouteMap.this.gestureDetector.onTouchEvent(event);
				return true;
			} else {
		        return false;
			}
		}

		/* (non-Javadoc)
		 * @see org.andnav.osm.views.overlay.OpenStreetMapViewOverlay#onDraw(android.graphics.Canvas, org.andnav.osm.views.OpenStreetMapView)
		 */
		@Override
		protected void onDraw(final Canvas arg0, final OpenStreetMapView arg1) {
			
		}

		/* (non-Javadoc)
		 * @see org.andnav.osm.views.overlay.OpenStreetMapViewOverlay#onDrawFinished(android.graphics.Canvas, org.andnav.osm.views.OpenStreetMapView)
		 */
		@Override
		protected void onDrawFinished(final Canvas arg0, final OpenStreetMapView arg1) {			
		}
		
	}

}
