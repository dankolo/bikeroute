/**
 * 
 */
package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.adapter.DirectionListAdapter;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A class for displaying a list of directions.
 * 
 * @author jono@nanosheep.net
 * @version Jun 24, 2010
 */
public class DirectionsView extends ListActivity {
	/** Route object. **/
	private Route route;
	/** Segment id. **/
	private int segId;
	
	@Override
	public void onCreate(final Bundle in) {
		requestWindowFeature(Window.FEATURE_RIGHT_ICON);
		super.onCreate(in);

		//Get bundled route.
		final Bundle bundle = getIntent().getExtras();
	  
		route = bundle.getParcelable(Route.ROUTE);
		segId = bundle.getInt("segment", -1);
		setTitle(route.getName());
		setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON, R.drawable.ic_bar_bikeroute);
	  
		//Create a header for the list.
		TextView header = new TextView(this);
		StringBuffer sBuf = new StringBuffer("Total distance: ");
		sBuf.append(route.getLength());
		sBuf.append("m (");
		sBuf.append(route.getLength() / 1000);
		sBuf.append("km)");
		header.setText(sBuf.toString());
		getListView().addHeaderView(header, "", false);
	  
		//Create a footer to display warnings & copyrights
		TextView footer = new TextView(this);
		sBuf = new StringBuffer();
		if (route.getWarning() != null) {
			sBuf.append(route.getWarning());
			sBuf.append('\n');
		}
		if (route.getCopyright() != null) {
			sBuf.append(route.getCopyright());
		}
		footer.setText(sBuf.toString());
		getListView().addFooterView(footer, "", false);
		//Add the list of directions and set it filterable
		setListAdapter(new DirectionListAdapter(this, R.layout.direction_item, route.getSegments()));
		getListView().setTextFilterEnabled(true);
	}
	
	/**
	 * Return to the navigation activity if a direction item is
	 * clicked, focus the map there.
	 */
	
	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		Intent intent = new Intent(this, RouteMap.class);
		intent.putExtra(Route.ROUTE, route);
		intent.putExtra("segment", position - 1);
		intent.putExtra("jump", true);
		startActivity(intent);
		finish();
	}	
	
	/**
	 * Create the options menu.
	 * @return true if menu created.
	 */

	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.directions_menu, menu);
		return true;
	}
	
	/**
	 * Handle option selection.
	 * @return true if option selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final Intent intentDir;
		if (item.getItemId() ==  R.id.navigate) {
			intentDir = new Intent(this, Navigate.class);
			intentDir.putExtra(Route.ROUTE, route);
			intentDir.putExtra("segment", segId);
			startActivity(intentDir);
		} else {
			intentDir = new Intent(this, RouteMap.class);
			intentDir.putExtra(Route.ROUTE, route);
			intentDir.putExtra("segment", segId);
		}
		startActivity(intentDir);
		finish();
		return true;
	}
	
}
