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
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * @author jono@nanosheep.net
 * @version Jun 24, 2010
 */
public class DirectionsView extends ListActivity {
	private Route route;
	
	@Override
	public void onCreate(Bundle in) {
	  super.onCreate(in);
	  setResult(-1, new Intent());
	  Bundle bundle = getIntent().getExtras();
	  
	  route = bundle.getParcelable(BikeNav.ROUTE);
	  setTitle(route.getName());
	  List<String> step = new ArrayList<String>();
	  StringBuffer sBuf;
	  for (Segment s : route.getSegments()) {
		  sBuf = new StringBuffer();
		  if (!s.getTurn().equals("unknown")) {
			  sBuf.append(s.getTurn());
			  sBuf.append(" at ");
		  }
		  sBuf.append(s.getName());
		  sBuf.append(" ");
		  if (s.isWalk()) {
			  sBuf.append("(dismount) ");
		  }
		  sBuf.append(s.getLength());
		  sBuf.append("m");
		  step.add(sBuf.toString());
	  }

	  setListAdapter(new ArrayAdapter<String>(this,
	          android.R.layout.simple_list_item_1, step));
	  getListView().setTextFilterEnabled(true);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		setResult(position, new Intent());
		finish();
	}	
	
}
