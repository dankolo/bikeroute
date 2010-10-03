/**
 * 
 */
package com.nanosheep.bikeroute.adapter;

import java.util.List;

import com.nanosheep.bikeroute.R;
import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.Segment;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An adapter for displaying Segment objects as a set of directions.
 * 
 * @author jono@nanosheep.net
 * @version Jun 28, 2010
 */
public class DirectionListAdapter extends ArrayAdapter<Segment> {
	/** Layout inflater . **/
	private final transient LayoutInflater inflater;
	/** Units. **/
	private final String unit;

	/**
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public DirectionListAdapter(final Context context, final int textView,
			final List<Segment> objects) {
		super(context, textView, objects);
		inflater = LayoutInflater.from(context);
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		unit = settings.getString("unitsPref", "km");
	}
	
	/**
	 * Get a view which displays an instruction with road name, a distance
	 * for that segment and total distance covered below, with an icon on
	 * the righthand side.
	 */
	
	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final Segment segment = getItem(position);
		final View view = inflater.inflate(R.layout.direction_item, null);
		final TextView turn = (TextView) view.findViewById(R.id.turn);
		final TextView distance = (TextView) view.findViewById(R.id.distance);
		final TextView length = (TextView) view.findViewById(R.id.length);
		
		turn.setText(segment.getInstruction());
		if ("km".equals(unit)) {
			length.setText(Convert.asMeterString(segment.getLength()));
			distance.setText("(" + Convert.asKilometerString(segment.getLength()) + ")");
			
		} else {
			length.setText(Convert.asFeetString(segment.getLength()));
			distance.setText("(" + Convert.asMilesString(segment.getLength()) + ")");
		}
		
		return view;
	}

}
