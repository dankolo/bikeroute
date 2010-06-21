package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.MapActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

/**
 * Activity consisting of entering an address, geocoding it and then passing
 * that information back to a map.
 * 
 * @author jono@nanosheep.net
 * 
 */

public class FindPlace extends MapActivity {

	/** Lat & lng of resulting address. **/
	private List<Integer> latLng;
	/** Progress dialog. **/
	private ProgressDialog searching;
	/** Geocoder. **/
	private Geocoder geocoder;
	/** Search thread. **/
	private Thread search;
	/** Found addresses list. **/
	private List<Address> addresses;
	/** Address box. **/
	private AutoCompleteTextView addressField;
	private FindPlaceAdapter adapter;
	/** Handler codes. **/
	private static final int IOERROR = -1;
	private static final int ARGERROR = -2;
	private static final int OK = 1;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findplace);

		searching = new ProgressDialog(this);

		latLng = new ArrayList<Integer>();
		addresses = new ArrayList<Address>();

		final Button searchButton = (Button) findViewById(R.id.search_button);
		// final EditText addressField = (EditText)
		// findViewById(R.id.address_input);

		geocoder = new Geocoder(this);

		addressField = (AutoCompleteTextView) findViewById(R.id.address_input);
		adapter = new FindPlaceAdapter(this,
				android.R.layout.simple_dropdown_item_1line);
		addressField.setAdapter(adapter);

		searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				searching = ProgressDialog.show(FindPlace.this, "Working..",
						"Searching..", true, false);
				search = new Thread() {
					@Override
					public void run() {
						String addressInput = addressField.getText().toString();
						int msg = OK;
						try {
							addresses = geocoder.getFromLocationName(
									addressInput, 1);
							Thread.sleep(1500);
						} catch (IOException e) {
							msg = IOERROR;
						} catch (IllegalArgumentException e) {
							msg = ARGERROR;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						results.sendEmptyMessage(msg);
					}
				};
				search.start();
			}
		});

	}

	private Handler results = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			searching.dismiss();

			switch (msg.what) {
			case IOERROR:
				showIOError();
				break;
			case ARGERROR:
				break;
			default:
				if (addresses.size() == 0) {
					showResultError();
					break;
				} else {
					Address addr = addresses.get(0);
					latLng.add((int) (addr.getLatitude() * Degrees.CNV));
					latLng.add((int) (addr.getLongitude() * Degrees.CNV));
					finish();
				}
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

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void showIOError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(FindPlace.this);
		builder.setTitle("Error").setPositiveButton(R.string.ok, null)
				.setMessage("Network unavailable.");

		Dialog ioError = builder.create();
		ioError.show();
	}

	private void showResultError() {
		Dialog locationError = new AlertDialog.Builder(FindPlace.this).setIcon(
				0).setTitle("Error").setPositiveButton(R.string.ok, null)
				.setMessage("Address not found.").create();
		locationError.show();
	}

}
