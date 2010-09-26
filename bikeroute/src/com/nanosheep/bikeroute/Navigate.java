package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.adapter.FindPlaceAdapter;
import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.ContactAccessor;
import com.nanosheep.bikeroute.utility.Parking;
import com.nanosheep.bikeroute.utility.Route;
import com.nanosheep.bikeroute.utility.RouteManager;
import com.nanosheep.bikeroute.utility.Stands;
import com.nanosheep.bikeroute.utility.StringAddress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
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
 * @version Jun 28, 2010
 */
public class Navigate extends Activity {	
	/** Geocoder. **/
	private transient Geocoder geocoder;
	
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

	private static final int ABOUT = 0;

	/** Parking manager. */
	private Parking prk;
	
	/** Current gps location. **/
	private Location self;
	
	/** Address db. **/
	private AddressDatabase db;
	
	/** Planning thread. **/
	private SearchTask search;

	/** Is a search running. **/
	public boolean isSearching;

	/** Contact accessor to navigate to contact. **/
	protected ContactAccessor mContactAccessor;

	/**Is planning dialog showing. **/
	private static boolean mShownDialog;
	/** Activity ref. **/
	private static Navigate mAct;
	
	
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	mContactAccessor = ContactAccessor.getInstance();
	mAct = this;
	requestWindowFeature(Window.FEATURE_RIGHT_ICON);
	setContentView(R.layout.findplace);
	setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON, R.drawable.ic_bar_bikeroute);
	
	isSearching = false;
	mShownDialog = false;
	
	//DB
	db = ((BikeRouteApp) getApplication()).getDb();
	
	//Parking manager
	prk = new Parking(this);
	
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
		search = (SearchTask) data[0];
		isSearching = (Boolean) data[2];
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
			/* Spawn a new thread to geocode the input addresses
			 * return the 1st found to the handler.
			 */
			search = new SearchTask();
			search.execute();
		}
	}
	
	/**
	 * Overridden to deal with rotations which require tracking
	 * displayed dialog to ensure it is not duplicated.
	 */
	
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		if (id == PLAN) {
			mShownDialog = true;
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
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					removeDialog(PLAN);
					if (!Navigate.mAct.isSearching) {
						mShownDialog = false;
					}
				}
			});
			pDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface arg0) {
					if (isSearching) {
						mAct.search.cancel(true);
						isSearching = false;
					}
				}
				
			});
			dialog = pDialog;
			break;
		case PLAN_FAIL_DIALOG:
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
		case IOERROR:
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
		case ARGERROR:
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
		case RES_ERROR:
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
		case ABOUT:
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
		if (((BikeRouteApp)getApplication()).getRoute() != null) {
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
			intent = new Intent(this, RouteMap.class);
			startActivity(intent);
			break;
		case R.id.bike:
			search = new BikeTask();
			search.execute();
			break;
		case R.id.stand:
			search = new StandsTask();
			search.execute();
			break;
		case R.id.about:
			showDialog(Navigate.ABOUT);
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
	
	private void searchComplete(Integer msg, Route route) {
		if (msg != null) {
			if (mShownDialog) {
				dismissDialog(Navigate.PLAN);
			}
			if (msg == RESULT_OK) {
				db.insert(startAddressField.getText().toString());
				if (!"".equals(endAddressField.getText().toString())) {
					db.insert(endAddressField.getText().toString());
				}
				BikeRouteApp app = (BikeRouteApp) getApplication();
				app.setRoute(route);
				Intent map = new Intent(this, RouteMap.class);
				startActivity(map);
			} else {
				showDialog(msg);
			}
		}
	}

	
	/**
	 * Search task, extend to override doInBackGround method to perform
	 * searches.
	 * Displays a planning dialog, searches, then transitions to a map displaying the
	 * located route if one is found and adds start & destination to a db of recently used
	 * addresses, displays an error if planning failed.
	 * @author jono@nanosheep.net
	 * @version Jul 25, 2010
	 */
	
	private static class SearchTask extends AsyncTask<Void, Void, Integer> {
		protected RouteManager planner;
		protected String startAddressInput;
		protected String endAddressInput;
		
		
		public SearchTask() {
			super();
			planner = new RouteManager(mAct);
			startAddressInput = mAct.startAddressField.getText().toString();
			endAddressInput = mAct.endAddressField.getText().toString();
		}
		
		@Override
		protected void onPreExecute() {
			mAct.showDialog(Navigate.PLAN);
			mAct.isSearching = true;
		}
		@Override
		protected Integer doInBackground(Void... arg0) {
			int msg;
			if ("".equals(startAddressInput) || "".equals(endAddressInput)) {
				msg = ARGERROR;
			} else {
				msg = RESULT_OK;
				try {
					planner.setStart(startAddressInput);
					planner.setDest(endAddressInput);		
					if (isCancelled()) {
						return null;
					}
					if (!planner.showRoute()) {
						msg = PLAN_FAIL_DIALOG;
					}
				} catch (Exception e) {
					msg = IOERROR;
					Log.e(e.getMessage(), "Navigate: " + startAddressInput +
								" - " + endAddressInput);
				}
			}
			if (isCancelled()) {
				return null;
			}
			return msg;
		}
		@Override
		protected void onPostExecute(final Integer msg) {
			Navigate.mAct.searchComplete(msg, planner.getRoute());
			Navigate.mAct.isSearching = false;
		}
		
		@Override
		protected void onCancelled() {
			Navigate.mAct.search = null;
			Navigate.mAct.isSearching = false;
			super.onCancelled();
		}
	}
	
	/**
	 * Extends SearchTask to specific case of searching for a cyclestand near
	 * whatever was entered in the start destination.
	 * @author jono@nanosheep.net
	 * @version Jul 26, 2010
	 */
	
	private static class StandsTask extends SearchTask {
		@Override
		protected Integer doInBackground(final Void... unused) {
			int msg;
			if ("".equals(startAddressInput)) {
				msg = ARGERROR;
			} else {
				try {
					msg = RESULT_OK;
					planner.setStart(startAddressInput);
					planner.setDest(Stands.getNearest(planner.getStart()));	
					if (isCancelled()) {
						return null;
					}
					if (!planner.showRoute()) {
						msg = PLAN_FAIL_DIALOG;
					}
				} catch (Exception e) {
					msg = IOERROR;
					Log.e(e.getMessage(), "Navigate stands: " + startAddressInput);
					}
				}
			if (isCancelled()) {
				return null;
			}
			return msg;
		}
	}
	
	/**
	 * Extends SearchTask to the specific case of planning a route back to
	 * the stored location of the bicycle.
	 * @author jono@nanosheep.net
	 * @version Jul 26, 2010
	 */
	
	private static class BikeTask extends SearchTask {
		@Override
		protected Integer doInBackground(Void... arg0) {
			int msg; 
			if ("".equals(startAddressInput)) {
				msg = ARGERROR;
			} else {
				try {
					msg = RESULT_OK;
					planner.setStart(startAddressInput);
					planner.setDest(mAct.prk.getLocation());	
					
					if (isCancelled()) {
						return null;
					}
		
					if (!planner.showRoute()) {
						msg = PLAN_FAIL_DIALOG;
					}
				} catch (Exception e) {
					msg = IOERROR;
					Log.e(e.getMessage(), "Navigate bike: " + startAddressInput);
				}
			}
			if (isCancelled()) {
				return null;
			}
			return msg;
		}
	}
	
	/**
   	 * Callback for contact picker activity - requests a string address for the contact
   	 * chosen.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            loadContactAddress(data.getData());
        }
    }
    
    /**
     * Load contact address on a background thread and set as destination.
     */
    private void loadContactAddress(Uri contactUri) {
        AsyncTask<Uri, Void, String> task = new AsyncTask<Uri, Void, String>() {

            @Override
            protected String doInBackground(Uri... uris) {
                return mContactAccessor.loadAddress(getContentResolver(), uris[0]);
            }

            @Override
            protected void onPostExecute(String result) {
            	endAddressField.setText(result);
            }
        };

        task.execute(contactUri);
    }
	
	/**
	 * Overridden to preserve searchtask, dialog & textfields on rotation.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[5];
		objs[0] = search;
		objs[1] = mShownDialog;
		objs[2] = isSearching;
		objs[3] = startAddressField.getText().toString();
		objs[4] = endAddressField.getText().toString();
	    return objs;
	}

}
