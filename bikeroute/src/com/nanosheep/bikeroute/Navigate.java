package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.adapter.FindPlaceAdapter;
import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.service.RoutePlannerService;
import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.AbstractContactAccessor;
import com.nanosheep.bikeroute.utility.Parking;
import com.nanosheep.bikeroute.utility.Route;
import com.nanosheep.bikeroute.utility.StringAddress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
 * @version Oct 3, 2010
 */
public class Navigate extends Activity {	
	
	/** Start Address box. **/
	private transient AutoCompleteTextView startAddressField;
	/** End address box. **/
	private transient AutoCompleteTextView endAddressField;

	/** Parking manager. */
	private Parking prk;
	
	/** Address db. **/
	private AddressDatabase db;

	/** Is a search running. **/
	public boolean isSearching;

	/** Contact accessor to navigate to contact. **/
	protected AbstractContactAccessor mContactAccessor;
	private Intent searchIntent;
	protected BroadcastReceiver routeReceiver;

	/**Is planning dialog showing. **/
	private static boolean mShownDialog;
	
	
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	mContactAccessor = AbstractContactAccessor.getInstance();
	requestWindowFeature(Window.FEATURE_RIGHT_ICON);
	setContentView(R.layout.findplace);
	setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON, R.drawable.ic_bar_bikeroute);
	
	searchIntent = new Intent(this, RoutePlannerService.class);
	
	isSearching = false;
	mShownDialog = false;
	
	//DB
	db = ((BikeRouteApp) getApplication()).getDb();
	
	//Parking manager
	prk = new Parking(this);
	
	//Initialise geocoder
	final Geocoder geocoder = new Geocoder(this);
	
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
	final LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	
	Location self = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	if (self == null) {
		self = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
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
	
	//Handle rotations
	final Object[] data = (Object[]) getLastNonConfigurationInstance();
	if (data != null) {
		isSearching = (Boolean) data[2];
		if (isSearching) {
			routeReceiver = new RouteReceiver();
			registerReceiver(routeReceiver, new IntentFilter(RoutePlannerService.INTENT_ID));
		}
		mShownDialog = (Boolean) data[1];
		startAddressField.setText((String) data[3]);
		endAddressField.setText((String) data[4]);
	}
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
			searchIntent.putExtra(RoutePlannerService.PLAN_TYPE, RoutePlannerService.ADDRESS_PLAN);
			searchIntent.putExtra(RoutePlannerService.START_ADDRESS, startAddressField.getText().toString());
			searchIntent.putExtra(RoutePlannerService.END_ADDRESS, endAddressField.getText().toString());
			requestRoute();
		}
	}
	
	/**
	 * Overridden to deal with rotations which require tracking
	 * displayed dialog to ensure it is not duplicated.
	 */
	
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == BikeRouteConsts.PLAN) {
			mShownDialog = true;
		}
	}
	
	/**
	 * Request a route from the planning service, register a receiver to handle it.
	 */
	private void requestRoute() {
		showDialog(BikeRouteConsts.PLAN);
		isSearching = true;
		startService(searchIntent);
		routeReceiver = new RouteReceiver();
		registerReceiver(routeReceiver, new IntentFilter(RoutePlannerService.INTENT_ID));
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
		case BikeRouteConsts.PLAN:
			pDialog = new ProgressDialog(this);
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(final DialogInterface arg0) {
					removeDialog(BikeRouteConsts.PLAN);
					if (!Navigate.this.isSearching) {
						mShownDialog = false;
					}
				}
			});
			pDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(final DialogInterface arg0) {
					if (isSearching) {
						stopService(searchIntent);
						unregisterReceiver(routeReceiver);
						isSearching = false;
					}
				}
				
			});
			dialog = pDialog;
			break;
		case BikeRouteConsts.PLAN_FAIL_DIALOG:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.planfail_msg)).setCancelable(
					true).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
						}
					});
			dialog = builder.create();
			break;
		case BikeRouteConsts.IOERROR:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.io_error_msg)).setCancelable(
					true).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		case BikeRouteConsts.ARGERROR:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.arg_error_msg)).setCancelable(
					true).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		case BikeRouteConsts.RES_ERROR:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.result_error_msg)).setCancelable(
					true).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
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
		inflater.inflate(R.menu.navigate_menu, menu);
		return true;
	}
	
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem steps = menu.findItem(R.id.directions);
		final MenuItem back = menu.findItem(R.id.bike);
		final MenuItem stand  = menu.findItem(R.id.stand);
		steps.setVisible(false);
		if (((BikeRouteApp)getApplication()).getRoute() != null) {
			steps.setVisible(true);
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
		switch(item.getItemId()) {
		case R.id.prefs:
			intent = new Intent(this, Preferences.class);
			startActivity(intent);
			break;
		case R.id.directions:
			intent = new Intent(this, DirectionsView.class);
			startActivity(intent);
			break;
		case R.id.map:
			intent = new Intent(this, LiveRouteMap.class);
			startActivity(intent);
			break;
		case R.id.bike:
			searchIntent.putExtra(RoutePlannerService.PLAN_TYPE, RoutePlannerService.BIKE_PLAN);
			searchIntent.putExtra(RoutePlannerService.START_ADDRESS, startAddressField.getText().toString());
			requestRoute();
			break;
		case R.id.stand:
			searchIntent.putExtra(RoutePlannerService.PLAN_TYPE, RoutePlannerService.STANDS_PLAN);
			searchIntent.putExtra(RoutePlannerService.START_ADDRESS, startAddressField.getText().toString());
			requestRoute();
			break;
		case R.id.about:
			showDialog(BikeRouteConsts.ABOUT);
			break;
		case R.id.contacts:
			startActivityForResult(mContactAccessor.getPickContactIntent(), 0);
		}
		return true;
	}
	
	/**
	 * Handles callbacks from searchtasks.
	 * Dismisses any shown planning dialog, loads the routemap
	 * if the search was successful, displays an error if not.
	 * @param msg Result status message from search task.
	 * @param route Route returned by the planner, or null.
	 */
	
	private void searchComplete(final Integer msg, final Route route) {
		if (mShownDialog) {
			dismissDialog(BikeRouteConsts.PLAN);
		}
		unregisterReceiver(routeReceiver);
		if (msg != null) {
			if (msg == BikeRouteConsts.RESULT_OK) {
				db.insert(startAddressField.getText().toString());
				if (!"".equals(endAddressField.getText().toString())) {
					db.insert(endAddressField.getText().toString());
				}
				final BikeRouteApp app = (BikeRouteApp) getApplication();
				app.setRoute(route);
				final Intent map = new Intent(this, LiveRouteMap.class);
				startActivity(map);
			} else {
				showDialog(msg);
			}
		}
	}
	
	/**
   	 * Callback for contact picker activity - requests a string address for the contact
   	 * chosen.
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == RESULT_OK) {
            loadContactAddress(data.getData());
        }
    }
    
    /**
     * Load contact address on a background thread and set as destination.
     */
    private void loadContactAddress(final Uri contactUri) {
        final AsyncTask<Uri, Void, String> task = new AsyncTask<Uri, Void, String>() {

            @Override
            protected String doInBackground(Uri... uris) {
                return mContactAccessor.loadAddress(getContentResolver(), uris[0]);
            }

            @Override
            protected void onPostExecute(final String result) {
            	endAddressField.setText(result);
            }
        };

        task.execute(contactUri);
    }
    
    private class RouteReceiver extends BroadcastReceiver {

		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			Navigate.this.searchComplete(intent.getIntExtra("msg", 0), 
					(Route) intent.getParcelableExtra("route"));
			Navigate.this.isSearching = false;
		}
    	
    }
	
	/**
	 * Overridden to preserve searchtask, dialog & textfields on rotation.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[5];
		objs[1] = mShownDialog;
		objs[2] = isSearching;
		objs[3] = startAddressField.getText().toString();
		objs[4] = endAddressField.getText().toString();
	    return objs;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(routeReceiver);
	}

}
