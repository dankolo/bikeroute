/**
 * 
 */
package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.Route;

import android.app.Application;

/**
 * @author jono@nanosheep.net
 * @version Jul 2, 2010
 */
public class BikeRouteApp extends Application {
	/** Route object. **/
	private Route route;
	/** The current segment. **/
	private int segId;
	/** Previous addresses db. **/
	private AddressDatabase db;

	public BikeRouteApp () {
		super();
		segId = 0;
	}
	
	@Override
	public void onCreate() {
		db = new AddressDatabase(this);
	}
	
	/**
	 * @param route the route to set
	 */
	public void setRoute(final Route route) {
		this.route = route;
		this.segId = 0;
	}

	/**
	 * @return the route
	 */
	public Route getRoute() {
		return route;
	}

	/**
	 * @param segId the segId to set
	 */
	public void setSegId(final int segId) {
		this.segId = segId;
	}

	/**
	 * @return the segId
	 */
	public int getSegId() {
		return segId;
	}

	/**
	 * @return the db
	 */
	public AddressDatabase getDb() {
		return db;
	}

}
