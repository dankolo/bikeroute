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

import org.andnav.osm.util.GeoPoint;

import com.nanosheep.bikeroute.utility.route.Route;
import com.nanosheep.bikeroute.utility.route.Segment;

/**
 * SQLite database helper class for storing and retrieving
 * favourite routes.
 * 
 * @author jono@nanosheep.net
 * @version Jan 8, 2011
 */
public class RouteDatabase {
		private static final int DATABASE_VERSION = 2;
		private static final String ROUTE_TABLE_NAME = "route";
		private static final String NAME = "name_string";
		
		private static final String POINTS_TABLE_NAME = "points";
		private static final String LAT = "latitude_e6_int";
		private static final String LNG = "longitute_e6_int";
		private static final String SEG_ID = "segid";
		
		private static final String SEGMENT_TABLE_NAME = "segments";
		private static final String INSTRUCTION = "turn_string";
		private static final String ROUTE_ID = "routeid";
		
		private static final String ROUTE_TABLE_CREATE =
               "CREATE TABLE " + ROUTE_TABLE_NAME + " (id INTEGER PRIMARY KEY, " + NAME + " TEXT);";
		
		private static final String POINTS_TABLE_CREATE =
			"CREATE TABLE " + POINTS_TABLE_NAME + " (" + SEG_ID + " INTEGER , " + LAT + 
			" INTEGER, " + LNG + " INTEGER);";
		
		private static final String SEGMENT_TABLE_CREATE = 
			"CREATE TABLE " + SEGMENT_TABLE_NAME + " (id INTEGER PRIMARY KEY, " + INSTRUCTION + " TEXT, " +
					ROUTE_ID + "INTEGER);";
		
		
		private static final String DATABASE_NAME = "bikeroute_db";

	   private Context context;
	   private SQLiteDatabase db;

	   private List<SQLiteStatement> insertStmt;
	   private static final String INSERT_ROUTE = "insert into " 
	      + ROUTE_TABLE_NAME + " values (NULL, ?);";
	   private static final String INSERT_SEGMENT = "insert into "
		   + SEGMENT_TABLE_NAME + " values (NULL, ?, ?);";
	   private static final String INSERT_POINT = "insert into "
		   + POINTS_TABLE_NAME + " values (?, ?, ?);";
	   
	   private static final String LIKE_QUERY = NAME + " LIKE ";

	   public RouteDatabase(Context context) {
	      this.context = context;
	      AddressDatabaseHelper openHelper = new AddressDatabaseHelper(this.context);
	      this.db = openHelper.getWritableDatabase();
	      this.insertStmt = new ArrayList<SQLiteStatement>(3);
	      this.insertStmt.add(this.db.compileStatement(INSERT_ROUTE));
	      this.insertStmt.add(this.db.compileStatement(INSERT_SEGMENT));
	      this.insertStmt.add(this.db.compileStatement(INSERT_POINT));
	   }
	   
	   /**
	    * Add route to the database.
	    * @param route the route to store.
	    * @return
	    */

	   public void insert(Route route) {
		   
		   //Insert the route and get the id back
		   this.insertStmt.get(0).bindString(1, route.getName()); 
		   final long routeId = this.insertStmt.get(0).executeInsert();
		   
		   //Insert each segment with a ref to the route
		   for(Segment s : route.getSegments()) {
			   this.insertStmt.get(1).clearBindings();
			   this.insertStmt.get(1).bindString(1, s.getInstruction());
			   this.insertStmt.get(1).bindLong(2, routeId);
			   final long segId = this.insertStmt.get(1).executeInsert();
			   //And each point with a ref to the segment
			   for (GeoPoint p : s.getPoints()) {
				   this.insertStmt.get(2).clearBindings();
				   this.insertStmt.get(2).bindLong(1, segId);
				   this.insertStmt.get(2).bindLong(2, p.getLatitudeE6());
				   this.insertStmt.get(2).bindLong(3, p.getLongitudeE6());
				   this.insertStmt.get(2).executeInsert();
			   }
		   }
	   }

	   /**
	    * Delete all rows in the database
	    */
	   
	   public void deleteAll() {
	      this.db.delete(ROUTE_TABLE_NAME, null, null);
	      this.db.delete(SEGMENT_TABLE_NAME, null, null);
	      this.db.delete(POINTS_TABLE_NAME, null, null);
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
	   		}

	       /* (non-Javadoc)
	        * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	        */
	       @Override
	       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    	   db.execSQL("DROP TABLE IF EXISTS " + ROUTE_TABLE_NAME);
	    	   db.execSQL("DROP TABLE IF EXISTS " + POINTS_TABLE_NAME);
	    	   db.execSQL("DROP TABLE IF EXISTS " + SEGMENT_TABLE_NAME);
	    	   onCreate(db);
	       }
	   }
}
