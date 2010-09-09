package com.nanosheep.bikeroute;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.GestureDetector;
import android.view.KeyEvent;
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

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.nanosheep.bikeroute.overlay.LiveMarkers;
import com.nanosheep.bikeroute.overlay.RouteOverlay;
import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.Parking;

/**
 * A class for displaying a route map with overlays for directions and
 * nearby bicycle stands.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class RouteMap extends MapActivity implements OnInitListener {

	/** The map view. */
	private MapView mapView;
	/** The controller for the view. */
	private MapController mc;
	/** Stand markers overlay. */
	private LiveMarkers stands;
	/** User location overlay. **/
	private UserLocation locOverlay;
	/** Route overlay. **/
	private RouteOverlay routeOverlay;
	
	/** Route. **/
	private Route route;
	
	/* Constants. */
	/** Dialog ids. **/
	/** Unpark. **/
	public static final int UNPARK_DIALOG = 2;
	/** Awaiting GPS fix dialog. **/
	public static final int AWAITING_FIX = 4;
	/** Initial zoom level. */
	private static final int ZOOM = 15;

	/** Parking manager. */
	private Parking prk;

	/** Bike alert manager. **/
	private BikeAlert bikeAlert;
	
	/** Dialog display. **/
	private Dialog dialog;
	
	/** Current segment. **/
	private Segment currSegment;
	/** Segment pointer. **/
	private int segId;
	
	/** Onscreen directions shown. **/
	private boolean directionsVisible;
	
	/** Gesture detection for the onscreen directions. **/
    private GestureDetector gestureDetector;
	private OnTouchListener gestureListener;
	
	/** Units for directions. **/
	private String unit;
	/** TTS enabled. **/
	private boolean tts;
	/** TTS. **/
	private TextToSpeech directionsTts;
	

	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		unit = settings.getString("unitsPref", "km");
		tts = settings.getBoolean("tts", false);
		
		//Detect swipes (left & right, taps.)
        gestureDetector = new GestureDetector(this, new TurnByTurnGestureListener(this));
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        
        //Initialize tts if in use.
        if (tts) {
        	directionsTts = new TextToSpeech(this, this);
        }
		
		//Set OSD invisible
		directionsVisible = false;
		
		// Initialize map, view & controller
		setContentView(R.layout.main);
		mapView = (MapView) findViewById(R.id.bikeview);
		mapView.displayZoomControls(true);
		mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_UNDER);
		
		mc = mapView.getController();
		mc.setZoom(ZOOM);
		
		//Directions overlay
		View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.setVisibility(View.INVISIBLE);
		
		
		//Route and segment
		route = ((BikeRouteApp)getApplication()).getRoute();
		segId = ((BikeRouteApp)getApplication()).getSegId();
		
		if (route != null) {
			routeOverlay = new RouteOverlay(route, Color.BLUE);
			mapView.getOverlays().add(routeOverlay);
			currSegment = route.getSegments().get(segId);
			if (getIntent().getBooleanExtra("jump", false)) {
				showStep();
			}
			mc.setCenter(currSegment.startPoint());
		}


		// Initialize stands overlay
		final Drawable drawable = this.getResources().getDrawable(
				R.drawable.parking);
		stands = new LiveMarkers(drawable, mapView);

		// Initialise parking manager
		prk = new Parking(this);
		// Initialize bike alert manager
		bikeAlert = new BikeAlert(this);
		// Initialize location service
		locOverlay = new UserLocation(this, mapView, mc);
		locOverlay.enableMyLocation();
		mapView.getOverlays().add(locOverlay);
				
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if (data != null) {
			if ((Boolean) data[0]) {
				mc.setZoom(16);
				showStep();
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
									RouteMap.this.mapView.getOverlays().remove(routeOverlay);
									RouteMap.this.hideStep();
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
					RouteMap.this.removeDialog(AWAITING_FIX);
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
		return (route != null);
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
	public final boolean onPrepareOptionsMenu(final Menu menu) {
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
		if (route != null) {
			steps.setVisible(true);
			elev.setVisible(true);
			if (directionsVisible) {
				turnByTurn.setVisible(false);
				map.setVisible(true);
			} else {
				turnByTurn.setVisible(true);
				map.setVisible(false);
			}
		} else {
			map.setVisible(false);
			steps.setVisible(false);
			turnByTurn.setVisible(false);
			elev.setVisible(false);
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
			showDialog(AWAITING_FIX);
			RouteMap.this.locOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (RouteMap.this.dialog.isShowing()) {
						RouteMap.this.dismissDialog(AWAITING_FIX);
						RouteMap.this.mc.animateTo(RouteMap.this.locOverlay.getMyLocation());
					}
				}
			});
			break;
		case R.id.showstands:
			Toast.makeText(this, "Getting stands from OpenStreetMap..",
					Toast.LENGTH_LONG).show();
			stands.refresh(mapView.getMapCenter());
			return true;
		case R.id.park:
			showDialog(AWAITING_FIX);
			RouteMap.this.locOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (RouteMap.this.dialog.isShowing()) {
						RouteMap.this.dismissDialog(AWAITING_FIX);
						prk.park(locOverlay.getMyLocation());
					}
				}
			});
			break;
		case R.id.directions:
			intent = new Intent(this, DirectionsView.class);
			intent.putExtra("segment", segId);
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
			if (route != null) {
				intent.putExtra("segment", segId);
			} 
			startActivity(intent);
			
			break;
		case R.id.elevation:
			XYMultipleSeriesDataset elevation = route.getElevations();
			XYMultipleSeriesRenderer renderer = route.getChartRenderer();
			if (!"km".equals(unit)) {
				elevation = Convert.asImperial(elevation);
				renderer.setYTitle("ft");
			    renderer.setXTitle("m");
			}
			renderer.setYAxisMax(elevation.getSeriesAt(0).getMaxY() + 200);
			intent = ChartFactory.getLineChartIntent(this, elevation, renderer);
			startActivity(intent);
		default:
			return false;

		}
		return true;
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
	 * Retain any route data if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[1];
		objs[0] = directionsVisible;
	    return objs;
	}
	
	/**
	 * Go to the next step of the directions and show it.
	 */
	
	public void nextStep() {
		if (segId + 1 < route.getSegments().size()) {
			segId++;
		}
		showStep();
	}
	
	/**
	 * Go to the previous step of the directions and show it.
	 */
	
	public void lastStep() {
		if (segId > 0) {
			segId--;
		}
		showStep();
 	}
	
	/**
	 * Hide the onscreen directions.
	 */
	
	public void hideStep() {
		View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.setVisibility(View.INVISIBLE);
		mapView.setClickable(true);
		directionsVisible = false;
		((BikeRouteApp) getApplication()).setSegId(0);
		if (tts) {
			directionsTts.stop();
		}
	}
	
	/**
	 * Show the currently selected step of directions onscreen, focus
	 * the map at the start of that section.
	 */
	
	public void showStep() {
		((BikeRouteApp) getApplication()).setSegId(segId);
		directionsVisible = true;
		currSegment = route.getSegments().get(segId);
		mc.setZoom(16);
		mc.setCenter(currSegment.startPoint());
		View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.requestFocus();
		mapView.setClickable(false);
		
		//Setup buttons
		Button back = (Button) overlay.findViewById(R.id.back_button);
		back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				lastStep();
			}
			
		});
		
		Button next = (Button) overlay.findViewById(R.id.next_button);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				nextStep();
			}
			
		});
		
		if (segId > 0) {
			back.setVisibility(View.VISIBLE);
		} else {
			back.setVisibility(View.INVISIBLE);
		}
		
		if (segId + 1 < route.getSegments().size()) {
			next.setVisibility(View.VISIBLE);
		} else {
			next.setVisibility(View.INVISIBLE);
		}
		
		overlay.setOnTouchListener(gestureListener);
		
		TextView turn = (TextView) overlay.findViewById(R.id.turn);
		TextView distance = (TextView) overlay.findViewById(R.id.distance);
		TextView num = (TextView) overlay.findViewById(R.id.step_no);
		turn.setText(currSegment.getInstruction());
		
		String distanceString = "km".equals(unit) ? Convert.asMeterString(currSegment.getLength()) + " ("
				+ Convert.asKilometerString(currSegment.getDistance()) + ")" : 
					Convert.asFeetString(currSegment.getLength()) + " ("
					+ Convert.asMilesString(currSegment.getDistance()) + ")";
		
		distance.setText(distanceString);
		num.setText(segId + 1 + "/" + route.getSegments().size());
		overlay.setVisibility(View.VISIBLE);
		
		if (tts) {
			directionsTts.speak(currSegment.getInstruction(), TextToSpeech.QUEUE_FLUSH, null);
		}
	}
	
	/**
	 * If the onscreen display is enabled, capture motion events to control it.
	 */
	
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		if (directionsVisible) {
			if (gestureDetector.onTouchEvent(event)) {
				return true;
			}
		}
		return super.onTouchEvent(event);
	}
	
	/**
	 * Keyboard forward-back navigation for onscreen directions.
	 */
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (directionsVisible) {
			if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				nextStep();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				lastStep();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/* (non-Javadoc)
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	@Override
	public void onInit(int arg0) {
		// TODO Auto-generated method stub
		
	}


}
