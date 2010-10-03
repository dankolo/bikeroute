package com.nanosheep.bikeroute;

import org.andnav.osm.DefaultResourceProxyImpl;
import org.andnav.osm.ResourceProxy;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.MyLocationOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererFactory;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;

/**
 * Based on osmdroid default map view activity.
 * 
 * Default map view activity.
 * @author Manuel Stahl
 *
 */
public class OpenStreetMapActivity extends Activity {

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
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        mPrefs = getSharedPreferences(BikeRouteConsts.PREFS_NAME, MODE_PRIVATE);

    }

    @Override
    protected void onPause() {
        final SharedPreferences.Editor edit = mPrefs.edit();
        edit.putInt(BikeRouteConsts.PREFS_SCROLL_X, mOsmv.getScrollX());
        edit.putInt(BikeRouteConsts.PREFS_SCROLL_Y, mOsmv.getScrollY());
        edit.putInt(BikeRouteConsts.PREFS_ZOOM_LEVEL, mOsmv.getZoomLevel());
        edit.putBoolean(BikeRouteConsts.PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(BikeRouteConsts.PREFS_FOLLOW_LOCATION, mLocationOverlay.isLocationFollowEnabled());
        edit.commit();

        this.mLocationOverlay.disableMyLocation();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOsmv.setRenderer(OpenStreetMapRendererFactory.CYCLEMAP);
        if(mPrefs.getBoolean(BikeRouteConsts.PREFS_SHOW_LOCATION, false)) {
                this.mLocationOverlay.enableMyLocation();
        }
        this.mLocationOverlay.followLocation(mPrefs.getBoolean(BikeRouteConsts.PREFS_FOLLOW_LOCATION, true));
    }


    @Override
    public boolean onTrackballEvent(final MotionEvent event) {
            return this.mOsmv.onTrackballEvent(event);
    }
    
    @Override
	public boolean onTouchEvent(final MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				this.mLocationOverlay.followLocation(false);
			}
	        return super.onTouchEvent(event);
	}
}