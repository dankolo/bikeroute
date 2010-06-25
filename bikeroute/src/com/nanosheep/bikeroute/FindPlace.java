package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
	private List<Integer> latLng;
	/** Progress dialog. **/
	private ProgressDialog searching;
	/** Geocoder. **/
	private Geocoder geocoder;
	/** Found addresses list. **/
	private List<Address> addresses;
	/** Address box. **/
	private AutoCompleteTextView addressField;
	/** Handler codes. **/
	/** Io/network error. **/
	public static final int IOERROR = -1;
	/** Argument exception. **/
	public static final int ARGERROR = -2;
	/** Result ok. **/
	private static final int OK = 1;
	/** Empty result set. **/
	private static final int RES_ERROR = -3;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findplace);
		
		String title = getIntent().getStringExtra("title");
		setTitle(title);

		searching = new ProgressDialog(this);

		latLng = new ArrayList<Integer>();
		addresses = new ArrayList<Address>();

		final Button searchButton = (Button) findViewById(R.id.search_button);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

		geocoder = new Geocoder(this);

		addressField = (AutoCompleteTextView) findViewById(R.id.address_input);
		final FindPlaceAdapter adapter = new FindPlaceAdapter(this,
				android.R.layout.simple_dropdown_item_1line);
		addressField.setAdapter(adapter);

		searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				searching = ProgressDialog.show(FindPlace.this, "Working..",
						"Searching..", true, false);
				final Thread search = new Thread() {
					@Override
					public void run() {
						String addressInput = addressField.getText().toString();
						int msg = OK;
						try {
							addresses = geocoder.getFromLocationName(
									addressInput, 1);
						} catch (IOException e) {
							msg = IOERROR;
						} catch (IllegalArgumentException e) {
							msg = ARGERROR;
						}
						results.sendEmptyMessage(msg);
					}
				};
				search.start();
			}
		});

	}

	private final Handler results = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			searching.dismiss();
			if (msg.what == OK) {
				if (addresses.isEmpty()) {
					showDialog(RES_ERROR);
				} else {
					final Address addr = addresses.get(0);
					latLng.add(Degrees.asMicroDegrees(addr.getLatitude()));
					latLng.add(Degrees.asMicroDegrees(addr.getLongitude()));
					finish();
				}
			} else {
				showDialog(msg.what);
			}
		}
	};

	/**
	 * Set latLng as extra to return to main activity.
	 */

	@Override
	public void finish() {
		final Intent intent = new Intent();
		if (latLng.size() == 2) {
			intent.putIntegerArrayListExtra("latLng",
					(ArrayList<Integer>) latLng);
			setResult(RESULT_OK, intent);
		} else {
			setResult(RESULT_CANCELED, intent);
		}
		super.finish();
	}
	
	/**
	 * Creates dialogs for loading, on errors, alerts.
	 * Available dialogs:
	 * Planning progress, planning error, unpark.
	 * @return the approriate Dialog object
	 */
	
	public Dialog onCreateDialog(final int id) {
		Dialog dialog;
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);;
		switch(id) {
		case IOERROR:
			builder.setMessage(getText(R.string.io_error_msg)).setCancelable(
					false).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
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
