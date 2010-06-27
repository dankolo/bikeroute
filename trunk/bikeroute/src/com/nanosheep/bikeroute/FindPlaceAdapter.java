package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.List;

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
	private final Geocoder geocoder;

	public FindPlaceAdapter(final Context context, final int resource,
			final int txtViewResId) {
		super(context, resource, txtViewResId);
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
		return new GeoFilter();
	}
	
	/**
	 * Psuedo filter, returns addresses based on an input char sequence.
	 * @author jono@nanosheep.net
	 * @version Jun 27, 2010
	 */
	
	private class GeoFilter extends Filter {
		private List<Address> addresses;
		
		/**
		 * Perform filtering by using the character sequence
		 * as a search string for a geocoder.
		 */
		
		@Override
		protected Filter.FilterResults performFiltering(final CharSequence ch) {
			final Filter.FilterResults res = new Filter.FilterResults();
			if (ch == null) {
				res.count = 0;
			} else {
				final String addressInput = ch.toString();
				final Activity act = (Activity) getContext();
				try {
					addresses = geocoder.getFromLocationName(addressInput, 5);
				} catch (IOException e) {
					act.showDialog(FindPlace.IOERROR);
				} catch (IllegalArgumentException e) {
					act.showDialog(FindPlace.ARGERROR);
				}
				
				res.count = addresses.size();
				res.values = addresses;
			}
			return res;
		}
		
		/**
		 * Publish results back to the adapter for display.
		 */

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(final CharSequence constraint,
				final FilterResults results) {
			if (results.count > 0) {
				clear();
				for (Address address : (List<Address>) results.values) {
					add(addressToString(address));
				}
				FindPlaceAdapter.this.notifyDataSetChanged();
			} else {
				FindPlaceAdapter.this.notifyDataSetInvalidated();
			}
		}
		
		/**
		 * Convert an address result to a string for display in the
		 * adapter.
		 * @param address Address object to convert
		 * @return a string of the address.
		 */
		
		public String addressToString(final Address address) {
			final StringBuffer sb = new StringBuffer();
			final int top = address.getMaxAddressLineIndex() + 1;
			for (int i = 0; i < top; i++) {
				sb.append(address.getAddressLine(i));
				if (i != top - 1) {
					sb.append(", ");
				}
			}
			return sb.toString();			
		}
	}
}
