/**
 * 
 */
package com.nanosheep.bikeroute.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.andnav.osm.util.GeoPoint;

import com.nanosheep.bikeroute.BikeRouteApp;
import com.nanosheep.bikeroute.LiveRouteMap;
import com.nanosheep.bikeroute.constants.BikeRouteConsts;

import com.nanosheep.bikeroute.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * Service providing live navigation using GPS and notification updates
 * reflecting current location on route.
 * 
 * @author jono@nanosheep.net
 * @version Nov 4, 2010
 */
public class NavigationService extends Service implements LocationListener{
	/** Local binder. **/
    private final IBinder mBinder = new LocalBinder();
    /** Notification manager. **/
	private NotificationManager mNM;
	/** Location manager. **/
	private LocationManager mLocationManager;
	/** Custom application reference. **/
	private BikeRouteApp app;
	/** Notification for notifying. **/
	private Notification notification;
	/** Intent for callbacks from notifier. **/
	private PendingIntent contentIntent;
	
	 /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public NavigationService getService() {
            return NavigationService.this;
        }
    }

	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	@Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        /* Get location manager. */
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        app = (BikeRouteApp) getApplication();
        int icon = R.drawable.bikeroute;
        CharSequence tickerText = "";
        long when = System.currentTimeMillis();

        notification = new Notification(icon, tickerText, when);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        Intent notificationIntent = new Intent(this, LiveRouteMap.class);
        notificationIntent.putExtra(getString(R.string.jump_intent), true);
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        if (app.getRoute() != null) {
        	notification.setLatestEventInfo(app, getText(R.string.notify_title),
				app.getSegment().getInstruction(), contentIntent);
        	mNM.notify(R.id.notifier, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	mLocationManager.removeUpdates(this);
    	mNM.cancelAll();
    }
    
    /**
     * Listen for location updates and pass an update intent back to the
     * routemap, then update the status bar notification with current
     * instruction.
     */
    
    @Override
	public void onLocationChanged(Location location) {
    	if (app.getRoute() != null) {
    		Intent update = new Intent((String) getText(R.string.navigation_intent));
        	//Find the nearest points and unless it is far, assume we're there
    		GeoPoint self = new GeoPoint(location);
    		List<GeoPoint> near = app.getRoute().nearest(self, 2);
    		
    		double range = range(self, near.get(0), near.get(1)) - 50;
    		double accuracy = location.getAccuracy();
    		
    		//Check we're still on the route.
    		
    		if (range > accuracy) {
    			update.putExtra((String) getText(R.string.replan), true);
    			String logMsg = "Range=" + range + ",Self="+self+",near points:" + near + ",near points dist=" + near.get(0).distanceTo(near.get(1)) + ",accuracy=" + accuracy;
    			Log.e("Replanned", logMsg);
    			notification.setLatestEventInfo(app, getText(R.string.notify_title), 
    					getText(R.string.replanning), contentIntent);
    			boolean mExternalStorageAvailable = false;
    	        boolean mExternalStorageWriteable = false;
    	        String state = Environment.getExternalStorageState();

    	        if (Environment.MEDIA_MOUNTED.equals(state)) {
    	            // We can read and write the media
    	            mExternalStorageAvailable = mExternalStorageWriteable = true;
    	            
    	            File f = Environment.getExternalStorageDirectory ();
    	            File o = new File(f, "/Android/data/nanosheep_log.txt");
    	            try {
						FileOutputStream fs = new FileOutputStream(o, true);
						// Print a line of text
					    new PrintStream(fs).println (logMsg);

					    // Close our output stream
					    fs.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    	            
    	            
    	        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	            // We can only read the media
    	            mExternalStorageAvailable = true;
    	            mExternalStorageWriteable = false;
    	        } else {
    	            // Something else is wrong. It may be one of many other states, but all we need
    	            //  to know is we can neither read nor write
    	            mExternalStorageAvailable = mExternalStorageWriteable = false;
    	        }
    	        
    		} else if (near.get(0).equals(app.getRoute().getEndPoint())) { //If we've arrived, shutdown and signal.
    			update.putExtra((String) getText(R.string.arrived), true);
    			notification.setLatestEventInfo(app, getText(R.string.notify_title), 
    					getText(R.string.arrived), contentIntent);
    			stopSelf();
    		}	else {
    			update.putExtra((String) getText(R.string.point), near.get(0));
    			app.setSegment(app.getRoute().getSegment(near.get(0)));
    			notification.setLatestEventInfo(app, getText(R.string.notify_title),
        				app.getSegment().getInstruction(), contentIntent);
    		}
    		sendBroadcast(update);
    		
    		mNM.notify(R.id.notifier, notification);
    	}
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Get the cross track error of p0 from the path p1 -> p2
	 * @param p0 point to get distance to.
	 * @param p1 start point of line.
	 * @param p2 end point of line.
	 * @return the distance from p0 to the path in meters as a double.
	 */
	
	private double range(final GeoPoint p0, final GeoPoint p1, final GeoPoint p2) {
		double dist = Math.asin(Math.sin(p1.distanceTo(p0)/BikeRouteConsts.EARTH_RADIUS) * 
				Math.sin(p1.bearingTo(p0) - p1.bearingTo(p2))) * 
				BikeRouteConsts.EARTH_RADIUS;
		
		return Math.abs(dist);
	}

}
