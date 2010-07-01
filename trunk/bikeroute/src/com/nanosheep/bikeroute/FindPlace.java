package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.nanosheep.bikeroute.adapter.FindPlaceAdapter;
import com.nanosheep.bikeroute.utility.Degrees;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

/**
 * Activity consisting of entering an address, geocoding it and then passing
 * that information back to a map.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class FindPlace extends Activity {

	/** Lat & lng of resulting address. **/
	private transient List<Integer> latLng;
	/** Country for the address. **/
	private transient String country;
	/** Progress dialog. **/
	private transient ProgressDialog searching;
	/** Geocoder. **/
	private transient Geocoder geocoder;
	/** Found addresses list. **/
	private transient List<Address> addresses;
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

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findplace);
		
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
		final int startLat = getIntent().getIntExtra("lat", -1);
		final int startLng = getIntent().getIntExtra("lng", -1);
		
		/* Autofill starting location by reverse geocoding current
		 * lat & lng
		 */
		if (startLat != -1 && startLng != -1) {
			try {
				final Address startAddress = geocoder.getFromLocation(Degrees.asDegrees(startLat),
					Degrees.asDegrees(startLng), 1).get(0);
				final StringBuffer sb = new StringBuffer();
				final int top = startAddress.getMaxAddressLineIndex() + 1;
				for (int i = 0; i < top; i++) {
					sb.append(startAddress.getAddressLine(i));
					if (i != top - 1) {
						sb.append(", ");
					}
				}
				startAddressField.setText(sb);
			} catch (Exception e) {
				Log.e(e.getMessage(), "FindPlace - lat: " +
						startLat + ", lng: " + startLng);
			}
		}

		//Initialise searching dialog
		searching = new ProgressDialog(this);

		//Initialise latlng extras arraylist & addresses.
		latLng = new ArrayList<Integer>();
		addresses = new ArrayList<Address>();

		//Initialise search button
		searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				searching = ProgressDialog.show(FindPlace.this, "",
						"Searching..", true, false);
				/* Spawn a new thread to geocode the input addresses
				 * return the 1st found to the handler.
				 */
				final Thread search = new Thread() {
					@Override
					public void run() {
						String startAddressInput = startAddressField.getText().toString();
						String endAddressInput = endAddressField.getText().toString();
						int msg = RESULT_OK;
						try {
							addresses = geocoder.getFromLocationName(
									startAddressInput, 1);
							addresses.addAll(geocoder.getFromLocationName(
									endAddressInput, 1));
						} catch (Exception e) {
							msg = IOERROR;
							Log.e(e.getMessage(), "FindPlace: " + startAddressInput +
									" - " + endAddressInput);
						}
						results.sendEmptyMessage(msg);
					}
				};
				search.start();
			}
		});

	}

	/**
	 * Handler for geocoding addresses. Calls finish if addresses
	 * are found, packages geopoints as integer array.
	 */
	
	private final Handler results = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			searching.dismiss();
			if (msg.what == RESULT_OK) {
				if (addresses.isEmpty()) {
					showDialog(RES_ERROR);
				} else {
					for (Address addr : addresses) {
						latLng.add(Degrees.asMicroDegrees(addr.getLatitude()));
						latLng.add(Degrees.asMicroDegrees(addr.getLongitude()));
						country = addr.getCountryCode();
					}
				}
				finish();
			} else {
				showDialog(msg.what);
			}
		}
	};

	/**
	 * Set latLng and countrycode of destination as extras
	 *  to return to main activity.
	 */

	@Override
	public void finish() {
		final Intent intent = new Intent();
		if (latLng.size() == 4) {
			intent.putIntegerArrayListExtra("latLng",
					(ArrayList<Integer>) latLng);
			intent.putExtra("country", country);
			setResult(RESULT_OK, intent);
		} else {
			setResult(RESULT_CANCELED, intent);
		}
		super.finish();
	}
	
	/**
	 * Creates dialogs for loading, on errors, alerts.
	 * Available dialogs:
	 * IOError, argument error, result error.
	 * @return the approriate Dialog object
	 */
	
	@Override
	public Dialog onCreateDialog(final int id) {
		Dialog dialog;
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch(id) {
		case IOERROR:
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
			break;
		}
		return dialog;
		}

}
