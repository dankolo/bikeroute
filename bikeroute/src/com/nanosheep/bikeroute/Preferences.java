/**
 * 
 */
package com.nanosheep.bikeroute;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * @author jono@nanosheep.net
 * @version Jul 5, 2010
 */
public class Preferences extends PreferenceActivity {

        protected static final int TTS_CHECK = 0;
        private Preference tts;

		@Override
        protected void onCreate(Bundle savedState) {
                super.onCreate(savedState);
                
                //Check for TTS
                Intent checkIntent = new Intent();
				checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
				startActivityForResult(checkIntent, TTS_CHECK);
				
                addPreferencesFromResource(R.xml.preferences);
                tts = (Preference) findPreference("tts");
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
    	
    	protected void onActivityResult(
		        int requestCode, int resultCode, Intent data) {
		    if (requestCode == TTS_CHECK) {
		        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
		        	tts.setEnabled(true);
		        } else {
		            Intent installIntent = new Intent();
		            installIntent.setAction(
		                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
		            startActivity(installIntent);
		        }
		    }
		}
}
