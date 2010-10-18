/**
 * 
 */
package com.nanosheep.bikeroute.service;

import android.app.Activity;
import android.content.Context;

import com.nanosheep.bikeroute.utility.route.Route;

/**
 * @author jono@nanosheep.net
 * @version Oct 11, 2010
 */
public interface RouteListener {
	public void searchComplete(Integer msg, Route route);
	public void searchCancelled();
	public Context getContext();
}
