package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
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
	private final Geocoder geocoder;

	public FindPlaceAdapter(final Context context, final int resource,
			final int textViewResourceId) {
		super(context, resource, textViewResourceId);
		geocoder = new Geocoder(context, Locale.getDefault());
	}

	public FindPlaceAdapter(final Context context, final int resource) {
		super(context, resource);
		geocoder = new Geocoder(context, Locale.getDefault());
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
					Activity act = (Activity) getContext();
					try {
						addresses = geocoder.getFromLocationName(addressInput, 5);
					} catch (IOException e) {
						act.showDialog(FindPlace.IOERROR);
					} catch (IllegalArgumentException e) {
						act.showDialog(FindPlace.ARGERROR);
					}
					for (Address addr : addresses) {
						StringBuffer sb = new StringBuffer();
						final int top = addr.getMaxAddressLineIndex() + 1;
						for (int i = 0; i < top; i++) {
							sb.append(addr.getAddressLine(i));
							if (i != top - 1) {
								sb.append(", ");
							}
						}
						addrs.add(sb.toString());
					}

					res.count = addrs.size();
					res.values = addrs;
				}
				return res;
			}

			@SuppressWarnings("unchecked")
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
