/**
 * 
 */
package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;

import android.app.Application;

/**
 * @author jono@nanosheep.net
 * @version Jul 2, 2010
 */
public class BikeRouteApp extends Application {
	/** Route object. **/
	private Route route;
	/** The current segment. **/
	private Segment segment;
	/** Previous addresses db. **/
	private AddressDatabase db;

	public BikeRouteApp () {
		super();
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
		segment = route.getSegments().get(0);
	}

	/**
	 * @return the route
	 */
	public Route getRoute() {
		return route;
	}

	/**
	 * @param segment the segment to set.
	 */
	public void setSegment(final Segment segment) {
		this.segment = segment;
	}

	/**
	 * @return the current segment
	 */
	public Segment getSegment() {
		return segment;
	}

	/**
	 * @return the db
	 */
	public AddressDatabase getDb() {
		return db;
	}
}
