package com.nanosheep.bikeroute;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.OpenStreetMapViewController;
import org.andnav.osm.views.overlay.MyLocationOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;
import org.andnav.osm.views.overlay.OpenStreetMapViewPathOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
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
import com.nanosheep.bikeroute.utility.Route;
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

public class RouteMap extends OpenStreetMapActivity implements OnInitListener {

	/** The controller for the view. */
	private OpenStreetMapViewController mc;
	/** Stand markers overlay. */
	private LiveMarkers stands;
	/** Route overlay. **/
	private OpenStreetMapViewPathOverlay routeOverlay;
	/** Location manager. **/
	private LocationManager mLocationManager;
	
	/** Route. **/
	private Route route;
	
	/* Constants. */
	/** Initial zoom level. */
	private static final int ZOOM = 15;
	private static final Intent SEGMENT_START = null;

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
	private ProximityReceiver proxAlerter;
	

	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		
		/* Get location manager. */
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		proxAlerter = new ProximityReceiver();
		
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		unit = settings.getString("unitsPref", "km");
		tts = settings.getBoolean("tts", false);
		
		//Detect swipes (left & right, taps.)
        gestureDetector = new GestureDetector(this, new TurnByTurnGestureListener(this));
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(final View v, final MotionEvent event) {
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
		
		mc = mOsmv.getController();
		mc.setZoom(ZOOM);
		
		//Directions overlay
		final View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.setVisibility(View.INVISIBLE);
		
		
		//Route and segment
		route = ((BikeRouteApp)getApplication()).getRoute();
		segId = ((BikeRouteApp)getApplication()).getSegId();
		
		if (route != null) {
			routeOverlay = new RouteOverlay(Color.BLUE,this);
			for(GeoPoint pt : route.getPoints()) {
				routeOverlay.addPoint(pt);
			}
			mOsmv.getOverlays().add(routeOverlay);
			currSegment = route.getSegments().get(segId);
			if (getIntent().getBooleanExtra("jump", false)) {
				showStep();
			}
			mc.setCenter(currSegment.startPoint());
		}


		// Initialize stands overlay
		stands = new LiveMarkers(mOsmv, this);

		// Initialize parking manager
		prk = new Parking(this);
		// Initialize bike alert manager
		bikeAlert = new BikeAlert(this);
		// Initialize location service
		mOsmv.getOverlays().add(mLocationOverlay);
				
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		if ((data != null) && ((Boolean) data[0])) {
				mc.setZoom(16);
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
		map.setVisible(false);
		steps.setVisible(false);
		turnByTurn.setVisible(false);
		elev.setVisible(false);
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
			mLocationOverlay.followLocation(true);
			RouteMap.this.mLocationOverlay.runOnFirstFix(new Runnable() {
				@Override
				public void run() {
					if (RouteMap.this.dialog.isShowing()) {
							RouteMap.this.dismissDialog(BikeRouteConsts.AWAITING_FIX);
							Location self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (self == null) {
								self = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
							}
							if (self != null) {
								RouteMap.this.mc.animateTo(
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
							Location self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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
			final XYMultipleSeriesRenderer renderer = route.getChartRenderer();
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
		final View overlay = (View) findViewById(R.id.directions_overlay);
		overlay.setVisibility(View.INVISIBLE);
		mOsmv.setClickable(true);
		directionsVisible = false;
		((BikeRouteApp) getApplication()).setSegId(0);
		if (tts) {
			directionsTts.stop();
		}
		this.mOsmv.setBuiltInZoomControls(true);
        this.mOsmv.setMultiTouchControls(true);
	}
	
	/**
	 * Show the currently selected step of directions onscreen, focus
	 * the map at the start of that section.
	 */
	
	public void showStep() {
		((BikeRouteApp) getApplication()).setSegId(segId);
		proxAlerter.setStepAlert(route.getSegments().get(segId + 1).startPoint());
		directionsVisible = true;
		this.mOsmv.setBuiltInZoomControls(false);
        this.mOsmv.setMultiTouchControls(false);
		currSegment = route.getSegments().get(segId);
		mc.setZoom(16);
		mc.setCenter(currSegment.startPoint());
		
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
		
		final TextView turn = (TextView) overlay.findViewById(R.id.turn);
		final TextView distance = (TextView) overlay.findViewById(R.id.distance);
		final TextView num = (TextView) overlay.findViewById(R.id.step_no);
		turn.setText(currSegment.getInstruction());
		
		final String distanceString = "km".equals(unit) ? Convert.asMeterString(currSegment.getLength()) + " ("
				+ Convert.asKilometerString(currSegment.getDistance()) + ")" : 
					Convert.asFeetString(currSegment.getLength()) + " ("
					+ Convert.asMilesString(currSegment.getDistance()) + ")";
		
		distance.setText(distanceString);
		num.setText(segId + 1 + "/" + route.getSegments().size());
		overlay.setVisibility(View.VISIBLE);
		
		if (tts) {
			if (directionsTts.isSpeaking()) {
				final String speech = "then " + currSegment.getInstruction();
				directionsTts.speak(speech, TextToSpeech.QUEUE_ADD, null);
			}	else {
				directionsTts.speak(currSegment.getInstruction(), TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	}
	
	

	/* (non-Javadoc)
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	@Override
	public void onInit(int arg0) {
		// TODO Auto-generated method stub
		
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
	
	private class ProximityReceiver extends BroadcastReceiver {
		private static final String INTENT_ID = "com.nanosheep.bikeroute.STEP";
		/** Intent filter. **/
		private final IntentFilter filter;
		/** Pending Intent. **/
		private final PendingIntent pi;

		public ProximityReceiver () {
			super();
			filter = new IntentFilter(INTENT_ID);
			pi = PendingIntent.getBroadcast(RouteMap.this, 0, new Intent(INTENT_ID),
					PendingIntent.FLAG_CANCEL_CURRENT);
		}
		
		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)) {
				unsetAlert();
				nextStep();
				setStepAlert(currSegment.startPoint());
			}
			return;
		}
		
		/**
		 * Set a proximity alert at the given point for updating directions step.
		 * 
		 * @param start point to alert at.
		 */

		public void setStepAlert(final GeoPoint start) {
			final LocationManager lm = (LocationManager) RouteMap.this.getSystemService(Context.LOCATION_SERVICE);
			lm.addProximityAlert(Convert.asDegrees(start.getLatitudeE6()),
					Convert.asDegrees(start.getLongitudeE6()), 20f, -1, pi);
			RouteMap.this.registerReceiver(this, filter);
		}

		/**
		 * Remove the alert.
		 */

		public void unsetAlert() {
			final LocationManager lm = (LocationManager) RouteMap.this.getSystemService(Context.LOCATION_SERVICE);
			lm.removeProximityAlert(pi);
		}
		
	}

}
