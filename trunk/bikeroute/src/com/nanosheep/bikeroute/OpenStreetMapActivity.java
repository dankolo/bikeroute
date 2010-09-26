// Created by plusminus on 00:23:14 - 03.10.2008
package com.nanosheep.bikeroute;

import org.andnav.osm.ResourceProxy;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.MyLocationOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererFactory;

import com.nanosheep.bikeroute.utility.BikeRouteResourceProxy;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;

/**
 * Default map view activity.
 * @author Manuel Stahl
 *
 */
public class OpenStreetMapActivity extends Activity {
        // ===========================================================
        // Constants
        // ===========================================================

        private static final int MENU_MY_LOCATION = Menu.FIRST;
        private static final int MENU_MAP_MODE = MENU_MY_LOCATION + 1;
        private static final int MENU_SAMPLES = MENU_MAP_MODE + 1;
        private static final int MENU_ABOUT = MENU_SAMPLES + 1;

        private static final int DIALOG_ABOUT_ID = 1;
        public static final String DEBUGTAG = "OPENSTREETMAP";

        public static final int NOT_SET = Integer.MIN_VALUE;
        
        public static final String PREFS_NAME = "com.nanosheep.bikeroute";
        public static final String PREFS_RENDERER = "renderer";
        public static final String PREFS_SCROLL_X = "scrollX";
        public static final String PREFS_SCROLL_Y = "scrollY";
        public static final String PREFS_ZOOM_LEVEL = "zoomLevel";
        public static final String PREFS_SHOW_LOCATION = "showLocation";
        public static final String PREFS_FOLLOW_LOCATION = "followLocation";

        // ===========================================================
        // Fields
        // ===========================================================

        protected SharedPreferences mPrefs;
        protected OpenStreetMapView mOsmv;
        protected MyLocationOverlay mLocationOverlay;
        protected ResourceProxy mResourceProxy;

        // ===========================================================
        // Constructors
        // ===========================================================
            
        
        /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResourceProxy = new BikeRouteResourceProxy(getApplicationContext());

        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor edit = mPrefs.edit();
        edit.putInt(PREFS_SCROLL_X, mOsmv.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mOsmv.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mOsmv.getZoomLevel());
        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(PREFS_FOLLOW_LOCATION, mLocationOverlay.isLocationFollowEnabled());
        edit.commit();

        this.mLocationOverlay.disableMyLocation();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOsmv.setRenderer(OpenStreetMapRendererFactory.CYCLEMAP);
        if(mPrefs.getBoolean(PREFS_SHOW_LOCATION, false))
                this.mLocationOverlay.enableMyLocation();
        this.mLocationOverlay.followLocation(mPrefs.getBoolean(PREFS_FOLLOW_LOCATION, true));
    }


    @Override
    public boolean onTrackballEvent(MotionEvent event) {
            return this.mOsmv.onTrackballEvent(event);
    }
    
    @Override
	public boolean onTouchEvent(final MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE)
				this.mLocationOverlay.followLocation(false);

	        return super.onTouchEvent(event);
	}

        // ===========================================================
        // Methods
        // ===========================================================

        // ===========================================================
        // Inner and Anonymous Classes
        // ===========================================================
}