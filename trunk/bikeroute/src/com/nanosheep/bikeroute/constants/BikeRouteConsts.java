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
	/** Handler codes. **/
	/** Io/network error. **/
	public static final int IOERROR = 1;
	/** Argument exception. **/
	public static final int ARGERROR = 2;
	/** Empty result set. **/
	public static final int RES_ERROR = 3;
	
	/** Dialog ids. **/
	/** Unpark. **/
	public static final int UNPARK_DIALOG = 4;
	/** Awaiting GPS fix dialog. **/
	public static final int AWAITING_FIX = 5;
	/** Planning dialog id. **/
	public static final int PLAN = 6;
	/** Planning failed dialog id. **/
	public static final int PLAN_FAIL_DIALOG = 7;
	/** About dialog id. **/
	public static final int ABOUT = 8;
	
	
	/** Osmdroid consts. **/
	public static final int MENU_MY_LOCATION = Menu.FIRST;
    public static final int MENU_MAP_MODE = MENU_MY_LOCATION + 1;
    public static final int MENU_SAMPLES = MENU_MAP_MODE + 1;
    public static final int MENU_ABOUT = MENU_SAMPLES + 1;

    public static final String DEBUGTAG = "OPENSTREETMAP";

    public static final int NOT_SET = Integer.MIN_VALUE;
    
    public static final String PREFS_NAME = "com.nanosheep.bikeroute";
    public static final String PREFS_RENDERER = "renderer";
    public static final String PREFS_SCROLL_X = "scrollX";
    public static final String PREFS_SCROLL_Y = "scrollY";
    public static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    public static final String PREFS_SHOW_LOCATION = "showLocation";
    public static final String PREFS_FOLLOW_LOCATION = "followLocation";
    
    /** Text to speech check id. **/
    
    public static final int TTS_CHECK = 9;
    
    /** OSM stands api. **/
    /** API url. OpenStreetMap xapi interface. **/
	public static final String OSM_API =
		"http://xapi.openstreetmap.org/api/0.6/node[amenity=bicycle_parking]";
	
	/** Pi/180 for converting degrees - radians. **/
	public static final double PI_180 = Math.PI / 180;
	/** Radius of the earth for degrees - miles calculations. **/
	public static final double EARTH_RADIUS = 3960.0;
	public static final int RESULT_OK = 0;
	
	private BikeRouteConsts() { }
}
