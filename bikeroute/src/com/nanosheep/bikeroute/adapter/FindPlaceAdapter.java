package com.nanosheep.bikeroute.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nanosheep.bikeroute.FindPlace;
import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.StringAddress;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.nanosheep.bikeroute.BikeRouteApp;

/**
 * Overrides the arrayadapter to display a list of suggestions retrieved
 * from a geocoder.
 * @author jono@nanosheep.net
 * @version Jun 21, 2010
 */

public class FindPlaceAdapter extends ArrayAdapter<String> {
	private final Geocoder geocoder;
	/** Previous addresses db. **/
	private final AddressDatabase db;

	public FindPlaceAdapter(final Context context, final int resource,
			final int txtViewResId) {
		super(context, resource, txtViewResId);
		geocoder = new Geocoder(context);
		db = ((BikeRouteApp) context.getApplicationContext()).getDb();
	}

	public FindPlaceAdapter(final Context context, final int resource) {
		super(context, resource);
		geocoder = new Geocoder(context);
		db = ((BikeRouteApp) context.getApplicationContext()).getDb();
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
		 * as a search string for a geocoder and the existing sql store
		 * of previous addresses.
		 */
		
		@Override
		protected Filter.FilterResults performFiltering(final CharSequence ch) {
			final Filter.FilterResults res = new Filter.FilterResults();
			final Set<String> results = new HashSet<String>();
			if (ch == null) {
				res.count = 0;
			} else {
				final String addressInput = ch.toString();
				final Activity act = (Activity) getContext();
				
				//Add results from db
				results.addAll(db.selectLike(ch));
				//Search using geocoder
				try {
					addresses = geocoder.getFromLocationName(addressInput, 5);
				} catch (IOException e) {
					act.showDialog(FindPlace.IOERROR);
				} catch (IllegalArgumentException e) {
					act.showDialog(FindPlace.ARGERROR);
				}
				
				for (Address address : addresses) {
					results.add(StringAddress.asString(address));
				}
				
				res.count = results.size();
				res.values = results;
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
				for (String address : (Set<String>) results.values) {
					add(address);
				}
				FindPlaceAdapter.this.notifyDataSetChanged();
			} else {
				FindPlaceAdapter.this.notifyDataSetInvalidated();
			}
		}
	}
}
