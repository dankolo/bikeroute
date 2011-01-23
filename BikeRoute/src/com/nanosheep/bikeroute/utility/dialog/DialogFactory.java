/**
 * 
 */
package com.nanosheep.bikeroute.utility.dialog;

import com.nanosheep.bikeroute.R;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
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
 * @author jono@nanosheep.net
 * @version Jan 21, 2011
 */
public class DialogFactory {
	
	/**
	 * Return an about dialog with clickable links.
	 * @param context
	 * @return
	 */
	public static AlertDialog getAboutDialog(Context context) {
		final TextView message = new TextView(context);
		
		final Spanned s = 
		               Html.fromHtml(context.getString(R.string.about_message));
		message.setText(s);
		message.setMovementMethod(LinkMovementMethod.getInstance());

		return new AlertDialog.Builder(context)
		   .setCancelable(true)
		   .setIcon(android.R.drawable.ic_dialog_info)
		   .setPositiveButton(R.string.ok, null)
		   .setView(message)
		   .create();
	}
}
