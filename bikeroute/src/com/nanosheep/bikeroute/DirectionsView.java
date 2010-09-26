/**
 * 
 */
package com.nanosheep.bikeroute;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import com.nanosheep.bikeroute.adapter.DirectionListAdapter;
import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.Route;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
	private static final int ABOUT = 0;
	/** Route object. **/
	private Route route;
	/** Units. **/
	private String unit;
	
	@Override
	public void onCreate(final Bundle in) {
		requestWindowFeature(Window.FEATURE_RIGHT_ICON);
		super.onCreate(in);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		unit = settings.getString("unitsPref", "km");

		route = ((BikeRouteApp)getApplication()).getRoute();
		
		setTitle(route.getName());
		setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON, R.drawable.ic_bar_bikeroute);
	  
		//Create a header for the list.
		TextView header = new TextView(this);
		StringBuffer sBuf = new StringBuffer("Total distance: ");
		if ("km".equals(unit)) {
			sBuf.append(Convert.asMeterString(route.getLength()));
			sBuf.append(" (");
			sBuf.append(Convert.asKilometerString(route.getLength()));
			sBuf.append(')');
		} else {
			sBuf.append(Convert.asFeetString(route.getLength()));
			sBuf.append(" (");
			sBuf.append(Convert.asMilesString(route.getLength()));
			sBuf.append(')');
		}
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
		((BikeRouteApp)getApplication()).setSegId(position - 1);
		Intent intent = new Intent(this, RouteMap.class);

		intent.putExtra("jump", true);
		startActivity(intent);
		
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
		Intent intentDir = null;
		switch (item.getItemId()) {
		case R.id.navigate:
			intentDir = new Intent(this, Navigate.class);
			break;
		case R.id.map:
			intentDir = new Intent(this, RouteMap.class);
			break;
		case R.id.prefs:
			intentDir = new Intent(this, Preferences.class);
			break;
		case R.id.about:
			showDialog(ABOUT);
			return true;
		case R.id.elevation:
			XYMultipleSeriesDataset elevation = route.getElevations();
			XYMultipleSeriesRenderer renderer = route.getChartRenderer();
			if (!"km".equals(unit)) {
				elevation = Convert.asImperial(elevation);
				renderer.setYTitle("ft");
			    renderer.setXTitle("m");
			}
			renderer.setYAxisMax(elevation.getSeriesAt(0).getMaxY() + 200);
			intentDir = ChartFactory.getLineChartIntent(this, elevation, renderer);
		}
		startActivity(intentDir);
		
		return true;
	}
	
	/**
	 * Creates dialogs for loading, on errors, alerts.
	 * Available dialogs:
	 * Planning progress, planning error.
	 * @return the approriate Dialog object
	 */
	
	public Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.about_message)).setCancelable(
				true).setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						dialog.dismiss();
					}
				});
		return builder.create();
	}

}
