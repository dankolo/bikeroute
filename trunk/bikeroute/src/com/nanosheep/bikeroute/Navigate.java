package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.adapter.FindPlaceAdapter;
import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.Parking;
import com.nanosheep.bikeroute.utility.Stands;
import com.nanosheep.bikeroute.utility.StringAddress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

/**
 * A class for performing A to B navigation and planning routes.
 * 
 * @author jono@nanosheep.net
 * @version Jun 28, 2010
 */
public class Navigate extends Activity {	
	/** Geocoder. **/
	private transient Geocoder geocoder;
	
	/** Start address **/
	private transient Address start;
	/** Destination address. **/
	private transient Address end;
	
	/** Start Address box. **/
	private transient AutoCompleteTextView startAddressField;
	/** End address box. **/
	private transient AutoCompleteTextView endAddressField;
	
	/** Handler codes. **/
	/** Io/network error. **/
	public static final int IOERROR = 1;
	/** Argument exception. **/
	public static final int ARGERROR = 2;
	/** Empty result set. **/
	private static final int RES_ERROR = 3;
	/** Planning dialog id. **/
	public static final int PLAN = 4;
	/** Planning failed dialog id. **/
	public static final int PLAN_FAIL_DIALOG = 5;
	
	/** Route planner. **/
	private RouteManager planner;

	/** Parking manager. */
	private Parking prk;
	
	/** Route parcel. **/
	private Parcelable route;

	/** Segment id. **/
	private int segId;
	
	/** Current gps location. **/
	private Location self;
	
	/** Address db. **/
	private AddressDatabase db;
	
	
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_RIGHT_ICON);
	setContentView(R.layout.findplace);
	setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON, R.drawable.ic_bar_bikeroute);
	
	//DB
	db = ((BikeRouteApp) getApplication()).getDb();
	
	//Parking manager
	prk = new Parking(this);
	
	//Route and route overlay
	Bundle routeBundle = getIntent().getExtras();
	
	if (routeBundle != null) {
		route = routeBundle.getParcelable(Route.ROUTE);
		segId = routeBundle.getInt("segment", -1);
	}
	
	//Initialise route planner
	planner = new RouteManager(this);
	
	//Initialise geocoder
	geocoder = new Geocoder(this);
	
	//Initialise fields
	startAddressField = (AutoCompleteTextView) findViewById(R.id.start_address_input);
	endAddressField = (AutoCompleteTextView) findViewById(R.id.end_address_input);		
	final Button searchButton = (Button) findViewById(R.id.search_button);
	
	//Initialise adapter
	final FindPlaceAdapter adapter = new FindPlaceAdapter(this,
			android.R.layout.simple_dropdown_item_1line);
	startAddressField.setAdapter(adapter);
	endAddressField.setAdapter(adapter);
	
	
	
	/* Get current lat & lng if available. */
	LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	self = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	
	/* Autofill starting location by reverse geocoding current
	 * lat & lng
	 */
	
	if (self != null) {
		try {
			final Address startAddress = geocoder.getFromLocation(self.getLatitude(),
				self.getLongitude(), 1).get(0);
			startAddressField.setText(StringAddress.asString(startAddress));
		} catch (Exception e) {
			Log.e(e.getMessage(), "FindPlace - location: " + self);
		}
	}
	
	//Initialise search button
	searchButton.setOnClickListener(new SearchClickListener());
	}
	
	/**
	 * Handler for navigation requests from the text boxes.
	 * Initiates a geocode on the addresses, passes that to
	 * the route planner and moves to the routemap once planning is
	 * completed.
	 * @author jono@nanosheep.net
	 * @version Jun 28, 2010
	 */
	
	private class SearchClickListener implements OnClickListener {
		public void onClick(final View view) {
			Navigate.this.showDialog(Navigate.PLAN);
			/* Spawn a new thread to geocode the input addresses
			 * return the 1st found to the handler.
			 */
			final Thread search = new Thread() {
				@Override
				public void run() {
					String startAddressInput = startAddressField.getText().toString();
					String endAddressInput = endAddressField.getText().toString();
					int msg;
					if ("".equals(startAddressInput) || "".equals(endAddressInput)) {
						msg = ARGERROR;
					} else {
						msg = RESULT_OK;
						try {
							planner.setStart(startAddressInput);
							planner.setDest(endAddressInput);		
							if (!planner.showRoute()) {
								msg = PLAN_FAIL_DIALOG;
							}
						} catch (Exception e) {
							msg = IOERROR;
							Log.e(e.getMessage(), "Navigate: " + startAddressInput +
								" - " + endAddressInput);
						}
					}
					Navigate.this.results.sendEmptyMessage(msg);
				}
			};
			search.start();
		}
	}
	
	/**
	 * Creates dialogs for loading, on errors, alerts.
	 * Available dialogs:
	 * Planning progress, planning error.
	 * @return the approriate Dialog object
	 */
	
	public Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder;
		ProgressDialog pDialog;
		Dialog dialog;
		switch(id) {
		case PLAN:
			pDialog = new ProgressDialog(this);
			pDialog.setCancelable(false);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					Navigate.this.removeDialog(PLAN);
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
		case IOERROR:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.io_error_msg)).setCancelable(
					false).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		case ARGERROR:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.arg_error_msg)).setCancelable(
					false).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		case RES_ERROR:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.result_error_msg)).setCancelable(
					false).setPositiveButton("OK",
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
		inflater.inflate(R.menu.navigate_menu, menu);
		return true;
	}
	
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem steps = menu.findItem(R.id.directions);
		final MenuItem back = menu.findItem(R.id.bike);
		final MenuItem stand  = menu.findItem(R.id.stand);
		if (route != null) {
			steps.setVisible(true);
		} else {
			steps.setVisible(false);
		}
		back.setVisible(false);
		stand.setVisible(false);
		if (startAddressField.getText().length() > 0) {
			stand.setVisible(true);
			if (prk.isParked()) {
				back.setVisible(true);
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
		Thread thread;
		switch(item.getItemId()) {
		case R.id.directions:
			intent = new Intent(this, DirectionsView.class);
			if (route != null) {
				intent.putExtra(Route.ROUTE, route);
				intent.putExtra("segment", segId);
			}
			startActivity(intent);
			break;
		case R.id.map:
			intent = new Intent(this, RouteMap.class);
			if (route != null) {
				intent.putExtra(Route.ROUTE, route);
				intent.putExtra("segment", segId);
			}
			startActivity(intent);
			break;
		case R.id.bike:
			Navigate.this.showDialog(Navigate.PLAN);
			thread = new Thread() {
				@Override
				public void run() {
					int msg;
					String startAddressInput = startAddressField.getText().toString();
					if ("".equals(startAddressInput)) {
						msg = ARGERROR;
					} else {
						try {
							msg = RESULT_OK;
							planner.setStart(startAddressInput);
							planner.setDest(prk.getLocation());	
							if (!planner.showRoute()) {
								msg = PLAN_FAIL_DIALOG;
							}
						} catch (Exception e) {
							msg = IOERROR;
							Log.e(e.getMessage(), "Navigate bike: " + startAddressInput);
						}
					}
					Navigate.this.results.sendEmptyMessage(msg);
				}
			};
			thread.start();
			break;
		case R.id.stand:
			Navigate.this.showDialog(Navigate.PLAN);
			thread = new Thread() {
				@Override
				public void run() {
					int msg;
					String startAddressInput = startAddressField.getText().toString();
					if ("".equals(startAddressInput)) {
						msg = ARGERROR;
					} else {
						try {
							msg = RESULT_OK;
							planner.setStart(startAddressInput);
							planner.setDest(Stands.getNearest(planner.getStart()));	
							if (!planner.showRoute()) {
								msg = PLAN_FAIL_DIALOG;
							}
						} catch (Exception e) {
							msg = IOERROR;
							Log.e(e.getMessage(), "Navigate stands: " + startAddressInput);
						}
					}
					Navigate.this.results.sendEmptyMessage(msg);
				}
			};
			thread.start();
			break;
		}
		return true;
	}
	
	/**
	 * Handler for planning threads.
	 */
	
	private final Handler results = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			if (msg.what == RESULT_OK) {
				Navigate.this.dismissDialog(Navigate.PLAN);
				db.insert(startAddressField.getText().toString());
				if (!"".equals(endAddressField.getText().toString())) {
					db.insert(endAddressField.getText().toString());
				}
				((BikeRouteApp)getApplication()).setRoute(planner.getRoute());
				Intent map = new Intent(Navigate.this, RouteMap.class);
				startActivity(map);
			} else {
				Navigate.this.dismissDialog(Navigate.PLAN);
				showDialog(msg.what);
			}
		}
	};

}
