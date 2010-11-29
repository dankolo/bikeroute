/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.ListIterator;

import org.andnav.osm.util.GeoPoint;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nanosheep.bikeroute.service.NavigationService;
import com.nanosheep.bikeroute.service.RouteListener;
import com.nanosheep.bikeroute.service.RoutePlannerTask;
import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;
import com.nanosheep.bikeroute.R;

/**
 * Extends RouteMap providing live/satnav features - turn guidance advancing with location,
 * route replanning.
 * 
 * @author jono@nanosheep.net
 * @version Oct 4, 2010
 */
public class LiveRouteMap extends SpeechRouteMap implements RouteListener {
	/** Intent for replanning searches. **/
	protected Intent searchIntent;
	/** Planning dialog tracker. **/
	protected boolean mShownDialog;
	
	/** Search task for replanning. **/
	private RoutePlannerTask search;
	
	/** Live navigation toggle. **/
	private boolean liveNavigation;
	
	/** Last segment visited. **/
	private Segment lastSegment;
	/** Spoken for this segment. **/
	private boolean spoken;
	/** Arrived at destination. **/
	private boolean arrived;
	/** Navigation service. **/
	private NavigationService mBoundService;
	/** Receiver for navigation updates. **/
	private NavigationReceiver mBroadcastReceiver = new NavigationReceiver();
	/** Connection to navigation service. **/
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        mBoundService = ((NavigationService.LocalBinder)service).getService();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        mBoundService = null;
	    }
	};
	/** Are we bound to navigation service? **/
	private boolean mIsBound;
	
	@Override
	public void onCreate(final Bundle savedState) {
		super.onCreate(savedState);
		//Handle rotations
		final Object[] data = (Object[]) getLastNonConfigurationInstance();
		arrived = false;
		if (data != null) {
			isSearching = (Boolean) data[2];
			search = (RoutePlannerTask) data[3];
			if(search != null) {
				search.setListener(this);
			}
			spoken = (Boolean) data[4];
			arrived = (Boolean) data[1];
		}
		registerReceiver(mBroadcastReceiver, 
				new IntentFilter(getString(R.string.navigation_intent)));
	}
	
	/**
	 * Retain any state if the screen is rotated.
	 */
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[5];
		objs[0] = directionsVisible;
		objs[1] = arrived;
		objs[2] = isSearching;
		objs[3] = search;
		objs[4] = spoken;
	    return objs;
	}
	
	@Override
	public final boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem replan = menu.findItem(R.id.replan);
		final MenuItem stopService = menu.findItem(R.id.stop_nav);
		if (app.getRoute() != null) {
			replan.setVisible(true);
		}
		if (mIsBound) {
			stopService.setVisible(true);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public void showStep() {
		super.showStep();
		if(mSettings.getBoolean("gps", false)) {
			mLocationOverlay.followLocation(true);
		} else {
			mLocationOverlay.followLocation(false);
		}
	}
	
	@Override
	public void hideStep() {
		super.hideStep();
		mLocationOverlay.followLocation(false);
	}
	
	/**
	 * Fire a replanning request (current location -> last point of route.)
	 * to the routing service, display a dialog while in progress.
	 */
	
	private void replan() {
		isSearching = true;
		try {
			dismissDialog(R.id.plan_fail);
		} catch (Exception e) {
			Log.e("Replanner", "Fail dialog not shown!");
		}
		showDialog(R.id.plan);

		Location self = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (self == null) {
			self = mLocationOverlay.getLastFix();
		}
		if (self == null) {
			self = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		if (self != null) {
			searchIntent = new Intent();
			searchIntent.putExtra(RoutePlannerTask.ROUTE_ID, app.getRoute().getRouteId());
			searchIntent.putExtra(RoutePlannerTask.PLAN_TYPE, RoutePlannerTask.REPLAN_PLAN);
			searchIntent.putExtra(RoutePlannerTask.START_LOCATION, self);
			searchIntent.putExtra(RoutePlannerTask.END_POINT,
					app.getRoute().getPoints().get(app.getRoute().getPoints().size() - 1));
			LiveRouteMap.this.search = new RoutePlannerTask(LiveRouteMap.this, searchIntent);
			LiveRouteMap.this.search.execute();
		} else {
			dismissDialog(R.id.plan);
			showDialog(R.id.plan_fail);
		}
			
	}
	

	@Override
	public Dialog onCreateDialog(final int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch(id) {
		case R.id.gps:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.gps_msg);
			builder.setCancelable(false);
			builder.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int id) {
						showGpsOptions();
					}
				});
			builder.setTitle(R.string.gps_msg_title);
			dialog = builder.create();
			break;
		case R.id.plan:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setCancelable(true);
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setMessage(getText(R.string.plan_msg));
			pDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(final DialogInterface arg0) {
					removeDialog(R.id.plan);
				}
				});
			pDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(final DialogInterface arg0) {
					if (search != null) {
						search.cancel(true);
					}
					isSearching = false;
				}
			
			});
			dialog = pDialog;
			break;
		case R.id.plan_fail:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.planfail_msg);
			builder.setCancelable(
				true).setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int id) {
					}
				});
			dialog = builder.create();
			break;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	/**
	 * Handle option selection.
	 * @return true if option selected.
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.replan:
			if (!mIsBound) {
				doBindService();
			}
			replan();
			break;
		case R.id.stop_nav:
			doUnbindService();
			finishActivity(R.id.trace);
			setResult(1);
			this.finish();
			app.setRoute(null);
			break;
		case R.id.turnbyturn:
			spoken = true;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * Bind to navigation service.
	 */
	
	private void doBindService() {
	    bindService(new Intent(LiveRouteMap.this, 
	            NavigationService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	/**
	 * Unbind from navigation service.
	 */
	
	private void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}

	/**
	 * Unregister navigation receiver and unbind from service.
	 */
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    doUnbindService();
	    unregisterReceiver(mBroadcastReceiver);
	}
	
	/**
	 * Update settings for gps, bind nav service if appropriate
	 * and speak segment if osd enabled.
	 */
	
	@Override
	public void onStart() {
		super.onStart();
		liveNavigation = mSettings.getBoolean("gps", false);
		if (app.getRoute() != null) {
			//Disable live navigation for non GB routes to comply with Google tos
			liveNavigation = !"GB".equals(app.getRoute().getCountry()) ? false : liveNavigation;
			if (tts && directionsVisible && !isSearching) {
				speak(app.getSegment());
				lastSegment = app.getSegment();
			}
			if (liveNavigation) {
				if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					showDialog(R.id.gps);
				}
				doBindService();
			} else {
				doUnbindService();
			}
		}
	}
	
	
	/**
	 * Receiver for updates from the live navigation service.
	 * @author jono@nanosheep.net
	 * @version Nov 4, 2010
	 */
	
	private class NavigationReceiver extends BroadcastReceiver {

		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			if (liveNavigation && directionsVisible && !arrived && !isSearching) {
				if (intent.getBooleanExtra(getString(R.string.replan), false)) {
					isSearching = true;
					replan();
				} else if (intent.getBooleanExtra(getString(R.string.arrived), false)) {
					arrive();
					spoken = true;
				} else {
					GeoPoint current = (GeoPoint) intent.getExtras().get(getString(R.string.point));
					if (!app.getSegment().equals(lastSegment)) {
						lastSegment = app.getSegment();
						spoken = false;
					}
					
					//Get next point
					ListIterator<GeoPoint> it = app.getRoute().getPoints().listIterator(
							app.getRoute().getPoints().indexOf(current) + 1);
					GeoPoint next = it.hasNext() ? it.next() : current;
					
					//Speak directions if the next point is a new segment
					//and have not spoken already
					if (!spoken && !app.getSegment().equals(app.getRoute().getSegment(next)) && tts) {
							speak(app.getRoute().getSegment(next));
							spoken = true;
					}
					showStep();
					traverse(current);
				}
			}
		}
	}
	
	/**
   	 * Finish cascade passer.
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if ((requestCode == R.id.trace) && (resultCode == 1)) {
        	setResult(1);
        	finish();
        }
    }

	/* (non-Javadoc)
	 * @see com.nanosheep.bikeroute.service.RouteListener#searchComplete(java.lang.Integer, com.nanosheep.bikeroute.utility.Route)
	 */
	@Override
	public void searchComplete(Integer msg, Route route) {
		try {
			dismissDialog(R.id.plan);
		} catch (Exception e)  {
			
		}
		
		isSearching = false;
		if (msg != null) {
			
			if (msg == R.id.result_ok) {
				app.setRoute(route);
				app.setSegment(app.getRoute().getSegments().get(0));
				mOsmv.getController().setCenter(app.getSegment().startPoint());
				traverse(app.getSegment().startPoint());
				arrived = false;
				if (directionsVisible) {
					showStep();
					if (tts) {
						speak(app.getSegment());
						spoken = true;
					}
				}
			} else {
				showDialog(msg);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.nanosheep.bikeroute.service.RouteListener#searchCancelled()
	 */
	@Override
	public void searchCancelled() {
		isSearching = false;
		search = null;
	}

	/* (non-Javadoc)
	 * @see com.nanosheep.bikeroute.service.RouteListener#getContext()
	 */
	@Override
	public Context getContext() {
		return this;
	}
	
	/**
	 * Arrive at a destination.
	 */
	
	private void arrive() {
		doUnbindService();
		arrived = true;
		app.setSegment(app.getRoute().getSegment(app.getRoute().getEndPoint()));
		traverse(app.getRoute().getEndPoint());
		showStep();
		if (tts) {
			directionsTts.speak(getString(R.string.arrived_speech), TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	
	/** Show GPS options if GPS provider is disabled.
	 * 
	 */
	
	private void showGpsOptions() { 
        startActivity(new Intent(  
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)); 
	}
}
