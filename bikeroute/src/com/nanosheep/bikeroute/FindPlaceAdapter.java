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

	public FindPlaceAdapter(final Context context, final int resource,
			final int textViewResourceId) {
		super(context, resource, textViewResourceId);
		geocoder = new Geocoder(context);
	}

	public FindPlaceAdapter(final Context context, final int resource) {
		super(context, resource);
		geocoder = new Geocoder(context);
	}

	/**
	 * Replaces ArrayAdapter's filter with one that retrieves suggestions
	 * from the geocoder service.
	 * @return a Filter object
	 */
	
	@Override
	public Filter getFilter() {
		final Filter filter = new Filter() {
			@Override
			protected Filter.FilterResults performFiltering(final CharSequence ch) {
				Filter.FilterResults res = new Filter.FilterResults();
				ArrayList<String> addrs = new ArrayList<String>();
				if (ch == null) {
					res.count = 0;
				} else {
					String addressInput = ch.toString();
					try {
						addresses = geocoder.getFromLocationName(addressInput, 5);
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
				}
				return res;
			}

			@Override
			protected void publishResults(final CharSequence constraint,
					final FilterResults results) {
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
