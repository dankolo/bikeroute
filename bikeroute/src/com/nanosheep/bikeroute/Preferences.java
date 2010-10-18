/**
 * 
 */
package com.nanosheep.bikeroute;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
        private Preference tts;

		@Override
        protected void onCreate(final Bundle savedState) {
                super.onCreate(savedState);
                
                //Check for TTS
                Intent checkIntent = new Intent();
				checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
				startActivityForResult(checkIntent, R.id.tts_check);
				
                addPreferencesFromResource(R.xml.preferences);
                tts = findPreference("tts");
        }
        
        @Override
    	public final boolean onPrepareOptionsMenu(final Menu menu) {
    		final MenuItem steps = menu.findItem(R.id.directions);

    		steps.setVisible(false);
    		if (((BikeRouteApp)getApplication()).getRoute() != null) {
    			steps.setVisible(true);
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
    			intent = new Intent(this, LiveRouteMap.class);
    			startActivity(intent);
    			break;
    		case R.id.about:
    			showDialog(R.id.about);
    			break;
    		}
    		return true;
    	}
    	
    	@Override
		protected void onActivityResult(
		        final int requestCode, final int resultCode, final Intent data) {
		    if (requestCode == R.id.tts_check) {
		        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
		        	tts.setEnabled(true);
		        } else {
		            final Intent installIntent = new Intent();
		            installIntent.setAction(
		                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
		            startActivity(installIntent);
		        }
		    }
		}
    	
    	/**
    	 * Creates dialogs for loading, on errors, alerts.
    	 * Available dialogs:
    	 * Planning progress, planning error.
    	 * @return the approriate Dialog object
    	 */
    	
    	@Override
		public Dialog onCreateDialog(final int id) {
    		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getText(R.string.about_message)).setCancelable(
					true).setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.dismiss();
						}
					});
			return builder.create();
    	}
}
