/**
 * 
 */
package com.nanosheep.bikeroute.service;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.utility.Parking;
import com.nanosheep.bikeroute.utility.RouteManager;
import com.nanosheep.bikeroute.utility.Stands;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;

/**
 * @author jono@nanosheep.net
 * @version Oct 3, 2010
 */
public class RoutePlannerService extends IntentService {

	/** Route planner service consts. **/
	/** Request name string. **/
	public static final String PLAN_TYPE = "plan_type";
	/** Bike plan. **/
	public static final int BIKE_PLAN = 0;
	/** GeoPoint plan. **/
	public static final int GEOPOINT_PLAN = 1;
	/** Replanning request. **/
	public static final int REPLAN_PLAN = 2;
	/** Stand plan. **/
	public static final int STANDS_PLAN = 3;
	/** Address plan. **/
	public static final int ADDRESS_PLAN = 4;
	public static final String START_ADDRESS = "start_address";
	public static final String END_ADDRESS = "end_address";
	private static final String START_LOCATION = "start_location";
	private static final String LOCATION = "location package";
	private static final String END_LOCATION = "end_location";
	public static final String INTENT_ID = "com.nanosheep.bikeroute.service.RoutePlannerService";
	
	/**
	 * @param name
	 */
	public RoutePlannerService() {
		super("Route Planner Service.");
	}

	/* (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		int msg = BikeRouteConsts.PLAN_FAIL_DIALOG; 
		RouteManager planner = new RouteManager(this);
		final String startAddressInput = intent.getStringExtra(START_ADDRESS);
		final String endAddressInput = intent.getStringExtra(END_ADDRESS);
		final Intent resultIntent = new Intent(INTENT_ID);
		
		switch(intent.getIntExtra(PLAN_TYPE, ADDRESS_PLAN)) {
		case ADDRESS_PLAN:
			if ("".equals(startAddressInput) || "".equals(endAddressInput)) {
				msg = BikeRouteConsts.ARGERROR;
			} else {
				msg = BikeRouteConsts.RESULT_OK;
				try {
					planner.setStart(startAddressInput);
					planner.setDest(endAddressInput);		
				} catch (Exception e) {
					msg = BikeRouteConsts.IOERROR;
				}
			}
			break;
		case BIKE_PLAN:
			final Parking prk = new Parking(this);
			if ("".equals(startAddressInput)) {
				msg = BikeRouteConsts.ARGERROR;
			} else {
				try {
					planner.setStart(startAddressInput);
					planner.setDest(prk.getLocation());	
				} catch (Exception e) {
					msg = BikeRouteConsts.IOERROR;
				}
			}
			break;
		case STANDS_PLAN:
			if ("".equals(startAddressInput)) {
				msg = BikeRouteConsts.ARGERROR;
			} else {
				msg = BikeRouteConsts.RESULT_OK;
				try {
					planner.setStart(startAddressInput);
					planner.setDest(Stands.getNearest(planner.getStart()));	
				} catch (Exception e) {
					msg = BikeRouteConsts.IOERROR;
				}
			}
			break;
		case REPLAN_PLAN:
			final Location start = intent.getBundleExtra(LOCATION).getParcelable(START_LOCATION);
			final Location dest = intent.getBundleExtra(LOCATION).getParcelable(END_LOCATION);
			msg = BikeRouteConsts.RESULT_OK;
			planner.setStart(start);
			planner.setDest(dest);	
			break;
		default:
			msg = BikeRouteConsts.PLAN_FAIL_DIALOG;
		}
		try {
			if ((msg == BikeRouteConsts.RESULT_OK) && !planner.showRoute()) {
				msg = BikeRouteConsts.PLAN_FAIL_DIALOG;
			}
			resultIntent.putExtra("route", planner.getRoute());
		} catch (Exception e) {
			msg = BikeRouteConsts.IOERROR;
		} finally {
			resultIntent.putExtra("msg", msg);
			sendBroadcast(resultIntent);
		}
		
	}

}
