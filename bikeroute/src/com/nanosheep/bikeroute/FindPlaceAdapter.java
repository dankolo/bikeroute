package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.ArrayAdapter;
import android.widget.Filter;

/**
 * Overrides the arrayadapter to display a list of suggestions retrieved
 * from a geocoder.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class FindPlaceAdapter extends ArrayAdapter<String> {
	private List<Address> addresses;
	private Geocoder geocoder;

	public FindPlaceAdapter(Context context, int resource,
			int textViewResourceId) {
		super(context, resource, textViewResourceId);
		geocoder = new Geocoder(context);
	}

	public FindPlaceAdapter(Context context, int resource) {
		super(context, resource);
		geocoder = new Geocoder(context);
	}

	/**
	 * Replaces ArrayAdapter's filter with one that retrieves suggestions
	 * from the geocoder service.
	 */
	
	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@Override
			protected Filter.FilterResults performFiltering(CharSequence ch) {
				Filter.FilterResults res = new Filter.FilterResults();
				ArrayList<String> addrs = new ArrayList<String>();
				if (ch != null) {
					String addressInput = ch.toString();
					try {
						addresses = geocoder.getFromLocationName(addressInput,
								5);
					} catch (IOException e) {
						// showIOError();
					} catch (IllegalArgumentException e) {
						// showResultError();
					}
					// clear();
					for (Address addr : addresses) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < addr.getMaxAddressLineIndex() + 1; i++) {
							sb.append(addr.getAddressLine(i));
							if (i != addr.getMaxAddressLineIndex()) {
								sb.append(", ");
							}
						}
						addrs.add(sb.toString());
						// add(sb.toString());
					}

					res.count = addrs.size();
					res.values = addrs;
				} else {
					res.count = 0;
				}
				return res;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				if (results.count > 0) {
					clear();
					for (String s : (List<String>) results.values) {
						add(s);
					}
					FindPlaceAdapter.this.notifyDataSetChanged();
				} else {
					notifyDataSetInvalidated();
				}
			}
		};
		return filter;
	}
}
