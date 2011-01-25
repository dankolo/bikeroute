package com.nanosheep.bikeroute.utility.route;

import java.io.IOException;
import java.net.URLEncoder;

import org.osmdroid.util.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;

import android.util.Log;

import com.nanosheep.bikeroute.constants.BikeRouteConsts;
import com.nanosheep.bikeroute.parser.BikeRouteParser;
import com.nanosheep.bikeroute.parser.CycleStreetsParser;
import com.nanosheep.bikeroute.parser.GoogleDirectionsParser;
import com.nanosheep.bikeroute.parser.GoogleElevationParser;
import com.nanosheep.bikeroute.parser.MapQuestParser;
import com.nanosheep.bikeroute.parser.Parser;
import com.nanosheep.bikeroute.utility.Convert;

import com.nanosheep.bikeroute.R;


/**
 * Plans routes and displays them as overlays on the provided mapview.
 * 
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
 * 
 * @author jono@nanosheep.net
 * @version Oct 3, 2010
 */
public class RouteManager {
	/** Owning activity. **/
	private final Context ctxt;
	/** Route planned switch. **/
	private boolean planned;
	/** Route. **/
	private Route route;
	/** Start point. **/
	private GeoPoint start;
	/** Destination point. **/
	private GeoPoint dest;
	/** Country the route is in. **/
	private String country;
	/** Geocoder. **/
	private final Geocoder geocoder;
	private int id;
	
	public RouteManager(final Context context) {
		super();
		ctxt = context;
		planned = false;
		geocoder = new Geocoder(ctxt);
	}
	
	public int showRoute(final String routeFile) {
		clearRoute();
		Parser parser = new BikeRouteParser(routeFile);
		route = parser.parse();
		if ((route == null) || RouteManager.this.route.getPoints().isEmpty()) {
			return R.id.plan_fail;
		}
		//Build KD tree for the route
		route.buildTree();
		return R.id.result_ok;
	}
	
	/**
	 * Plan a route between the points given and show it on the map. Displays an
	 * alert if the planning failed for some reason.
	 * Executes planning process in a separate thread, displays a progress
	 * dialog while planning.
	 */

	public int showRoute() {
		clearRoute();
		try {
			country = geocoder
					.getFromLocation(Convert.asDegrees(dest.getLatitudeE6()),
							Convert.asDegrees(dest.getLongitudeE6()), 1)
					.get(0).getCountryCode();
			route = plan(start, dest);
			route.setCountry(country);
			if (RouteManager.this.route.getPoints().isEmpty()) {
				throw new PlanException("Route is empty.");
			}
			//Build KD tree for the route
			route.buildTree();
		} catch (IOException e) {
			Log.e(e.getMessage(), "Planner");
			return R.id.plan_fail;
		}
		catch (PlanException e) {
			return R.id.network_error;
		}
		return R.id.result_ok;
	}

	public void setRouteId(int routeId) {
		id = routeId;
	}
	
	/**
	 * Plan a route from the start point to a destination.
	 * 
	 * @param start Start point.
	 * @param dest Destination.
	 * @return a list of segments for the route.
	 */

	private Route plan(final GeoPoint start, final GeoPoint dest) {
		Parser parser;
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctxt);
		String router = settings.getString("router", BikeRouteConsts.CS);
		
		if (BikeRouteConsts.CS.equals(router)) {
			String routeType = settings.getString("cyclestreetsJourneyPref", "balanced");
			final StringBuffer sBuf = new StringBuffer(ctxt.getString(R.string.cs_api));
			sBuf.append("start_lat=");
			sBuf.append(Convert.asDegrees(start.getLatitudeE6()));
			sBuf.append("&start_lng=");
			sBuf.append(Convert.asDegrees(start.getLongitudeE6()));
			sBuf.append("&dest_lat=");
			sBuf.append(Convert.asDegrees(dest.getLatitudeE6()));
			sBuf.append("&dest_lng=");
			sBuf.append(Convert.asDegrees(dest.getLongitudeE6()));
			sBuf.append("&plan=");
			sBuf.append(routeType);
			sBuf.append("&route_id=");
			sBuf.append(id);

			parser = new CycleStreetsParser(sBuf
				.toString());
		} else if (BikeRouteConsts.G.equals(router)) {
			final StringBuffer sBuf = new StringBuffer(ctxt.getString(R.string.us_api));
			sBuf.append("origin=");
			sBuf.append(Convert.asDegrees(start.getLatitudeE6()));
			sBuf.append(',');
			sBuf.append(Convert.asDegrees(start.getLongitudeE6()));
			sBuf.append("&destination=");
			sBuf.append(Convert.asDegrees(dest.getLatitudeE6()));
			sBuf.append(',');
			sBuf.append(Convert.asDegrees(dest.getLongitudeE6()));
			if ("US".equals(country)) {
				sBuf.append("&sensor=true&mode=bicycling");
			} else {
				sBuf.append("&sensor=true&mode=driving");
			}
		parser = new GoogleDirectionsParser(sBuf.toString());
		} else {
			final StringBuffer sBuf = new StringBuffer(ctxt.getString(R.string.mq_api));
			sBuf.append(start.getLatitudeE6()/1E6);
			sBuf.append(',');
			sBuf.append(start.getLongitudeE6()/1E6);
			sBuf.append("&to=");
			sBuf.append(dest.getLatitudeE6()/1E6);
			sBuf.append(',');
			sBuf.append(dest.getLongitudeE6()/1E6);
			sBuf.append("&generalize=0.1&shapeFormat=cmp");
			parser = new MapQuestParser(sBuf.toString());
		}
		Route r =  parser.parse();
		//Untidy.
		//If a polyline is set, need to query elevations api for
		//this route.
		if (r.getPolyline() != null) {
			final StringBuffer elev = new StringBuffer(ctxt.getString(R.string.elev_api));
			elev.append(URLEncoder.encode(r.getPolyline()));
			parser = new GoogleElevationParser(elev.toString(), r);
			r = parser.parse();
		}
		r.setCountry(country);
		r.setRouteId(id);
		return r;
	}
	
	/**
	 * Clear the current route.
	 */

	public void clearRoute() {
	//	routeOverlay = null;
		planned = false;
	}

	/**
	 * @param route the route to set
	 */
	public void setRoute(final Route route) {
		this.route = route;
		//routeOverlay = new RouteOverlay(route, Color.BLUE);
		planned = true;
	}

	/**
	 * @return the route
	 */
	public Route getRoute() {
		return route;
	}

	/**
	 * @param isPlanned the isPlanned to set
	 */
	public void setPlanned(final boolean isPlanned) {
		this.planned = isPlanned;
	}

	/**
	 * @return the isPlanned
	 */
	public boolean isPlanned() {
		return planned;
	}
	
	/**
	 * @return the starting geopoint
	 */
	
	public GeoPoint getStart() {
		return start;
	}
	
	/**
	 * @return the destination geopoint.
	 */
	
	public GeoPoint getDest() {
		return dest;
	}
	
	/**
	 * Set the start point for the route.
	 * @param start start point.
	 */
	
	public void setStart(final GeoPoint start) {
		this.start = start;
	}
	
	/**
	 * Set a start point which is an address.
	 * @param address
	 */
	
	public void setStart(final Address address) {
		GeoPoint p = new GeoPoint(Convert.asMicroDegrees(address.getLatitude()),
				Convert.asMicroDegrees(address.getLongitude()));
		setStart(p);
	}
	
	/**
	 * Set a destination point which is an address
	 * @param dest2
	 */
	
	public void setDest(final Address dest2) {
		GeoPoint p = new GeoPoint(Convert.asMicroDegrees(dest2.getLatitude()),
				Convert.asMicroDegrees(dest2.getLongitude()));
		setDest(p);
	}
	
	/**
	 * Set a start point which is a location
	 * @param location
	 */
	
	public void setStart(final Location location) {
		GeoPoint p = new GeoPoint(Convert.asMicroDegrees(location.getLatitude()),
				Convert.asMicroDegrees(location.getLongitude()));
		setStart(p);
	}
	
	/**
	 * Set a destination which is a location.
	 * @param location
	 */
	
	public void setDest(final Location location) {
		GeoPoint p = new GeoPoint(Convert.asMicroDegrees(location.getLatitude()),
				Convert.asMicroDegrees(location.getLongitude()));
		setDest(p);
	}
	
	/**
	 * Set the destination point for the route.
	 * @param end point.
	 */
	
	public void setDest(final GeoPoint end) {
		this.dest = end;
	}
	
	public void setDest(final String name) throws IOException {
		Address address = geocoder.getFromLocationName(
				name, 1).get(0);
		setDest(address);
	}
	
	public void setStart(final String name) throws IOException {
		Address address = geocoder.getFromLocationName(
				name, 1).get(0);
		setStart(address);
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	
	/**
	 * Exception type for route planning exceptions.
	 * 
	 * @author jono@nanosheep.net
	 * @version Jun 27, 2010
	 */
	private class PlanException extends Exception {

		/**
		 * Empty planexception.
		 */
		public PlanException() {
		}

		/**
		 * PlanException with specified message.
		 * @param detailMessage
		 */
		public PlanException(String detailMessage) {
			super(detailMessage);
		}

	}

}
