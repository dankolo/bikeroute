/**
 * 
 */
package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.utility.Stands;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * @author jono@nanosheep.net
 * @version Jul 5, 2010
 */
public class Preferences extends PreferenceActivity {

        @Override
        protected void onCreate(Bundle savedState) {
                super.onCreate(savedState);
                addPreferencesFromResource(R.xml.preferences);
        }
        
        @Override
    	public final boolean onPrepareOptionsMenu(final Menu menu) {
    		final MenuItem steps = menu.findItem(R.id.directions);

    		if (((BikeRouteApp)getApplication()).getRoute() != null) {
    			steps.setVisible(true);
    		} else {
    			steps.setVisible(false);
    		}
    		return super.onPrepareOptionsMenu(menu);
    	}
        
        /**
    	 * Create the options menu.
    	 * @return true if menu created.
    	 */

    	@Override
    	public final boolean onCreateOptionsMenu(final Menu menu) {
    		final MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.menu.options_menu, menu);
    		return true;
    	}
    	
    	/**
    	 * Handle option selection.
    	 * @return true if option selected.
    	 */
    	@Override
    	public boolean onOptionsItemSelected(final MenuItem item) {
    		Intent intent;
    		switch(item.getItemId()) {
    		case R.id.navigate:
    			intent = new Intent(this, Navigate.class);
    			startActivity(intent);
    			break;
    		case R.id.directions:
    			intent = new Intent(this, DirectionsView.class);
    			startActivity(intent);
    			break;
    		case R.id.map:
    			intent = new Intent(this, RouteMap.class);
    			startActivity(intent);
    			break;
    		}
    		return true;
    	}
}
