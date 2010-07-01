/**
 * 
 */
package com.nanosheep.bikeroute;

import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

/**
 * Detect gestures to control the onscreen directions display in a routemap.
 * 
 * Swipe right to advance, left to retreat. Tap to focus, double tap to close.
 * 
 * @author jono@nanosheep.net
 * @version Jun 29, 2010
 */
public class TurnByTurnGestureListener extends SimpleOnGestureListener implements
		OnGestureListener {
	
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private RouteMap map;
    
    public TurnByTurnGestureListener(RouteMap map) {
    	this.map = map;
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            // right to left swipe
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
               map.lastStep(); 
               return true;
            }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
            	map.nextStep();
            	return true;
            }
        } catch (Exception e) {
            // nothing
        }
        return false;
    }
    
    @Override
    public boolean onSingleTapConfirmed(MotionEvent evt) {
    	map.showStep();
    	return true;
    }
    
    @Override 
    public boolean onDoubleTap(MotionEvent evt) {
    	map.hideStep();
    	return true;
    }

}
