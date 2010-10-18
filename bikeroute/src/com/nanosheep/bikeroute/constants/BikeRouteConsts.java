/**
 * 
 */
package com.nanosheep.bikeroute.constants;

import android.view.Menu;

/**
 * @author jono@nanosheep.net
 * @version Sep 27, 2010
 */
public final class BikeRouteConsts {
	
	
	/** Osmdroid consts. **/
	public static final int MENU_MY_LOCATION = Menu.FIRST;
    public static final int MENU_MAP_MODE = MENU_MY_LOCATION + 1;
    public static final int MENU_SAMPLES = MENU_MAP_MODE + 1;
    public static final int MENU_ABOUT = MENU_SAMPLES + 1;

    public static final int NOT_SET = Integer.MIN_VALUE;
	
	/** Pi/180 for converting degrees - radians. **/
	public static final double PI_180 = Math.PI / 180;
	/** Radius of the earth for degrees - miles calculations. **/
	public static final double EARTH_RADIUS = 3960.0;
	
	private BikeRouteConsts() { }
}
