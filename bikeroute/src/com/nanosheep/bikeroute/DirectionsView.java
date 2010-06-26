/**
 * 
 */
package com.nanosheep.bikeroute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
	  //sert basic return for exit, do not jump the main map
	  setResult(-1, new Intent());
	  getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
              WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	  //Get bundled route.
	  final Bundle bundle = getIntent().getExtras();
	  
	  route = bundle.getParcelable(BikeNav.ROUTE);
	  setTitle(route.getName());
	  final List<String> step = new ArrayList<String>();
	  StringBuffer sBuf;
	  if (Locale.getDefault().equals(Locale.UK)) {
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
		  sBuf.append('\n');
		  sBuf.append(s.getLength());
		  sBuf.append('m');
		  step.add(sBuf.toString());
	  }
	  } else if (Locale.getDefault().equals(Locale.US)) {
		  for (Segment s : route.getSegments()) {
			  sBuf = new StringBuffer();
			  sBuf.append(s.getTurn());
			  sBuf.append('\n');
			  sBuf.append(s.getLength());
			  sBuf.append('m');
			  step.add(sBuf.toString());
		  }
	  }

	  TextView footer = new TextView(this);
	  sBuf = new StringBuffer();
	  if (route.getWarning() != null) {
		  sBuf.append(route.getWarning());
		  sBuf.append('\n');
	  }
	  if (route.getCopyright() != null) {
		  sBuf.append(route.getCopyright());
	  }
	  footer.setText(sBuf.toString());
	  getListView().addFooterView(footer, "", false);
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
