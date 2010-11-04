/**
 * 
 */
package com.nanosheep.bikeroute.service;

import android.content.Context;

import com.nanosheep.bikeroute.utility.route.Route;

/**
 * Interface for classes wishing to listen for route plans.
 * 
 * @author jono@nanosheep.net
 * @version Oct 11, 2010
 */
public interface RouteListener {
	/**
	 * Called when a route search completes.
	 * @param msg Response code
	 * @param route Route computed or null
	 */
	public void searchComplete(Integer msg, Route route);
	/**
	 * Called when a search is cancelled.
	 */
	public void searchCancelled();
	/**
	 * @return the context of the listening activity.
	 */
	public Context getContext();
}
