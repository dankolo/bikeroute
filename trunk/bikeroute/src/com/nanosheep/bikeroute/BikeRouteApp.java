/**
 * 
 */
package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.utility.AddressDatabase;
import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;

import android.app.Application;

/**
 * This file is part of BikeRoute.
 * 
 * Copyright (C) 2011  Jonathan Gray
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
		segment = !route.getSegments().isEmpty() ? route.getSegments().get(0) : null;
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
