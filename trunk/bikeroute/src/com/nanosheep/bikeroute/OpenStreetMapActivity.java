package com.nanosheep.bikeroute;

import org.andnav.osm.DefaultResourceProxyImpl;
import org.andnav.osm.ResourceProxy;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.MyLocationOverlay;
import org.andnav.osm.views.util.OpenStreetMapRendererFactory;

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

        mPrefs = getSharedPreferences(getString(R.string.prefs_name), MODE_PRIVATE);

    }

    @Override
    protected void onPause() {
        final SharedPreferences.Editor edit = mPrefs.edit();
        edit.putInt(getString(R.string.prefs_scrollx), mOsmv.getScrollX());
        edit.putInt(getString(R.string.prefs_scrolly), mOsmv.getScrollY());
        edit.putInt(getString(R.string.prefs_zoomlevel), mOsmv.getZoomLevel());
        edit.putBoolean(getString(R.string.prefs_showlocation), mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(getString(R.string.prefs_followlocation), mLocationOverlay.isLocationFollowEnabled());
        edit.commit();

        this.mLocationOverlay.disableMyLocation();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOsmv.setRenderer(OpenStreetMapRendererFactory.CYCLEMAP);
        if(mPrefs.getBoolean(getString(R.string.prefs_showlocation), false)) {
                this.mLocationOverlay.enableMyLocation();
        }
        this.mLocationOverlay.followLocation(mPrefs.getBoolean(getString(R.string.prefs_followlocation), true));
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