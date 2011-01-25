/**
 * 
 */
package com.nanosheep.bikeroute;

import com.nanosheep.bikeroute.utility.RouteDatabase;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Activity for browsing/selecting favourite routes.
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
 * @version Jan 25, 2011
 */
public class SavedRoutes extends ListActivity {
        
    private CursorAdapter dataSource;
    
    private RouteDatabase db;

    private static final String fields[] = {RouteDatabase.FRIENDLY_NAME, RouteDatabase.ROUTER };
 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new RouteDatabase(this);
        Cursor data = db.getRoutes();
        
        dataSource = new SimpleCursorAdapter(this, R.layout.row, data, fields, new int[] {R.id.routename, R.id.routeby });
        setListAdapter(dataSource);
    }
    
    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
    	BikeRouteApp app = (BikeRouteApp) getApplicationContext();
    	app.setRoute(db.getRoute(id));
    	db.close();
    	final Intent map = new Intent(this, LiveRouteMap.class);
		map.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivityForResult(map, R.id.trace);
    }
}
