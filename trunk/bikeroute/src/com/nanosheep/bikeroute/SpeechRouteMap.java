/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.Iterator;

import com.nanosheep.bikeroute.utility.Convert;
import com.nanosheep.bikeroute.utility.Segment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.MenuItem;

/**
 * Speaking route map.
 * 
 * @author jono@nanosheep.net
 * @version Oct 6, 2010
 */
public class SpeechRouteMap extends RouteMap implements OnInitListener {
	
	/** TTS enabled. **/
	protected boolean tts;
	/** TTS. **/
	protected TextToSpeech directionsTts;

	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		tts = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tts", false);
		//Initialize tts if in use.
        if (tts) {
        	directionsTts = new TextToSpeech(this, this);
        }
	}
	
	/**
	 * Handle option selection.
	 * @return true if option selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.turnbyturn) {
			speak(app.getSegment());
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void nextStep() {
		super.nextStep();
		speak(app.getSegment());
	}
	
	@Override
	public void hideStep() {
		super.hideStep();
		if (tts) {
			directionsTts.stop();
		}
	}
	
	/**
	 * Construct and speak a directions string for the segment.
	 * @param segment the segment to speak directions for.
	 */
	
	public void speak(final Segment segment) {
		if (tts) {
			Iterator<Segment> it = app.getRoute().getSegments().listIterator(
					app.getRoute().getSegments().indexOf(segment));
			StringBuffer sb = new StringBuffer(segment.getInstruction());
			if (it.hasNext()) {
				sb.append(" then after ");
				if (unit.equals("km")) {
					sb.append(segment.getLength());
					sb.append("meters");
				} else {
					sb.append(Convert.asFeet(segment.getLength()));
					sb.append("feet");
				}
				sb.append(it.next().getInstruction());
			}
			if (directionsTts.isSpeaking()) {
				sb.insert(0, " then");
			}	
			directionsTts.speak(sb.toString(), TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	/* (non-Javadoc)
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	@Override
	public void onInit(int arg0) {
		// TODO Auto-generated method stub
		
	}
}
