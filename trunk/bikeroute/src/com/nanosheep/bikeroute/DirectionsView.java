/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author jono@nanosheep.net
 * @version Jun 24, 2010
 */
public class DirectionsView extends ListActivity {
	/** Route object. **/
	private Route route;
	
	@Override
	public void onCreate(final Bundle in) {
	  super.onCreate(in);
	  getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
              WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	  setResult(-1, new Intent());
	  final Bundle bundle = getIntent().getExtras();
	  
	  route = bundle.getParcelable(BikeNav.ROUTE);
	  setTitle(route.getName());
	  final List<String> step = new ArrayList<String>();
	  StringBuffer sBuf;
	  for (Segment s : route.getSegments()) {
		  sBuf = new StringBuffer();
		  if (!s.getTurn().equals("Unknown")) {
			 
			  sBuf.append(s.getTurn());
			  sBuf.append(" at ");
		  }
		  sBuf.append(s.getName());
		  sBuf.append(' ');
		  if (s.isWalk()) {
			  sBuf.append("(dismount) ");
		  }
		  sBuf.append(s.getLength());
		  sBuf.append('m');
		  step.add(sBuf.toString());
	  }

	  setListAdapter(new ArrayAdapter<String>(this,
	          android.R.layout.simple_list_item_1, step));
	  getListView().setTextFilterEnabled(true);
	}
	
	/**
	 * Return to the navigation activity if a direction item is
	 * clicked, focus the map there.
	 */
	
	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		setResult(position, new Intent());
		finish();
	}	
	
}
