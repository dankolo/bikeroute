/**
 * 
 */
package com.nanosheep.bikeroute.utility;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

import com.nanosheep.bikeroute.utility.route.PGeoPoint;
import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;

/**
 * SQLite database helper class for storing and retrieving
 * favourite routes.
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
 * @version Jan 8, 2011
 */
public class RouteDatabase {
		private static final int DATABASE_VERSION = 3;
		private static final String ROUTE_TABLE_NAME = "route";
		private static final String NAME = "name_string";
		private static final String CRIGHT = "copyright_string";
		private static final String WARN = "warning_string";
		private static final String CC = "country_code_string";
		private static final String POLY = "polyline_string";
		private static final String ITIN = "itenerary_id";
		
		
		private static final String POINTS_TABLE_NAME = "points";
		private static final String LAT = "latitude_e6_int";
		private static final String LNG = "longitute_e6_int";
		private static final String SEG_ID = "segid";
		
		private static final String SEGMENT_TABLE_NAME = "segments";
		private static final String INSTRUCTION = "turn_string";
		private static final String ROUTE_ID = "routeid";
		private static final String LENGTH = "length_int";
		private static final String DIST = "distance_dbl";
		
		private static final String ELEVATION_TABLE_NAME = "elevations";
		private static final String ELEV = "elevation_int";
		private static final String DIST_M = "distance_int";
		
		private static final String ROUTE_TABLE_CREATE =
               "CREATE TABLE " + ROUTE_TABLE_NAME + " (id INTEGER PRIMARY KEY, " + NAME + " TEXT, " +
               CRIGHT + " TEXT, " + WARN + " TEXT, " + CC + " TEXT, " + POLY + " TEXT, " + ITIN + " INTEGER, "
               + LENGTH + " INTEGER" + ");";
		
		private static final String POINTS_TABLE_CREATE =
			"CREATE TABLE " + POINTS_TABLE_NAME + " (" + SEG_ID + " INTEGER , " + LAT + 
			" INTEGER, " + LNG + " INTEGER);";
		
		private static final String SEGMENT_TABLE_CREATE = 
			"CREATE TABLE " + SEGMENT_TABLE_NAME + " (id INTEGER PRIMARY KEY, " + NAME + " TEXT, " + INSTRUCTION + " TEXT, " +
					ROUTE_ID + "INTEGER, " + DIST + " DOUBLE, " + LENGTH + " INTEGER);";
		
		private static final String ELEVATION_TABLE_CREATE = 
			"CREATE TABLE " + ELEVATION_TABLE_NAME + " (id INTEGER PRIMARY KEY, " + ELEV + " INTEGER, " + DIST_M +
			" INTEGER, " + ROUTE_ID + " INTEGER);";
		
		
		private static final String DATABASE_NAME = "bikeroute_db";

	   private Context context;
	   private SQLiteDatabase db;

	   private List<SQLiteStatement> insertStmt;
	   private static final String INSERT_ROUTE = "insert into " 
	      + ROUTE_TABLE_NAME + " values (NULL, ?, ?, ?, ?, ?, ?, ?);";
	   private static final String INSERT_SEGMENT = "insert into "
		   + SEGMENT_TABLE_NAME + " values (NULL, ?, ?, ?, ?, ?);";
	   private static final String INSERT_POINT = "insert into "
		   + POINTS_TABLE_NAME + " values (?, ?, ?);";
	   private static final String INSERT_ELEV = "insert into "
		   + ELEVATION_TABLE_NAME + " values(null, ?, ?, ?);";
	   
	   private static final String LIKE_QUERY = NAME + " LIKE ";

	   public RouteDatabase(Context context) {
	      this.context = context;
	      AddressDatabaseHelper openHelper = new AddressDatabaseHelper(this.context);
	      this.db = openHelper.getWritableDatabase();
	      this.insertStmt = new ArrayList<SQLiteStatement>(4);
	      this.insertStmt.add(this.db.compileStatement(INSERT_ROUTE));
	      this.insertStmt.add(this.db.compileStatement(INSERT_SEGMENT));
	      this.insertStmt.add(this.db.compileStatement(INSERT_POINT));
	      this.insertStmt.add(this.db.compileStatement(INSERT_ELEV));
	   }
	   
	   /**
	    * Add route to the database.
	    * @param route the route to store.
	    * @return
	    */

	   public void insert(Route route) {
		   db.beginTransaction();
		   try {
			   //Insert the route and get the id back
			   this.insertStmt.get(0).bindString(1, route.getName()); 
			   this.insertStmt.get(0).bindString(2, route.getCopyright());
			   this.insertStmt.get(0).bindString(3, route.getWarning());
			   this.insertStmt.get(0).bindString(4, route.getCountry());
			   this.insertStmt.get(0).bindString(5, route.getPolyline()); 
			   this.insertStmt.get(0).bindLong(6, route.getItineraryId()); 
			   this.insertStmt.get(0).bindLong(7, route.getLength()); 
			   final long routeId = this.insertStmt.get(0).executeInsert();
		   
			   //Insert elevation set
			   for(int i = 0; i < route.getElevationSeries().getItemCount(); i++) {
				   this.insertStmt.get(3).clearBindings();
				   //Elevation
				   this.insertStmt.get(3).bindDouble(1, route.getElevationSeries().getY(i));
				   //Distance
				   this.insertStmt.get(3).bindDouble(2, route.getElevationSeries().getX(i));
				   this.insertStmt.get(3).bindLong(3, routeId);
				   this.insertStmt.get(3).executeInsert();
			   }
		   
			   //Insert each segment with a ref to the route
			   for(Segment s : route.getSegments()) {
				   this.insertStmt.get(1).clearBindings();
				   this.insertStmt.get(1).bindString(1, s.getName());
				   this.insertStmt.get(1).bindString(2, s.getInstruction());
				   this.insertStmt.get(1).bindLong(3, routeId);
				   this.insertStmt.get(1).bindDouble(4, s.getDistance());
				   this.insertStmt.get(1).bindLong(5, s.getLength());
				   final long segId = this.insertStmt.get(1).executeInsert();
				   //And each point with a ref to the segment
				   for (PGeoPoint p : s.getPoints()) {
					   this.insertStmt.get(2).clearBindings();
					   this.insertStmt.get(2).bindLong(1, segId);
					   this.insertStmt.get(2).bindLong(2, p.getLatitudeE6());
					   this.insertStmt.get(2).bindLong(3, p.getLongitudeE6());
					   this.insertStmt.get(2).executeInsert();
				   }
		   		}
			   	db.setTransactionSuccessful();
		   	} finally {
		   		db.endTransaction();
		   	}
	   }

	   /**
	    * Delete all rows in the database
	    */
	   
	   public void deleteAll() {
	      this.db.delete(ROUTE_TABLE_NAME, null, null);
	      this.db.delete(SEGMENT_TABLE_NAME, null, null);
	      this.db.delete(POINTS_TABLE_NAME, null, null);
	      this.db.delete(ELEVATION_TABLE_NAME, null, null);
	   }
	   
	   /** Delete a given route and associated data.
	    * 
	    */
	   
	   public void delete(final int routeId) {
		   db.delete(ROUTE_TABLE_NAME, "WHERE id = " + routeId, null);
		   db.delete(ELEVATION_TABLE_NAME, "WHERE " + ROUTE_ID + " = " + routeId, null);
	   }
	   
	   /**
	    * Extract a route previously stored in the database.
	    * 
	    * @param routeId The row id of the route to restore.
	    * @return a new route object.
	    */
	   public Route getRoute(final int routeId) {
		   Route r = new Route();
		   
		   Cursor cursor = this.db.query(ROUTE_TABLE_NAME, new String[] { NAME, CRIGHT, WARN, CC, POLY, ITIN, LENGTH}, 
			        "id = '" + routeId + "'", null, null, null, NAME + " desc", "10");
		   if (cursor.moveToFirst()) {
			   r.setName(cursor.getString(0));
			   r.setCopyright(cursor.getString(1));
			   r.setWarning(cursor.getString(2));
			   r.setCountry(cursor.getString(3));
			   r.setPolyline(cursor.getString(4));
			   r.setItineraryId(cursor.getInt(5));
			   r.setLength(cursor.getInt(6));
		   }
		   cursor.close();		   
		   
		   cursor = this.db.query(SEGMENT_TABLE_NAME, new String[] { "id", NAME, INSTRUCTION, DIST, LENGTH}, 
			        ROUTE_ID + " = '" + routeId + "'", null, null, null, "id asc", null);
		   
		   if (cursor.moveToFirst()) {
			   Segment s = new Segment();
			   do {
				   s.setName(cursor.getString(1));
				   s.setInstruction(cursor.getString(2));
				   s.setDistance(cursor.getDouble(3));
				   s.setLength(cursor.getInt(4));
				   Cursor pointsCursor = this.db.query(POINTS_TABLE_NAME, new String[] { NAME, CRIGHT, WARN, CC, POLY, ITIN, LENGTH}, 
					        "id = '" + routeId + "'", null, null, null, NAME + " desc", "10");
			   } while (cursor.moveToNext());
		   }
		   
		   return r;
	   }
	   
	   /**
	    * Query the database for strings like the one given.
	    * @param ch String to match against
	    * @return a list of strings
	    */
	   
	   public List<String> selectLike(CharSequence ch) {
		   List<String> output = new ArrayList<String>();
		   String query = "%" + ch + "%";
		   StringBuilder sb = new StringBuilder(LIKE_QUERY);
		   DatabaseUtils.appendEscapedSQLString(sb, query);
		   Cursor cursor = this.db.query(ROUTE_TABLE_NAME, new String[] { NAME }, 
			        sb.toString(), null, null, null, NAME + " desc", "10");
		   if (cursor.moveToFirst()) {
			   do {
				   output.add(cursor.getString(0)); 
			   } while (cursor.moveToNext());
		   }
		   if (cursor != null && !cursor.isClosed()) {
			   cursor.close();
		   }
		   return output;
	   }
	   
	   /**
	    * Get all routes in the database.
	    * @return a list of all the routes in the db.
	    */

	   public List<String> selectAll() {
	      List<String> list = new ArrayList<String>();
	      Cursor cursor = this.db.query(ROUTE_TABLE_NAME, new String[] { NAME }, 
	        null, null, null, null, NAME + " desc");
	      if (cursor.moveToFirst()) {
	         do {
	            list.add(cursor.getString(0)); 
	         } while (cursor.moveToNext());
	      }
	      if (cursor != null && !cursor.isClosed()) {
	         cursor.close();
	      }
	      return list;
	   }
	   
	   /**
	    * 
	    * @author jono@nanosheep.net
	    * @version Jul 2, 2010
	    */

	   public static class AddressDatabaseHelper extends SQLiteOpenHelper {

	       AddressDatabaseHelper(Context context) {
	           super(context, DATABASE_NAME, null, DATABASE_VERSION);
	       }

	       @Override
	       public void onCreate(SQLiteDatabase db) {
	           db.execSQL(ROUTE_TABLE_CREATE);
	           db.execSQL(POINTS_TABLE_CREATE);
	           db.execSQL(SEGMENT_TABLE_CREATE);
	           db.execSQL(ELEVATION_TABLE_NAME);
	   		}

	       /* (non-Javadoc)
	        * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	        */
	       @Override
	       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    	   db.execSQL("DROP TABLE IF EXISTS " + ROUTE_TABLE_NAME);
	    	   db.execSQL("DROP TABLE IF EXISTS " + POINTS_TABLE_NAME);
	    	   db.execSQL("DROP TABLE IF EXISTS " + SEGMENT_TABLE_NAME);
	    	   db.execSQL("DROP TABLE IF EXISTS " + ELEVATION_TABLE_NAME);
	    	   onCreate(db);
	       }
	   }
}
